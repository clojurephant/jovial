# jovial

[![Bintray](https://img.shields.io/bintray/v/ajoberstar/maven/jovial.svg?style=flat-square)](https://bintray.com/ajoberstar/maven/jovial/_latestVersion)
[![Travis](https://img.shields.io/travis/ajoberstar/jovial.svg?style=flat-square)](https://travis-ci.org/ajoberstar/jovial)
[![Quality Gate](https://sonarqube.ajoberstar.com/api/badges/gate?key=org.ajoberstar.jovial:jovial)](https://sonarqube.ajoberstar.com/dashboard/index/org.ajoberstar.jovial:jovial)
[![GitHub license](https://img.shields.io/github/license/ajoberstar/jovial.svg?style=flat-square)](https://github.com/ajoberstar/jovial/blob/master/LICENSE)

**DISCLAIMER:** JUnit5 is still in early development. Jovial is currently build on the M1 version of JUnit5.

## Why do you care?

Wouldn't it be great to have one test launcher to rule them all? One API that tools need to support and that other test frameworks
could implement, so that neither need to know about the other? [JUnit5](http://junit.org/junit5/)'s launching API has that promise.

All it needs is support from the JVM community to provide:

- Launchers to execute JUnit from new (and the same old) places
- TestEngines to support new (and the same old) testing frameworks

## What is it?

Jovial's main goal is to help provide launchers and engines to extend the support of JUnit5 to new places.

### Current Support

#### Engines

- [clojure.test](https://clojure.github.io/clojure/clojure.test-api.html)

#### Launchers

- [Gradle 3](https://docs.gradle.org/current/userguide/userguide.html)

## Usage

**NOTE:** *All* jovial modules require Java 8 (or higher).

* [Release Notes](https://github.com/ajoberstar/jovial/releases)
* [Full Documentation](https://github.com/ajoberstar/jovial/wiki)

### clojure.test

Run JUnit5 through [your favorite method](http://junit.org/junit5/docs/current/user-guide/#running-tests), just make
sure that you have `org.ajoberstar.jovial:jovial-engine-clojure.test` on the classpath.

See the sample [console](sample-junit-console), [maven](sample-junit-maven), and [gradle](sample-junit-gradle) projects
for more thorough information.

## Gradle 3

... coming soon ...

## Questions, Bugs, and Features

Please use the repo's [issues](https://github.com/ajoberstar/jovial/issues)
for all questions, bug reports, and feature requests.

## Contributing

Contributions are very welcome and are accepted through pull requests.

Smaller changes can come directly as a PR, but larger or more complex
ones should be discussed in an issue first to flesh out the approach.

If you're interested in implementing a feature on the
[issues backlog](https://github.com/ajoberstar/jovial/issues), add a comment
to make sure it's not already in progress and for any needed discussion.

## Acknowledgements

Thanks to the [JUnit5 team](https://github.com/junit-team/junit5/graphs/contributors) for putting together a great new API!
