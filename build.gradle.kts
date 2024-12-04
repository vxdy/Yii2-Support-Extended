plugins {
    id("java")
//    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij") version "1.17.3"
    id("org.jetbrains.changelog") version "2.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("idea")
}

// Import variables from gradle.properties file
// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
val pluginName_: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginVerifierIdeVersions: String by project

val platformType: String by project
val platformVersion: String by project
val platformDownloadSources: String by project

group = "com.nvlad"
version = pluginVersion

println("Plugin Version: $pluginVersion")


val platformPluginsAssociation = hashMapOf<String, String>()
platformPluginsAssociation["2019.1.4"] =
    "com.jetbrains.php:191.8026.56, org.jetbrains.plugins.phpstorm-remote-interpreter:191.5849.22, com.jetbrains.twig:191.6183.95"
platformPluginsAssociation["2020.2.3"] =
    "com.jetbrains.php:202.7660.42, org.jetbrains.plugins.phpstorm-remote-interpreter:202.6397.59, com.jetbrains.twig:202.6397.21"
platformPluginsAssociation["2020.3.3"] =
    "com.jetbrains.php:203.7717.11, org.jetbrains.plugins.phpstorm-remote-interpreter:203.5981.155, com.jetbrains.twig:203.6682.75"
platformPluginsAssociation["2021.1"] =
    "com.jetbrains.php:211.6693.120, org.jetbrains.plugins.phpstorm-remote-interpreter:211.6693.65, com.jetbrains.twig:211.6693.44, PsiViewer:211-SNAPSHOT"
platformPluginsAssociation["2021.2"] =
    "com.jetbrains.php:212.4746.92, org.jetbrains.plugins.phpstorm-remote-interpreter:212.4746.52, com.jetbrains.twig:212.4746.57, PsiViewer:212-SNAPSHOT"
platformPluginsAssociation["2022.3"] =
    "com.jetbrains.php:223.8617.59, org.jetbrains.plugins.phpstorm-remote-interpreter:223.7571.117, com.jetbrains.twig:223.8617.59, PsiViewer:2022.3"

platformPluginsAssociation["2023.1.4"] =
    "com.jetbrains.php:231.9225.18, org.jetbrains.plugins.phpstorm-remote-interpreter:231.8770.17, com.jetbrains.twig:231.9225.21, PsiViewer:231-SNAPSHOT, com.intellij.properties:231.8770.3"

platformPluginsAssociation["2024.1"] =
    "com.jetbrains.php:241.14494.237, org.jetbrains.plugins.phpstorm-remote-interpreter, com.jetbrains.twig, PsiViewer:241.14494.158-EAP-SNAPSHOT, com.intellij.properties:241.14494.150"

val bundledPlugins = "DatabaseTools, webDeployment, com.intellij.css, terminal"
val platformPlugins = platformPluginsAssociation[platformVersion] + ", $bundledPlugins"

repositories {
    mavenCentral()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.14.2")

    implementation("io.sentry:sentry:1.7.12") {
        exclude("org.slf4j", "slf4j-api")
        exclude("com.fasterxml.jackson.core", "jackson-core")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
}

intellij {
    version.set(platformVersion)
    type.set("PS")
    plugins.set(listOf(*platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty).toTypedArray()))
}

sourceSets {
    main {
        java {
            srcDirs("src")
//            assemble "com.rollbar:rollbar-java:1.3.1"
        }
        resources {
            srcDirs("resources")
        }
    }
    test {
        java {
            srcDirs("tests")
        }
    }
}

tasks {
    named<Zip>("buildPlugin") {
        archiveFileName.set("yii2support.zip")
    }
    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        version.set(pluginVersion)
        untilBuild.set("")
    }

//    runPluginVerifier  {
//        ideVersions.set(listOf(*pluginVerifierIdeVersions.split(',').map(String::trim).filter(String::isNotEmpty).toTypedArray()))
//    }
    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://jetbrains.org/intellij/sdk/docs/tutorials/build_system/deployment.html#specifying-a-release-channel
        channels.set(listOf("beta"))
    }
}