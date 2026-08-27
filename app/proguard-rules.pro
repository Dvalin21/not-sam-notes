# ProGuard rules for Not Sam Notes

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep data classes
-keep class com.openlight.notes.core.model.** { *; }
-keep class com.openlight.notes.core.db.** { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep SMBJ
-keep class com.hierynomus.** { *; }
-keep class org.bouncycastle.** { *; }

# Keep NanoHTTPD
-keep class fi.iki.elonen.** { *; }

# Keep Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Keep Compose
-keep class androidx.compose.** { *; }
