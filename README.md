# Eventk

Eventk (pronounced /ɪˈvɛntɪk/) aims to provide a toolkit that makes working with event sourcing much easier.
Some related architectural patterns, such as CQRS and DDD, will also be covered.

We aim to support all Kotlin targets relevant for server-side development, such as Kotlin/JVM, Kotlin/Native (Linux and macOS, arm64 and amd64) and Kotlin/JS.

A more detailed documentation and possibly a long-term roadmap coming soon :)

## Development

If you need to make changes, you can edit code however you like and test changes with:
```shell
./gradlew test
```

If you need to publish locally to test on other projects, you can use the following:

```shell
./publish-local.sh
```

If you're interested only in the JVM artefacts, you can run:

```shell
./publish-jvm-local.sh
```

You can then change your dependency version to `local-SNAPSHOT` and make sure you have
the local maven repository enable if you're using Gradle, and your external project
should see your local changes.
