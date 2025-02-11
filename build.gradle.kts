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
