plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// CI distribution targets Galaxy ARM64; emulator jobs keep both ABIs by default.
val jarvisAbis = providers.gradleProperty("jarvisAbis").orElse("arm64-v8a,x86_64").get().split(",")
require(jarvisAbis.all { it in setOf("arm64-v8a", "x86_64") })

android {
    namespace = "br.com.jarvis.mobile"
    compileSdk = 35
    defaultConfig {
        applicationId = "br.com.jarvis.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "4.0.1"
        buildConfigField("String", "VOICE_ENGINE_PACKAGE", "\"br.com.jarvis.voicea\"")
        manifestPlaceholders["voiceEnginePackage"] = "br.com.jarvis.voicea"
        manifestPlaceholders["jarvisLabel"] = "@string/app_name"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += jarvisAbis }
        externalNativeBuild {
            cmake { arguments += listOf("-DANDROID_STL=c++_static", "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON") }
        }
    }
    ndkVersion = "27.2.12479018"
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
    buildTypes {
        val signingFile = System.getenv("JARVIS_KEYSTORE_PATH")
        if (!signingFile.isNullOrBlank()) {
            val fixed = signingConfigs.create("jarvisTest") {
                storeFile = file(signingFile)
                storePassword = System.getenv("JARVIS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("JARVIS_KEY_ALIAS")
                keyPassword = System.getenv("JARVIS_KEY_PASSWORD")
            }
            getByName("debug") { signingConfig = fixed }
        }
        create("preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".escutapreview"
            versionNameSuffix = "-escuta"
            matchingFallbacks += listOf("debug")
            buildConfigField("String", "VOICE_ENGINE_PACKAGE", "\"br.com.jarvis.voicea.escutapreview\"")
            manifestPlaceholders["voiceEnginePackage"] = "br.com.jarvis.voicea.escutapreview"
            manifestPlaceholders["jarvisLabel"] = "JARVIS Escuta · Prévia"
            ndk { abiFilters.clear(); abiFilters += "arm64-v8a" }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    lint { abortOnError = true; warningsAsErrors = false }
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
