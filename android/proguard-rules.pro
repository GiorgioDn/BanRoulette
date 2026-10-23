# Proguard / R8 rules for Ban Roulette Android app

# Keep domain models from :core
-keep class com.example.model.** { *; }

# Keep Room entities, DAOs, and database
-keep class com.example.banroulette.data.entity.** { *; }
-keep class com.example.banroulette.data.dao.** { *; }
-keep class com.example.banroulette.data.db.** { *; }

# Keep core wheel geometry, calculator, and palette
-keep class com.example.core.geometry.** { *; }
-keep class com.example.core.calculator.** { *; }
-keep class com.example.core.selector.** { *; }
-keep class com.example.core.validator.** { *; }
