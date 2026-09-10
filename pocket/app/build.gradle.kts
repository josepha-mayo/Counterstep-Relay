plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 buildFeatures { buildConfig = true }
 namespace = "dev.joseph.countersteppocket"
 compileSdk = 35
 defaultConfig {
  applicationId = "dev.joseph.countersteppocket"
  minSdk = 26
  targetSdk = 35
  versionCode = 3
  versionName = "0.3.0-preview"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 testOptions { unitTests.isReturnDefaultValues = true; animationsDisabled = true }
}
dependencies {
 implementation("com.revenuecat.purchases:purchases:9.9.0")
 testImplementation("junit:junit:4.13.2")
 testImplementation(kotlin("test"))
 androidTestImplementation("androidx.test:core:1.6.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
 androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
}
