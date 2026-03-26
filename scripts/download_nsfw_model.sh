#!/bin/bash
# Downloads a pre-trained NSFW classification TFLite model.
#
# Source: GantMan/nsfw_model (MobileNetV2 v2, trained on Yahoo Open NSFW dataset)
# https://github.com/GantMan/nsfw_model
#
# The model classifies images into 5 categories:
#   - drawings, hentai, neutral, porn, sexy
# For our use, we combine hentai+porn+sexy as "nsfw" and drawings+neutral as "safe"
#
# Model input:  224x224 RGB float32
# Model output: [1][5] float32 probabilities

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ASSETS_DIR="$PROJECT_DIR/app/src/main/assets"

mkdir -p "$ASSETS_DIR"

MODEL_URL="https://github.com/nicnl31/nsfw-image-detection/raw/main/models/mobilenet_v2_140_224.tflite"
MODEL_PATH="$ASSETS_DIR/nsfw_model.tflite"

if [ -f "$MODEL_PATH" ]; then
    echo "Model already exists at $MODEL_PATH"
    echo "Size: $(du -h "$MODEL_PATH" | cut -f1)"
    exit 0
fi

echo "Downloading NSFW classification model..."
echo "Source: MobileNetV2 trained on Yahoo Open NSFW dataset"
echo ""

# Try primary URL
if curl -L -o "$MODEL_PATH" "$MODEL_URL" 2>/dev/null; then
    echo "Downloaded successfully."
    echo "Size: $(du -h "$MODEL_PATH" | cut -f1)"
    echo "Location: $MODEL_PATH"
else
    echo ""
    echo "Automatic download failed. Please manually download a TFLite NSFW model."
    echo ""
    echo "Option 1: GantMan's nsfw_model"
    echo "  https://github.com/GantMan/nsfw_model"
    echo "  Download the mobilenet_v2_140_224 model and convert to TFLite"
    echo ""
    echo "Option 2: Use the model from:"
    echo "  https://github.com/nicnl31/nsfw-image-detection"
    echo ""
    echo "Place the .tflite file at:"
    echo "  $MODEL_PATH"
    exit 1
fi
