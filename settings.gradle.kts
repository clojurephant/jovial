pluginManagement {
  plugins {
    id("dev.clojurephant.clojure") version("0.8.0-beta.4")

    id("org.ajoberstar.reckon.settings") version("0.18.2")
    id("com.diffplug.spotless") version("6.23.3")
  }
}

plugins {
  id("org.ajoberstar.reckon.settings")
}

extensions.configure<org.ajoberstar.reckon.gradle.ReckonExtension> {
  setDefaultInferredScope("patch")
  stages("beta", "rc", "final")
  setScopeCalc(calcScopeFromProp().or(calcScopeFromCommitMessages()))
  setStageCalc(calcStageFromProp())
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven {
      name = "Clojars"
      url = uri("https://repo.clojars.org/")
    }
  }
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "jovial"
