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

java {
  withSourcesJar()
}

sourceSets.register("sample")

sourceSets.named("test") {
  val sampleOutput = sourceSets["sample"].getOutput()
  runtimeClasspath = runtimeClasspath.plus(sampleOutput)
}

clojure.builds.named("main") {
  setReflection("fail")
}

clojure.builds.named("sample") {
  sourceSet.set(sourceSets.named("sample"))
  aotNamespaces.add("sample.other-test")
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

  "sampleImplementation"("org.clojure:clojure:latest.release")
}

tasks.jar {
  manifest {
    attributes(mapOf(
      "Implementation-Title" to project.name,
      "Implementation-Version" to project.version,
      "Automatic-Module-Name" to "org.ajoberstar.jovial"))
  }
}

tasks.test {
  classpath = classpath.plus(files("src/sample/clojure"))
  systemProperty("classpath.roots", file("src/sample/clojure"))
}

publishing {
  publications {
    create<MavenPublication>("main") {
      from(components["java"])
    }
  }
}
