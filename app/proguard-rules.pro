# Room: o banco é criado por reflexão a partir da classe gerada *_Impl
-keep class * extends androidx.room.RoomDatabase { <init>(); }
# WorkManager instancia os workers por reflexão
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
