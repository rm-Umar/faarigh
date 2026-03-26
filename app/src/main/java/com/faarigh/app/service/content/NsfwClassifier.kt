package com.faarigh.app.service.content

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * On-device NSFW image classifier using TensorFlow Lite.
 * Uses a MobileNetV2-based model (GantMan/nsfw_model) that takes 224x224 RGB images
 * and outputs probabilities for 5 categories:
 *   [drawings, hentai, neutral, porn, sexy]
 *
 * We combine hentai + porn + sexy as the "NSFW score."
 *
 * The model file (nsfw_model.tflite) must be placed in app/src/main/assets/.
 * Run scripts/download_nsfw_model.sh to obtain it.
 */
class NsfwClassifier(private val context: Context) {

    companion object {
        private const val TAG = "NsfwClassifier"
        private const val MODEL_FILE = "nsfw_model.tflite"
        private const val IMAGE_SIZE = 224
        private const val PIXEL_SIZE = 3 // RGB
        private const val FLOAT_SIZE = 4

        // Output category indices for the GantMan nsfw_model
        private const val IDX_DRAWINGS = 0
        private const val IDX_HENTAI = 1
        private const val IDX_NEUTRAL = 2
        private const val IDX_PORN = 3
        private const val IDX_SEXY = 4
        private const val NUM_CLASSES = 5
    }

    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private var outputSize = NUM_CLASSES

    fun initialize(): Boolean {
        return try {
            val model = loadModelFile(context, MODEL_FILE)
            val options = Interpreter.Options().apply {
                numThreads = 2
            }
            interpreter = Interpreter(model, options)

            // Detect output shape from the model itself
            val outputTensor = interpreter?.getOutputTensor(0)
            outputSize = outputTensor?.shape()?.last() ?: NUM_CLASSES
            Log.i(TAG, "Model output classes: $outputSize")

            isInitialized = true
            Log.i(TAG, "NSFW classifier initialized")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize NSFW classifier: ${e.message}")
            Log.w(TAG, "Make sure $MODEL_FILE is in the assets folder")
            isInitialized = false
            false
        }
    }

    /**
     * Classification result with per-class scores for fine-grained control.
     */
    data class ClassificationResult(
        val explicitScore: Float,  // porn + hentai only (truly explicit)
        val sexyScore: Float,      // suggestive but not explicit
        val rawScores: FloatArray,  // all 5 class probabilities
        val error: Boolean = false,
    )

    /**
     * Classify an image and return detailed NSFW scores.
     *
     * The 5-class model outputs: [drawings, hentai, neutral, porn, sexy]
     * - "sexy" triggers on normal photos with shoulders/skin — way too aggressive
     * - We ONLY use porn + hentai for the explicit score
     * - "sexy" is provided separately so the UI can optionally show it
     *
     * Returns error=true if the classifier is not initialized.
     */
    fun classifyDetailed(bitmap: Bitmap): ClassificationResult {
        if (!isInitialized || interpreter == null) {
            return ClassificationResult(0f, 0f, FloatArray(0), error = true)
        }

        return try {
            val resized = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true)
            val inputBuffer = bitmapToByteBuffer(resized)
            if (resized != bitmap) resized.recycle()

            val output = Array(1) { FloatArray(outputSize) }
            interpreter?.run(inputBuffer, output)

            val scores = output[0]
            val explicitScore: Float
            val sexyScore: Float

            if (outputSize >= NUM_CLASSES) {
                // Only porn + hentai — NOT sexy (too many false positives)
                explicitScore = scores[IDX_PORN] + scores[IDX_HENTAI]
                sexyScore = scores[IDX_SEXY]
            } else if (outputSize == 2) {
                explicitScore = scores[1]
                sexyScore = 0f
            } else {
                explicitScore = scores.last()
                sexyScore = 0f
            }

            Log.d(TAG, "NSFW explicit=${"%.3f".format(explicitScore)} sexy=${"%.3f".format(sexyScore)} (raw: ${scores.map { "%.3f".format(it) }})")
            ClassificationResult(
                explicitScore = explicitScore.coerceIn(0f, 1f),
                sexyScore = sexyScore.coerceIn(0f, 1f),
                rawScores = scores,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Classification failed: ${e.message}")
            ClassificationResult(0f, 0f, FloatArray(0), error = true)
        }
    }

    /**
     * Simple classify for backward compat — returns explicit score only (porn + hentai).
     * Does NOT include "sexy" class.
     */
    fun classify(bitmap: Bitmap): Float {
        val result = classifyDetailed(bitmap)
        return if (result.error) -1f else result.explicitScore
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val bufferSize = 1 * IMAGE_SIZE * IMAGE_SIZE * PIXEL_SIZE * FLOAT_SIZE
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        bitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

        // GantMan MobileNetV2 expects pixels normalized to [-1, 1]
        // using tf.keras.applications.mobilenet_v2.preprocess_input
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 127.5f - 1.0f) // R
            buffer.putFloat(((pixel shr 8) and 0xFF) / 127.5f - 1.0f)  // G
            buffer.putFloat((pixel and 0xFF) / 127.5f - 1.0f)           // B
        }

        buffer.rewind()
        return buffer
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val assetFd = context.assets.openFd(filename)
        val inputStream = FileInputStream(assetFd.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFd.startOffset,
            assetFd.declaredLength,
        )
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}
