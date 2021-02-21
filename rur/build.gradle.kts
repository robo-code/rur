version = "1.1"
group = "net.sf.robocode"

plugins {
    `java-library`
    `maven-publish`
}
java{
    sourceCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    jcenter()
    mavenLocal()
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

dependencies {
    implementation("net.sf.robocode:robocode.api:1.9.4.0")

    testImplementation("junit:junit:4.13")
    testImplementation("net.sf.robocode:robocode.core:1.9.4.0")
    testImplementation("net.sf.robocode:robocode.host:1.9.4.0")
    testImplementation("net.sf.robocode:robocode.repository:1.9.4.0")
    testImplementation("net.sf.robocode:robocode.battle:1.9.4.0")
    testImplementation("net.sf.robocode:robocode.ui:1.9.4.0")
    testImplementation("net.sf.robocode:robocode.main:1.9.4.0")
    testImplementation("net.sf.robocode:robocode.sound:1.9.4.0")
    testRuntimeOnly("net.sf.robocode:robocode.samples:1.9.4.0")
}

tasks {
    processResources{
        expand("version" to project.version)
    }
    register("copySamples", Copy::class) {
        from({
            configurations.testRuntimeClasspath.get()
                .filter { it.name.endsWith("jar") && it.name.contains("robocode.samples") }.map {
                zipTree(it)
            }
        })
        into("../.sandbox/robots")
    }
    test {
        dependsOn("copySamples")
    }
}