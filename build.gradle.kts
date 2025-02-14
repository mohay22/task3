// Project-level Gradle (settings for all modules)
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0") // Firebase Plugin
    }
}

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    id("org.sonarqube") version "4.3.1.3277"

}

sonarqube {
    properties {
        property ("sonar.projectKey", "task3")
        property ("sonar.host.url", "http://localhost:9000")
        property ("sonar.token", "sqp_1a77f0bc416924c98eb07e928a4a60601b391132")
    }
}