# Eventk

Eventk (pronounced /ɪˈvɛntɪk/) aims to provide a toolkit that makes working with event sourcing much easier.
Some related architectural patterns, such as CQRS and DDD, will also be covered.

We aim to support all Kotlin targets relevant for server-side development, such as Kotlin/JVM, Kotlin/Native (Linux and macOS, arm64 and amd64) and Kotlin/JS.

A more detailed documentation and possibly a long-term roadmap coming soon :)

## Components

These are the artifacts currently published to Maven Central.

### Event Store

The event store is the core of the library, providing a way to store events and retrieve them later.
The API is designed to be as simple as possible, while still providing enough flexibility to cover most use cases.

#### Maven

```xml
<dependency>
    <groupId>dev.eventk</groupId>
    <artifactId>event-store-api-jvm</artifactId>
    <version>0.0.1</version>
</dependency>

<!-- select one of the implementations below -->
<dependency>
    <groupId>dev.eventk</groupId>
    <artifactId>event-store-impl-memory-jvm</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>dev.eventk</groupId>
    <artifactId>event-store-impl-fs-jvm</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>dev.eventk</groupId>
    <artifactId>event-store-impl-postgresql-jvm</artifactId>
    <version>0.0.1</version>
</dependency>
```

#### Gradle

```kotlin
dependencies {
    implementation("dev.eventk:event-store-api-jvm:0.0.1")
    // select one of the implementations below
    implementation("dev.eventk:event-store-impl-memory-jvm:0.0.1")
    implementation("dev.eventk:event-store-impl-fs-jvm:0.0.1")
    implementation("dev.eventk:event-store-impl-postgresql-jvm:0.0.1")
}
```

### Hexagonal Architecture Helpers

Besides the core event store, we also provide some helpers to make working with hexagonal architecture easier.
Those modules are designed to provide some ports and adapters to handle common use cases
such as event listening, bookmarking, etc.

#### Maven

```xml
<!-- Port contracts that should be added to your domain module -->
<dependency>
    <groupId>dev.eventk</groupId>
    <artifactId>hex-arch-ports-jvm</artifactId>
    <version>0.0.1</version>
</dependency>

<!-- Framework-specific adapters (use only what you need) -->
<dependency>
    <groupId>dev.eventk</groupId>
    <artifactId>hex-arch-adapters-spring6</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>dev.eventk</groupId>
    <artifactId>hex-arch-adapters-ktor-jvm</artifactId>
    <version>0.0.1</version>
</dependency>
```

#### Gradle
```kotlin
dependencies {
    // Port contracts that should be added to your domain module
    implementation("dev.eventk:hex-arch-ports-jvm:0.0.1")

    // Framework-specific adapters (use only what you need)
    implementation("dev.eventk:hex-arch-adapters-spring6:0.0.1")
    implementation("dev.eventk:hex-arch-adapters-ktor-jvm:0.0.1")
}
```

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
./publish-local-jvm.sh
```

You can then change your dependency version to `local-SNAPSHOT` and make sure you have
the local maven repository enable if you're using Gradle, and your external project
should see your local changes.

## Publishing

For now, we are publishing non-SNAPSHOT versions just the JVM artifacts to Maven Central.
If you have interest in publishing other targets, please open an issue and I'll start doing it.

```shell
./publish-jvm.sh x.y.z
```
