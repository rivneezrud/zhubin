plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.libraries.savedgifs.test"
}

dependencies {
    api(projects.libraries.savedgifs.api)
    implementation(projects.libraries.matrix.api)
    implementation(libs.coroutines.core)
    implementation(projects.tests.testutils)
}
