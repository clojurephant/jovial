plugins {
  id("dev.clojurephant.clojure")
  id("java-library")
  id("maven-publish")

  id("org.ajoberstar.reckon")
  id("com.diffplug.spotless")
}

group = "dev.clojurephant"

reckon {
  setDefaultInferredScope("patch")
  stages("alpha", "beta", "rc", "final")
  setScopeCalc(calcScopeFromProp().or(calcScopeFromCommitMessages()))
  setStageCalc(calcStageFromProp())
}

java {
  withSourcesJar()
}

sourceSets {
  val sample by creating {}

  val test by existing {
    runtimeClasspath = runtimeClasspath.plus(sample.getOutput())
  }
}

clojure {
  builds {
    val main by existing {
      reflection.set("fail")
    }
    val sample by existing {
      aotNamespaces.add("sample.other-test")
    }
  }
}

dependencies {
  api("org.junit.platform:junit-platform-engine:latest.release")
  api("org.clojure:clojure:latest.release")
  api("org.clojure:tools.namespace:latest.release")

  testImplementation("junit:junit:latest.release")
  testImplementation("org.junit.platform:junit-platform-launcher:latest.release")

  "sampleImplementation"("org.clojure:clojure:latest.release")
}

tasks.named<Jar>("jar") {
  manifest {
    attributes(mapOf(
      "Implementation-Title" to project.name,
      "Implementation-Version" to project.version,
      "Automatic-Module-Name" to "org.ajoberstar.jovial"))
  }
}

tasks.named<Test>("test") {
  classpath = classpath.plus(files("src/sample/clojure"))
  systemProperty("classpath.roots", file("src/sample/clojure"))
}

////////////////////////////////////////////////////////////////////////////////
// Linting
////////////////////////////////////////////////////////////////////////////////
spotless {
  java {
    importOrder("java", "javax", "")
    removeUnusedImports()
    eclipse().configFile(project.rootProject.file("gradle/eclipse-java-formatter.xml"))
  }
}

////////////////////////////////////////////////////////////////////////////////
// Dependency locking
////////////////////////////////////////////////////////////////////////////////
sourceSets.configureEach {
  configurations[compileClasspathConfigurationName].resolutionStrategy.activateDependencyLocking()
  configurations[runtimeClasspathConfigurationName].resolutionStrategy.activateDependencyLocking()
}

tasks.register("lock") {
  doFirst {
    assert(gradle.startParameter.isWriteDependencyLocks())
  }
  doLast {
    sourceSets.all {
      configurations[compileClasspathConfigurationName].resolve()
      configurations[runtimeClasspathConfigurationName].resolve()
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
// Publishing
////////////////////////////////////////////////////////////////////////////////

publishing {
  repositories {
    maven {
      name = "Clojars"
      url = uri("https://repo.clojars.org/")
      credentials {
        username = System.getenv("CLOJARS_USER")
        password = System.getenv("CLOJARS_TOKEN")
      }
    }
  }

  publications {
    create<MavenPublication>("main") {
      from(components["java"])

      versionMapping {
        usage("java-api") { fromResolutionOf("runtimeClasspath") }
        usage("java-runtime") { fromResolutionResult() }
      }

      pom {
        name.set(project.name)
        description.set(project.description)
        url.set("https://github.com/clojurephant/jovial")

        developers {
          developer {
            name.set("Andrew Oberstar")
            email.set("andrew@ajoberstar.org")
          }
        }

        licenses {
          license {
            name.set("MIT License")
            url.set("https://github.com/clojurephant/jovial/blob/main/LICENSE")
          }
        }

        scm {
          url.set("https://github.com/clojurephant/jovial")
          connection.set("scm:git:git@github.com:clojurephant/jovial.git")
          developerConnection.set("scm:git:git@github.com:clojurephant/jovial.git")
        }
      }
    }
  }
}

// Clojars doesn't support module metadata yet
tasks.withType<GenerateModuleMetadata>() {
  enabled = false
}
