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
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao class * { *; }

# DataStore / Preferences
-keep class androidx.datastore.** { *; }
-keepclassmembers class * {
    @androidx.datastore.preferences.core.* *;
}

# Our app modules + module interface
-keep class com.mindful.app.module.** { *; }
-keep class com.mindful.app.data.preferences.** { *; }
-keep class com.mindful.app.data.db.** { *; }
-keep class com.mindful.app.data.repository.** { *; }
-keep class com.mindful.app.service.** { *; }
-keep class com.mindful.app.data.learn.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Glance widgets
-keep class com.mindful.app.widget.** { *; }
-keep class androidx.glance.** { *; }
