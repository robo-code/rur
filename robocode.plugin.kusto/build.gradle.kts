plugins {
    `java-library`
    `maven-publish`
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

description = "Robocode Kusto experimental module"

version = "1.9.4.3"
group = "net.sf.robocode"

repositories {
    mavenCentral()
    mavenLocal()
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}



val robocodeVersion = "1.9.4.1"
dependencies {
    implementation("net.sf.robocode:robocode.api:$robocodeVersion")
    implementation("net.sf.robocode:robocode.core:$robocodeVersion")
    implementation("net.sf.robocode:robocode.battle:$robocodeVersion")
    implementation("net.sf.robocode:robocode.roborumble:$robocodeVersion")
    implementation("org.picocontainer:picocontainer:2.14.2")
    implementation(project(":rur"))
    implementation("com.microsoft.azure:azure-storage:8.6.6")
    testImplementation("junit:junit:4.13")
}

tasks {
    register("copyDeps", Copy::class) {
        from({
            configurations.runtimeClasspath.get()
                    .filter { it.name.endsWith("jar") }
        })
        into("../.sandbox/libs")
    }
    register("copyDeps2", Copy::class) {
        from({
            configurations.runtimeClasspath.get()
                    .filter { it.name.endsWith("jar") }
        })
        into("../build/libs")
    }
    jar{
        dependsOn("copyDeps")
        dependsOn("copyDeps2")
    }

    task<Exec>("dockerBuild") {
        dependsOn("jar")
        workingDir = file("../")
        commandLine("docker", "build", "-t", "zamboch/kustorumble:${project.version}" , "-t", "zamboch/kustorumble:latest", ".")
    }

    task<Exec>("dockerPush") {
        dependsOn("dockerBuild")
        workingDir = file("../")
        commandLine("docker", "push", "zamboch/kustorumble","--all-tags")
    }

    build {
        dependsOn("dockerBuild")
    }

}