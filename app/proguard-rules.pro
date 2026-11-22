# Preserve Kotlin metadata and reflection for Moshi KotlinJsonAdapterFactory
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.Json <methods>;
}
-keep class com.example.data.** { *; }
-keep class kotlin.Metadata { *; }

# Retrofit & OkHttp
-dontwarn retrofit2.**
-keepattributes Exceptions
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
