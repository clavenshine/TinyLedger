# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities
-keep class com.tinyledger.app.data.local.entity.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep SMS parsing classes (data classes, regex patterns, enums)
-keep class com.tinyledger.app.data.sms.** { *; }
-keep class com.tinyledger.app.domain.model.TransactionType { *; }

# Keep notification service and its inner classes
-keep class com.tinyledger.app.data.notification.** { *; }
