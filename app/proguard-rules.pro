# kotlinx.serialization — keep serializer factories and @Serializable classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    static **$$serializer INSTANCE;
}
-keep,includedescriptorclasses class com.wfx.warungpos.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.wfx.warungpos.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase — belt-and-suspenders; Firebase plugin adds rules automatically
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Room — keep @Entity, @Dao, @Database; Room KSP generates rules but explicit backup
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# Hilt — Hilt Gradle plugin adds rules automatically; explicit backup
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# EncryptedSharedPreferences / Tink — keep crypto primitives
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin metadata (required for reflection-less serialization)
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
