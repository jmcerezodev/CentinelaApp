# Reglas específicas para iText7 (Generación de PDF)
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# SLF4J - Ignorar advertencias de clases faltantes (Común en iText7)
-dontwarn org.slf4j.**

# Reglas para Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao

# Reglas para Google Play Services (Location)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Reglas para Jetpack Glance (Widgets)
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver
-keep class * extends androidx.glance.appwidget.action.ActionCallback

# Evitar que R8 elimine clases necesarias para la Biometría
-dontwarn androidx.biometric.**

# Ignorar advertencias de dependencias de Java SE que no existen en Android
-dontwarn javax.annotation.**
-dontwarn java.awt.**
-dontwarn javax.naming.**

# Preservar atributos necesarios para el debugging en release
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
