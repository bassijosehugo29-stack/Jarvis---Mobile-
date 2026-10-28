plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "br.com.jarvis.voicea"
    compileSdk = 35
    defaultConfig {
        applicationId = "br.com.jarvis.voicea"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["voiceLabel"] = "JARVIS Voz A"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        externalNativeBuild { cmake { arguments += listOf("-DANDROID_STL=c++_static", "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON") } }
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
            applicationIdSuffix = ".v4preview"
            versionNameSuffix = "-fase1"
            matchingFallbacks += listOf("debug")
            manifestPlaceholders["voiceLabel"] = "JARVIS Voz A · Prévia 4"
            ndk { abiFilters.clear(); abiFilters += "arm64-v8a" }
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    sourceSets.getByName("main") {
        jniLibs.srcDir("src/main/generated/jniLibs")
        assets.srcDir("src/main/generated/assets")
    }
    androidResources { noCompress += listOf("onnx") }
    lint { abortOnError = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies { testImplementation("junit:junit:4.13.2") }
