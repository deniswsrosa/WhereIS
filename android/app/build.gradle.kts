import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Upload-key credentials live OUTSIDE version control (android/keystore.properties,
// git-ignored; the keystore itself under ~/keystores). Release signing activates only
// when the file exists, so clean checkouts still build debug without it.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.acme.clara"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.acme.clara"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.3"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
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
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    androidResources {
        // Keep MIDI + WAV SFX in assets/audio/ stored uncompressed so GameSound can openFd() them.
        noCompress += "mid"
        noCompress += "wav"
    }
    testOptions {
        // The ViewModel calls android.util.Log; return stub defaults instead of throwing
        // so the pure game logic can run under plain JVM unit tests.
        unitTests.isReturnDefaultValues = true
        // Robolectric needs the merged Android resources to drive the Compose UI tests.
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Latest stable Compose line that supports this app's Play-targeted API 36 toolchain.
    // Compose 1.12 requires compileSdk 37 / AGP 9.1, which would opt this release into a
    // not-yet-targeted platform generation.
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    // Billing's Play Services graph still requests Fragment 1.1.0. Pin a modern runtime so
    // ComponentActivity's Activity Result API is not paired with the pre-1.3 fragment bridge.
    implementation("androidx.fragment:fragment:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.work:work-runtime:2.11.2")
    // World Campaign one-time unlock — see billing/BillingManager.kt and
    // docs/06-play-console-iap-setup.md for the matching Play Console product setup.
    implementation("com.android.billingclient:billing-ktx:9.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation(composeBom)
    testImplementation("androidx.compose.runtime:runtime")
    // Robolectric + Compose UI test: run instrumentation-style UI checks on the JVM.
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    // Compose UI tests live in src/testDebug: ui-test-manifest intentionally never enters
    // the production release manifest, while pure logic/Robolectric tests run in both variants.
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
