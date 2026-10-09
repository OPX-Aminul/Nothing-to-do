-dontwarn kotlin.**
-keep class com.hairgrowth.os.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes *Annotation*
