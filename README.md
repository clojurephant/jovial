# jupiter

[![Bintray](https://img.shields.io/bintray/v/ajoberstar/libraries/jupiter.svg?style=flat-square)](https://bintray.com/ajoberstar/libraries/jupiter/_latestVersion)
[![Travis](https://img.shields.io/travis/ajoberstar/jupiter.svg?style=flat-square)](https://travis-ci.org/ajoberstar/jupiter)
[![GitHub license](https://img.shields.io/github/license/ajoberstar/jupiter.svg?style=flat-square)](https://github.com/ajoberstar/jupiter/blob/master/LICENSE)

## Why do you care?

[JUnit5](http://junit.org/junit5/)'s best new feature isn't the lambda support, it's the new launching API. It's extensible from two sides:

- Support for additional test frameworks is implemented through a `TestEngine`
- Support for additional tooling goes through the `Launcher` API

Importantly, the `TestEngine` can be implemented independently of the test framework itself. Neither the test framework
nor the developer writing tests in that framework need to know (or care) that it's being executed through a JUnit engine.

Additionally, the tools using the `Launcher` don't need to have knowledge of the available engines. They just execute
through the launcher, and whatever engines are on the classpath get used to discover and execute tests.

## What is it?

Jupiter's main goal is to build on JUnit5's launching/engine APIs to provide support for running JUnit in more places
and running more than just JUnit.

While my initial test framework support is targeted at Clojure, that isn't meant to imply that's the only goal.

## Usage

**NOTE:** *All* jupiter modules require Java 8 (or higher).

* [Release Notes](https://github.com/ajoberstar/jupiter/releases)
* [Full Documentation](https://github.com/ajoberstar/jupiter/wiki)

### Current Support

#### Engines

- [clojure.test](https://clojure.github.io/clojure/clojure.test-api.html)

#### Launchers

- [Gradle 3](https://docs.gradle.org/current/userguide/userguide.html) -- coming soon...

## Questions, Bugs, and Features

Please use the repo's [issues](https://github.com/ajoberstar/jupiter/issues)
for all questions, bug reports, and feature requests.

## Contributing

Contributions are very welcome and are accepted through pull requests.

Smaller changes can come directly as a PR, but larger or more complex
ones should be discussed in an issue first to flesh out the approach.

If you're interested in implementing a feature on the
[issues backlog](https://github.com/ajoberstar/jupiter/issues), add a comment
to make sure it's not already in progress and for any needed discussion.

## Acknowledgements

Thanks to the [JUnit5 team](https://github.com/junit-team/junit5/graphs/contributors) for putting together a great new API!
