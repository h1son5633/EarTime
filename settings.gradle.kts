pluginManagement {
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // google() first: Aliyun's public repo intermittently answers 502
        // (instead of 404) for Google artifacts, which poisons resolution.
        google()
        maven("https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
}

rootProject.name = "EarTime"
include(":app")
