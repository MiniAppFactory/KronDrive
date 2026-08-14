# Kron Drive — R8/ProGuard kurallari.

# play-services-ads, transitive olarak WorkManager+Room cekiyor ve bunlar
# reflection kullaniyor. Boom Blocks'ta release build R8 full-mode ile
# "Failed to create an instance of androidx.work.impl.WorkDatabase" crash'i
# verdigi icin ayni kurallar bastan konuldu.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keep class kotlin.Metadata { *; }
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
