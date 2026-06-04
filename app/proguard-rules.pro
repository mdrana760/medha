# Keep model classes used by Gson / Firestore deserialization.
-keepclassmembers class com.medha.app.firebase.** { *; }
-keepclassmembers class com.medha.app.ai.** { <fields>; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
