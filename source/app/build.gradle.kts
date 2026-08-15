import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.miniappfactory.krondrive"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.miniappfactory.krondrive"
    minSdk = 24
    targetSdk = 36
    versionCode = 10
    versionName = "1.0.9"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Boom Blocks ile ayni desen: imzalama sirlari ASLA kaynak kodda degil,
  // git'e girmeyen signing.properties'ten ya da ortam degiskeninden okunur
  // (ortam degiskeni oncelikli — CI/farkli makine senaryosu icin).
  // FARK: Kron Drive'in henuz bir upload keystore'u YOK. Dosya olusturulana
  // kadar release signingConfig hic tanimlanmaz — aksi halde `assembleRelease`
  // "keystore not found" ile patlardi. Keystore olusturuldugu an (bkz.
  // docs/RELEASE_CHECKLIST.md) bu blok kendiliginden devreye girer.
  val keystoreFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
  val signingPropsFile = rootProject.file("signing.properties")
  val hasReleaseKeystore = keystoreFile.exists()

  if (hasReleaseKeystore) {
    signingConfigs {
      create("release") {
        val signingProps = Properties().apply {
          if (signingPropsFile.exists()) signingPropsFile.inputStream().use { load(it) }
        }
        storeFile = keystoreFile
        storePassword = System.getenv("STORE_PASSWORD") ?: signingProps.getProperty("storePassword")
        keyAlias = System.getenv("KEY_ALIAS") ?: signingProps.getProperty("keyAlias") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD") ?: signingProps.getProperty("keyPassword")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
    }
    // debug: AGP'nin varsayilan debug imzasi kullanilir (ayri bir keystore
    // dosyasi gerektirmez, her makinede calisir).
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.play.services.ads)
  implementation(libs.user.messaging.platform)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
