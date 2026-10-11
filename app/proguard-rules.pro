# ProGuard / R8 rules for Remindly Release Build

# Preserve generic signatures, inner classes, annotations for Retrofit suspend functions, Gson, and Reflection
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep Kotlin coroutines Continuation generic signatures
-keep class kotlin.coroutines.Continuation { *; }
-keepclassmembers class * implements kotlin.coroutines.Continuation { *; }

# 1. DATA & DATA MANIPULATION (Models, DTOs, Entities, Payloads)
-keep class com.victorkirui.core.model.** { *; }
-keepclassmembers class com.victorkirui.core.model.** { *; }

-keep class com.victorkirui.local.entity.** { *; }
-keepclassmembers class com.victorkirui.local.entity.** { *; }

-keep class com.victorkirui.module_features.capturing.ai.** { *; }
-keepclassmembers class com.victorkirui.module_features.capturing.ai.** { *; }

-keep class com.victorkirui.remote.** { *; }
-keepclassmembers class com.victorkirui.remote.** { *; }

# 2. VIEWMODELS & USECASES
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }

-keep class com.victorkirui.module_features.**.*ViewModel { *; }
-keepclassmembers class com.victorkirui.module_features.**.*ViewModel { *; }

-keep class com.victorkirui.module_features.**.*UseCase { *; }
-keepclassmembers class com.victorkirui.module_features.**.*UseCase { *; }

# 3. REPOSITORIES, DAOs, & ROOM DATABASE
-keep interface com.victorkirui.local.dao.** { *; }
-keepclassmembers interface com.victorkirui.local.dao.** { *; }

-keep class com.victorkirui.local.RemindlyDatabase { *; }
-keepclassmembers class com.victorkirui.local.RemindlyDatabase { *; }

-keep interface com.victorkirui.core.repository.** { *; }
-keep class com.victorkirui.core.repository.** { *; }
-keepclassmembers class com.victorkirui.core.repository.** { *; }

-keep class com.victorkirui.local.repository.** { *; }
-keepclassmembers class com.victorkirui.local.repository.** { *; }

# 4. RETROFIT, OKHTTP & GSON
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# 5. WORKMANAGER WORKERS & KOIN
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class com.victorkirui.module_features.capturing.** { *; }
-keep class com.victorkirui.module_features.subscription.** { *; }

-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }
