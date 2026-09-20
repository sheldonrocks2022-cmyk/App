plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID")
    .orElse(System.getenv("GOOGLE_WEB_CLIENT_ID") ?: "")
    .get()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.esn.hub"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.esn.hub"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "ESN_WEBSITE_URL", "\"https://esnoffical.com\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.credentials:credentials:1.7.0-alpha03")
    implementation("androidx.credentials:credentials-play-services-auth:1.7.0-alpha03")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}