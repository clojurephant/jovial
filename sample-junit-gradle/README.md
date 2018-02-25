# JUnit Gradle Sample

[Official JUnit Gradle Docs](http://junit.org/junit5/docs/current/user-guide/#build-support)

This project is a sample of the official JUnit5 Gradle plugin. There will be a Jovial Gradle plugin beginning in 0.2.0
to provide support for the Gradle 3 model.

Sample `clojure.test` tests are included in the `src/test/resources` directory.

Run the tests with:

```
./gradlew junitPlatformTest
```

You'll see the basic console output from JUnit with any failures or messages from `clojure.test`.
