# Firebase
-keepclassmembers class com.savingcoach.app.data.model.** { *; }
-keepclassmembers class com.savingcoach.app.data.firestore.** { *; }

# Gemini
-keep class com.google.ai.client.generativeai.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
