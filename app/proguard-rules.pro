-keep class xyz.elouan.movies.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
# Coil
-dontwarn coil.**
