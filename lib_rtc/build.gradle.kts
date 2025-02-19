plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}
//apply(from = rootProject.file("maven-publish.gradle.kts"))

android {
    namespace = "com.sensetime.lib_rtc"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    compileOnly(libs.androidx.core.ktx)
    compileOnly(libs.androidx.appcompat)
    compileOnly(libs.material)

    implementation(libs.jjwt.api)
    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.jackson)
    implementation(libs.rtc)

    implementation(libs.okhttp)
}

// 🟢 生成源码 Jar
val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

afterEvaluate {
    // 🟢 Maven 发布配置
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.engineer.third"
                artifactId = "thirdlib"
                version = "1.0.0"

//                artifact(sourceJar.get()) // 上传源码
            }
        }

        repositories {
            mavenLocal()
            maven { url = uri("${rootProject.rootDir}/local_repo/") }
        }
    }
}

// 🟢 解决 Gradle 8.0+ 任务依赖问题
tasks.withType<GenerateModuleMetadata>().configureEach {
    dependsOn(sourceJar)
}
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(sourceJar)
}