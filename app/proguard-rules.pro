# ProGuard rules for ProjektT

# Keep Google API classes
-keep class com.google.api.** { *; }
-keep class com.google.auth.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
