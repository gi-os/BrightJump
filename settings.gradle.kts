pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // Deliberately no GitHub Packages repository here. Every other app in the fleet pulls
    // light-common for shake-to-report and the shared type scale; this one draws nothing and
    // lives for about 200 ms, so there is no screen to shake and no text to style. Skipping the
    // dependency also skips the GPR token handshake that has broken CI in three other repos.
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "BrightJump"
include(":app")
