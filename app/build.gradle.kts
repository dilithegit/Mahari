import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.ksp)
}









android {
    namespace = "com.example.mahari"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.mahari"
        minSdk = 24
        targetSdk = 35


        versionCode = 18
        versionName = "3.11.0"
    }

    lint {
        disable += "NullSafeMutableLiveData"
    }

    signingConfigs {
        create("release") {
            val keystorePropsFile = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()
            if (keystorePropsFile.exists()) {
                keystoreProperties.load(FileInputStream(keystorePropsFile))
            }

            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
                ?: keystoreProperties.getProperty("RELEASE_KEYSTORE_PATH")
            val storePass = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                ?: keystoreProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
            val alias = System.getenv("RELEASE_KEY_ALIAS")
                ?: keystoreProperties.getProperty("RELEASE_KEY_ALIAS")
            val keyPass = System.getenv("RELEASE_KEY_PASSWORD")
                ?: keystoreProperties.getProperty("RELEASE_KEY_PASSWORD")

            if (!keystorePath.isNullOrEmpty() && rootProject.file(keystorePath).exists()) {
                storeFile = rootProject.file(keystorePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    sourceSets {
        getByName("main") {
            java.srcDir("build/generated/ksp/main/kotlin")
        }
        getByName("debug") {
            java.srcDir("build/generated/ksp/debug/kotlin")
        }
    }



    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation.compose)


  // Security & Biometric
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.biometric)
  implementation("net.zetetic:android-database-sqlcipher:4.5.4@aar")
  implementation("androidx.sqlite:sqlite-ktx:2.4.0")

  // Room Database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
}










