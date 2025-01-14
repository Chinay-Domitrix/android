plugins {
    id("com.android.application")
}

android {
    compileSdkVersion(19)
    buildToolsVersion("19.0.0")

    defaultConfig {
        minSdkVersion(7)
        targetSdkVersion(19)
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    compile("<error descr="This support library should not use a different version (18) than the `compileSdkVersion` (19)">com.android.support:support-v4:18.0.0</error>")

    // Suppressed:
    //noinspection GradleCompatible
    compile("com.android.support:support-v4:18.0.0")
}
