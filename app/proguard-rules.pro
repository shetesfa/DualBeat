# DualBeat ProGuard Rules
-keepattributes *Annotation*
-keepclassmembers class * extends androidx.room.RoomDatabase
-dontwarn androidx.media3.**
-keep class com.tesfa.dualbeat.audio.processor.** { *; }
-keep class * extends androidx.media3.common.audio.BaseAudioProcessor { *; }
