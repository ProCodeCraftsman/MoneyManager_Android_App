# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools.

-keepattributes Signature
-keepattributes *Annotation*

# Hilt
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Firebase
-keep class com.google.firebase.** { *; }
-keepnames class com.google.firebase.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }