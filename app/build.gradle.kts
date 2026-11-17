plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.assignmenttracker"
    compileSdk {
        version = release(37)
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.dhernandez.assignmenttracker"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // SEC-0-1: Login/SignUp are placeholders, not authentication. They are off the
        // launch path in every build, and in release they also refuse to open.
        buildConfigField("boolean", "AUTH_ENABLED", "false")

        // Room writes the schema JSON here; it is committed (architecture §3.2).
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        debug {
            // Kept on in debug only so the parked screens can still be opened by tests
            // (SystemBarInsetsImeTest). Nothing in the UI starts them.
            buildConfigField("boolean", "AUTH_ENABLED", "true")
        }
        release {
            buildConfigField("boolean", "AUTH_ENABLED", "false")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity)
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.arch.core.testing)

    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.arch.core.testing)
}
