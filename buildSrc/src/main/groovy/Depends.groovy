class Depends {
    static final androidx = new AndroidX()
    static final google = new Google()
    static final kotlin = new Kotlin()
    static final kotlinResult = 'com.michael-bull.kotlin-result:kotlin-result:1.1.9'
    static final okhttp = 'com.squareup.okhttp3:okhttp:4.9.0'
    static final test = new Test()

    static class AndroidX {
        final activity = 'androidx.activity:activity-ktx:1.2.1'
        final annotation = 'androidx.annotation:annotation:1.1.0'
        final constraint = 'androidx.constraintlayout:constraintlayout:2.0.4'
        final datastore = 'androidx.datastore:datastore-core:1.0.0-alpha08'
        final fragment = 'androidx.fragment:fragment-ktx:1.3.1'
        final leanback = 'androidx.leanback:leanback:1.0.0'
        final lifecycleVersion = '2.3.0'
        final lifecycleLiveData = "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion"
        final lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion"
        final navigationVersion = '2.3.4'
        final navigationFragment = "androidx.navigation:navigation-fragment-ktx:$navigationVersion"
        final navigationUi = "androidx.navigation:navigation-ui-ktx:$navigationVersion"
        final recyclerView = "androidx.recyclerview:recyclerview:1.1.0"
        final roomVersion = '2.2.6'
        final roomCompiler = "androidx.room:room-compiler:$roomVersion"
        final roomKtx = "androidx.room:room-ktx:$roomVersion"
        final roomRuntime = "androidx.room:room-runtime:$roomVersion"
    }

    static class Google {
        final androidOssLicenses = 'com.google.android.gms:play-services-oss-licenses:17.0.0'
        final hiltVersion = '2.33-beta'
        final hilt = "com.google.dagger:hilt-android:$hiltVersion"
        final hiltCompiler = "com.google.dagger:hilt-android-compiler:$hiltVersion"
        final material = 'com.google.android.material:material:1.2.1'
        final protobufVersion = '3.15.3'
        final protoc = "com.google.protobuf:protoc:$protobufVersion"
        final protobufLite = "com.google.protobuf:protobuf-javalite:$protobufVersion"
    }

    static class Kotlin {
        final coroutinesVersion = '1.4.2'
        final coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
        final coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
        final stdlib = 'org.jetbrains.kotlin:kotlin-stdlib:1.4.31'
        final serializationJson = 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0'
    }

    static class Test {
        final androidJunit4Ktx = 'androidx.test.ext:junit-ktx:1.1.2'
        final junit = 'junit:junit:4.12'
        final robolectric = 'org.robolectric:robolectric:4.4'
        final testVersion = '1.3.0'
        final testCoreKtx = "androidx.test:core-ktx:$testVersion"
        final testRunner = "androidx.test:runner:$testVersion"
        final testRules = "androidx.test:rules:$testVersion"
        final truthVersion = '1.1'
        final truth = "com.google.truth:truth:$truthVersion"
        final truthJava8 = "com.google.truth.extensions:truth-java8-extension:$truthVersion"
    }
}
