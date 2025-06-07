plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlytics)


}

android {
    namespace = "com.eriknivar.firebasedatabase"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eriknivar.firebasedatabase"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }


}

apply(plugin = "com.google.gms.google-services")

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth) // Para Firebase Auth
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.icons.extended)
    implementation(libs.zxing.android.embedded)
    implementation(libs.coil.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.apache.poi)
    implementation(libs.poi.ooxml)
    implementation(libs.androidx.material3.v121)
    implementation(libs.coil.compose.v250)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.identity.jvm)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.fragment)
    implementation(libs.ui)
    implementation(libs.coil.compose.v222)




    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
