//import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdk = Versions.compileSdk

    defaultConfig {
        minSdk = Versions.minSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
                )
            )
        }
    }
    lint.abortOnError = false

    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res","src/main/res-i18n")
        }
    }
    namespace = "com.stardust"
}

dependencies {
    // MQTT
    implementation(libs.org.eclipse.paho.client.mqttv3)
    implementation(libs.org.eclipse.paho.android.service)

    androidTestImplementation(libs.espresso.core) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)
//    api(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    api(libs.androidx.annotation)
    api("com.github.hyb1996:settingscompat:1.1.5")
    implementation(libs.androidx.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    api(kotlin("reflect", version = "1.7.10"))
}
