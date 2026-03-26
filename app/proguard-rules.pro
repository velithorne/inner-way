# Aura Shell — keep minimal; Compose + Kotlin metadata handled by R8 defaults.

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.* class * { *; }
