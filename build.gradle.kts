plugins {
  id("convention.clojars-publish")
  id("convention.lint")
  id("convention.locking")

  id("org.ajoberstar.grgit")
  id("org.ajoberstar.reckon")

  `java-library`
  id("dev.clojurephant.clojure")
}

group = "org.ajoberstar"

reckon {
  scopeFromProp()
  stageFromProp("beta", "rc", "final")
}

repositories {
  mavenCentral()
  maven {
    name = "Clojars"
    url = uri("https://repo.clojars.org/")
  }
}

dependencies {
  implementation("org.junit.platform:junit-platform-engine:latest.release")
  implementation("org.clojure:clojure:latest.release")
  implementation("org.clojure:tools.namespace:latest.release")

  testImplementation("junit:junit:latest.release")
  testImplementation("org.junit.platform:junit-platform-launcher:latest.release")
}

tasks.test {
  classpath = classpath.plus(files("src/test/samples"))
  systemProperty("classpath.roots", file("src/test/samples"))
  testLogging {
    events("passed", "failed")
  }
}
