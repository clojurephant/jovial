plugins {
  `kotlin-dsl`
}

repositories {
  jcenter()
  maven(url = "https://repo.clojars.org")
}

dependencies {
  implementation("com.diffplug.spotless:spotless-plugin-gradle:5.9.0")
}
