rootProject.name = "confis"
include(
    "lib",
    "script",
    "plugin",
    "docs"
)
pluginManagement {
    repositories {
        maven {
            url = java.net.URI("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        gradlePluginPortal()
    }
}
