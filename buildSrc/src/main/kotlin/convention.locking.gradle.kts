plugins {
  `java-base`
}

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
