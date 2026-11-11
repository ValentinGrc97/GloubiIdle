plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.vgarcia.gloubi"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.vgarcia.gloubi"
        minSdk = 24
        targetSdk = 37
        versionCode = 4
        versionName = "1.0.3"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            isDebuggable = true
            isMinifyEnabled = false
            // IDs AdMob de TEST Google
            buildConfigField("String", "ADMOB_APP_ID",      "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_BANNER_ID",   "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_REWARDED_BOOST_ID",    "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "ADMOB_REWARDED_BIG_FEED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "ADMOB_REWARDED_AUTO_ID",     "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("Boolean", "IS_DEBUG", "true")
            resValue("string", "app_name", "Gloubi DEV")
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ADMOB_APP_ID",      "\"ca-app-pub-5168307355330093~3429507831\"")
            buildConfigField("String", "ADMOB_BANNER_ID",   "\"ca-app-pub-5168307355330093/8490262824\"")
            buildConfigField("String", "ADMOB_REWARDED_BOOST_ID",    "\"ca-app-pub-5168307355330093/9514951336\"")
            buildConfigField("String", "ADMOB_REWARDED_BIG_FEED_ID", "\"ca-app-pub-5168307355330093/2323776894\"")
            buildConfigField("String", "ADMOB_REWARDED_AUTO_ID",     "\"ca-app-pub-5168307355330093/4904788337\"")
            buildConfigField("Boolean", "IS_DEBUG", "false")
            resValue("string", "app_name", "Gloubi")
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-5168307355330093~3429507831"
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
}
}

dependencies {
    implementation("androidx.compose.foundation:foundation-layout:1.12.1")
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.12.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.12.1")

    val room = "2.8.5"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // AdMob (inclut le SDK UMP pour le consentement RGPD)
    implementation("com.google.android.gms:play-services-ads:25.5.0")
}