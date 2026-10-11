import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.victorkirui.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        val localProperties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val webClientId = localProperties.getProperty("WEB_CLIENT_ID") ?: ""
        val cfClientId = localProperties.getProperty("CF_ACCESS_CLIENT_ID") ?: ""
        val cfClientSecret = localProperties.getProperty("CF_ACCESS_CLIENT_SECRET") ?: ""

        resValue("string", "default_web_client_id", webClientId)
        buildConfigField("String", "CF_ACCESS_CLIENT_ID", "\"$cfClientId\"")
        buildConfigField("String", "CF_ACCESS_CLIENT_SECRET", "\"$cfClientSecret\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        resValues = true
        buildConfig = true
    }
}

dependencies {
    // Firebase & Auth
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.credentialsManager)
    implementation(libs.androidx.credentialsPlayServices)
    implementation(libs.googleid)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.koin.android)
    implementation(libs.play.billing.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.retrofit.converter.gson)
}

