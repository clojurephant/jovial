# jovial

JUnit Platform engines for Clojure test frameworks

[![Clojars Project](https://img.shields.io/clojars/v/dev.clojurephant/jovial.svg)](https://clojars.org/dev.clojurephant/jovial)
![CI](https://github.com/clojurephant/jovial/workflows/CI/badge.svg)

**DISCLAIMER:** Before 0.4.0 this was `org.ajoberstar:jovial`. It provides the same functionality, was just a namespacing change.

## Why do you care?

The [JUnit Platform](https://junit.org/junit5/) is a de-factor standard in the Java community. In polyglot projects, there is advantage in running everything through the same test runner. Or maybe you'd like to "embrace the host" and utlize JUnit as a key part of the JVM ecosystem.

## What is it?

A JUnit Platform test engine for Clojure testing frameworks.

Currently supporting:

- [clojure.test](https://clojure.github.io/clojure/clojure.test-api.html)

See the [Release Notes](https://github.com/ajoberstar/jovial/releases) for updates.

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
