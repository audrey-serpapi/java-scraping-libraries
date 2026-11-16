plugins {
    java
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

// Versions are starting points: bump to the latest releases before publishing.
dependencies {
    implementation("de.hs-heilbronn.mi:crawler4j-with-hsqldb:5.1.4")
    implementation("org.jsoup:jsoup:1.21.1")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("com.microsoft.playwright:playwright:1.52.0")
    implementation("org.seleniumhq.selenium:selenium-java:4.33.0")
    implementation("org.htmlunit:htmlunit:5.3.0")
    implementation("us.codecraft:webmagic-core:1.0.3")
    implementation("org.jsoup:jsoup:1.18.1")

    // Jaunt isn't on Maven Central: download the jar from jaunt-api.com into libs/
    implementation(fileTree("libs") { include("*.jar") })
}

// Run any example by class name: ./gradlew runExample -Pexample=JsoupExample
tasks.register<JavaExec>("runExample") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(providers.gradleProperty("example").map { "examples.$it" })
}