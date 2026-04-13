# ProGuard rules for Botinis
-keepattributes Signature
-keepattributes *Annotation*
-keep class io.botinis.app.data.model.** { *; }
-keep class com.google.gson.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Groq SDK
-keep class com.groq.** { *; }
