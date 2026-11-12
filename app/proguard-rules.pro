# Campus Life — ProGuard / R8 configuration
-keep class ru.campuslife.app.MainActivity { *; }
-keep class ru.campuslife.app.BootReceiver { *; }
-keep class ru.campuslife.app.ReminderReceiver { *; }
-dontwarn com.google.zxing.**
-dontwarn androidx.camera.**
