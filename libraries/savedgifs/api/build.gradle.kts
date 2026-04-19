plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.libraries.savedgifs.api"
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(projects.libraries.matrix.api)
}
