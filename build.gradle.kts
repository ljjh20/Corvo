import org.gradle.internal.os.OperatingSystem.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    maven
    kotlin("jvm") version "1.4.20"
}

group = "graphics.scenery"
version = "0.1.0-SNAPSHOT"

description = "scenery-dimensional-reduction"

//sourceCompatibility = 1.8
//targetCompatibility = 1.8
//tasks.withType(JavaCompile) {
//	options.encoding = 'UTF-8'
//}
//
//configurations.all {
//}
//
repositories {
    mavenCentral()
    maven("https://maven.scijava.org/content/groups/public")
    maven("https://jitpack.io")
    maven("http://nexus.senbox.net/nexus/content/groups/public/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.scenerygraphics:scenery:886a7492")
    implementation("junit:junit:4.12")
    val lwjglNative = "natives-" + when(current()) {
        WINDOWS -> "windows"
        LINUX -> "linux"
        else -> "macos"
    }
    listOf("", "-glfw", "-jemalloc", "-vulkan", "-opengl", "-openvr", "-xxhash", "-remotery").forEach {
        implementation("org.lwjgl:lwjgl$it:3.2.3")
        if(it != "-vulkan")
            runtimeOnly("org.lwjgl", "lwjgl$it", version = "3.2.3", classifier = lwjglNative)
    }
    implementation("org.joml:joml:1.9.25")
    implementation("org.janelia.saalfeldlab:n5-imglib2:3.5.1")
    implementation("org.jogamp.gluegen:gluegen-rt:2.3.2")
    implementation("org.jogamp.jogl:jogl-all:2.3.2")
    implementation("org.scijava:ui-behaviour:2.0.3")
    implementation("graphics.scenery:spirvcrossj:0.7.0-1.1.106.0")
    implementation("com.bc.zarr:jzarr:0.3.3-SNAPSHOT")
    implementation("org.nd4j:nd4j-api:1.0.0-beta7")
    implementation("org.nd4j:nd4j-native-platform:1.0.0-beta7")
    implementation("org.janelia.saalfeldlab:n5:2.3.0")
    implementation("org.janelia.saalfeldlab:n5-zarr:0.0.6")
    runtimeOnly("graphics.scenery", "spirvcrossj", version = "0.7.0-1.1.106.0", classifier = lwjglNative)
    testImplementation("junit:junit:4.13")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
        sourceCompatibility = "11"
    }

    test {
        useJUnitPlatform()
    }
}