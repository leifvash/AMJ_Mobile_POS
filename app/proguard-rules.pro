# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.amj_pos.data.local.entities.** { *; }
-keep class com.amj_pos.domain.model.** { *; }

# WorkManager
-keep class * extends androidx.work.ListenableWorker
-keep class * extends androidx.work.CoroutineWorker

# Paging
-dontwarn androidx.paging.compose.**
