import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val keystoreProps = Properties()
val loadedKeystorePropsFile = listOf(
    rootProject.file("../keystore.properties"),
    rootProject.file("keystore.properties")
).firstOrNull { it.isFile }
if (loadedKeystorePropsFile != null) {
    loadedKeystorePropsFile.inputStream().use { keystoreProps.load(it) }
}

val envStorePath = System.getenv("ANDROID_KEYSTORE_PATH")?.trim()?.takeIf { it.isNotEmpty() }
val propStorePath = keystoreProps.getProperty("storeFile")?.trim()?.takeIf { it.isNotEmpty() }
val releaseStoreFile: File? = when {
    envStorePath != null -> File(envStorePath).let {
        if (it.isAbsolute) it else File(rootProject.projectDir, envStorePath)
    }
    propStorePath != null && loadedKeystorePropsFile != null -> File(propStorePath).let {
        if (it.isAbsolute) it else File(loadedKeystorePropsFile.parentFile, propStorePath)
    }
    else -> null
}
val releaseStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
    ?: keystoreProps.getProperty("storePassword")?.takeIf { it.isNotBlank() }
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")?.takeIf { it.isNotBlank() }
    ?: keystoreProps.getProperty("keyAlias")?.takeIf { it.isNotBlank() }
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
    ?: keystoreProps.getProperty("keyPassword")?.takeIf { it.isNotBlank() }

val hasReleaseSigning = releaseStoreFile?.isFile == true &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.exantt.btrouterclient"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.exantt.btrouterclient"
        minSdk = 29
        targetSdk = 36
        versionCode = 19
        // Semantic Versioning: повышать версию при изменениях (например, 1.1.0 -> 1.1.1)
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}