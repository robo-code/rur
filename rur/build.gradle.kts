version = "1.1"
group = "net.sf.robocode"

plugins {
    `java-library`
    `maven-publish`
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    mavenLocal()
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}
val robocodeVersion = "1.9.4.3"
dependencies {
    implementation("net.sf.robocode:robocode.api:$robocodeVersion")
    testImplementation("net.sf.robocode:robocode.core:$robocodeVersion")
    testImplementation("net.sf.robocode:robocode.host:$robocodeVersion")
    testImplementation("net.sf.robocode:robocode.repository:$robocodeVersion")
    testImplementation("net.sf.robocode:robocode.battle:$robocodeVersion")
    testImplementation("net.sf.robocode:robocode.ui:$robocodeVersion")
    testImplementation(project(":robocode.plugin.kusto"))
    testRuntimeOnly("net.sf.robocode:robocode.samples:$robocodeVersion")
    testImplementation("junit:junit:4.13")
}

tasks {
    processResources {
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