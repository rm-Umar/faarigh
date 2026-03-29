# Widget providers — must survive R8 by exact class name (manifest resolves them via reflection)
-keep class com.faarigh.app.widget.** { *; }
-keepnames class com.faarigh.app.widget.FaarighWidget$WidgetSize { *; }

# LiteRT (formerly TensorFlow Lite)
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn org.tensorflow.lite.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
