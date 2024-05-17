plugins {
    id("com.android.application") version "8.3.1"
    id("com.google.gms.google-services") version "4.4.1"
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
}

android {
    namespace = "com.example.turismo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.turismo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    packagingOptions {
        exclude("META-INF/LICENSE")
        exclude("META-INF/NOTICE")
        exclude("META-INF/DEPENDENCIES")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:20.0.0")
    implementation("com.google.android.libraries.places:places:3.3.0")
    implementation("com.github.clans:fab:1.6.4")
    implementation("com.google.maps.android:android-maps-utils:2.3.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.api-client:google-api-client-android:1.32.2")
    implementation("com.google.api-client:google-api-client:1.32.2") {
        exclude(group = "com.google.api.client", module = "google-api-client")
    }
    implementation("com.google.http-client:google-http-client-jackson2:1.41.5") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.google.http-client:google-http-client-android:1.41.5")
    implementation("com.google.apis:google-api-services-calendar:v3-rev411-1.25.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.android.gms:play-services-auth:20.3.0") // Added dependency for Google Auth
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.glide)
}