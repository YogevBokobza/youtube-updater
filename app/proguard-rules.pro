# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.yogev.youtubeupdater.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.yogev.youtubeupdater.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
