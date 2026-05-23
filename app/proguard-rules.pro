# Keep Media3 and XML-driven app classes stable enough for Fire TV release builds.
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# Release builds should not leak verbose local diagnostics.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
