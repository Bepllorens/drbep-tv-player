# Keep Media3 and XML-driven app classes stable enough for Fire TV release builds.
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# libVLC resolves its Java bridge classes from native JNI_OnLoad. R8 cannot see
# those references, so release minification must preserve the complete bridge.
-keep class org.videolan.libvlc.** { *; }
-keep interface org.videolan.libvlc.** { *; }

# Release builds should not leak verbose local diagnostics.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep the canonical snapshot verifier as a stable, auditable unit.
-keepclassmembers class com.drbep.tvplayer.CatalogSnapshotStore {
    private static void validateSnapshotSignature(org.json.JSONObject);
    private static void updateCanonicalSnapshotPayload(java.security.Signature, org.json.JSONObject);
    private static void appendCanonicalJson(java.security.Signature, java.lang.Object, boolean);
    private static java.lang.String numberToCanonicalString(java.lang.Number);
    private static java.lang.String goStyleJsonQuote(java.lang.String);
}

# --- Estabilidad de las caches serializadas entre versiones (OTA) ---
# Las caches de arranque (CatalogSnapshotStore$StartupParsedCatalogCache /
# $StartupPlaybackChannelCache) y el modelo de catalogo/EPG (ChannelItem,
# ChannelFilter, OfflinePermissions, CatalogLoadResult) se persisten en disco
# con serializacion Java. R8 reofusca los nombres de clase en cada build, por lo
# que el snapshot escrito por una version anterior deja de ser legible en la
# siguiente (java.io.InvalidClassException). Eso invalida la cache en el primer
# arranque tras cada actualizacion OTA, obliga a reconstruir el catalogo (~27s),
# retrasa el arranque del primer canal y deja el EPG sin datos hasta terminar.
# Mantener estables el nombre de clase y los nombres de campo de todas las
# clases Serializable evita esa invalidacion y hace que las caches sobrevivan a
# las actualizaciones.
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
