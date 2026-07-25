# Keep audio effect classes since they're accessed via reflection-adjacent system APIs
-keep class android.media.audiofx.** { *; }

# Keep serializable data models (kotlinx.serialization uses generated serializers)
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.premiumeq.equalizer.**$$serializer {
    *** INSTANCE;
}
-keepclasseswithmembers class com.premiumeq.equalizer.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt / Dagger generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Glance widgets
-keep class androidx.glance.** { *; }
