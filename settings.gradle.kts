pluginManagement {
    repositories {
        // CI runners reach Google / Maven Central / the Plugin Portal directly.
        // The Aliyun gradle-plugin mirror answered with a broken response for
        // the KSP plugin marker and poisoned plugin resolution (run 44505718352),
        // so no Aliyun route is used here at all.
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EarTime"
include(":app")
