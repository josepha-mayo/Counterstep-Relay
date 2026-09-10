plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "dev.joseph.countersteppocket"
 compileSdk = 35
 defaultConfig {
  applicationId = "dev.joseph.countersteppocket"
  minSdk = 26
  targetSdk = 35
  versionCode = 1
  versionName = "0.1.0-prototype"
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
 implementation("com.revenuecat.purchases:purchases:9.9.0")
 testImplementation("junit:junit:4.13.2")
}
