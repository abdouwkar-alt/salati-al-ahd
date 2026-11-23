plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.github.jk1.dependency-license-report")
}
android {
 namespace = "com.salati.app"
 compileSdk = 35
 defaultConfig { applicationId = "com.salati.app"; minSdk = 26; targetSdk = 35; versionCode = 5; versionName = "1.3.1" }

 signingConfigs {
  val releaseStorePath = System.getenv("ANDROID_KEYSTORE_PATH")
  val releaseStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
  val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
  val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
  if (!releaseStorePath.isNullOrBlank() && !releaseStorePassword.isNullOrBlank() &&
      !releaseKeyAlias.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()) {
   create("release") {
    storeFile = file(releaseStorePath)
    storePassword = releaseStorePassword
    keyAlias = releaseKeyAlias
    keyPassword = releaseKeyPassword
   }
  }
 }
 buildTypes {
  getByName("release") {
   isMinifyEnabled = false
   if (signingConfigs.findByName("release") != null) {
    signingConfig = signingConfigs.getByName("release")
   }
  }
 }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.02.00"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.fragment:fragment-ktx:1.8.5")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("com.google.android.gms:play-services-location:21.3.0")
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.work:work-runtime-ktx:2.10.0")
}
