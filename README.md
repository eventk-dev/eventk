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

#### Usage

Once you have an `EventStore` instance, use `withStreamType` to get a `StreamTypeHandler` scoped to a specific stream type.

##### `loadStream` / `useStream`

Load all events for a stream (eagerly or lazily):

```kotlin
// Eagerly loads all events into a list
val events = eventStore.withStreamType(CarStreamType).loadStream(car1StreamId)

// Streams events lazily — resources are released when the block returns
val result = eventStore.withStreamType(CarStreamType).useStream(car1StreamId) { envelopes ->
    envelopes.fold(Car()) { car, envelope -> car + envelope.event }
}
```

Both methods accept an optional `sinceVersion` parameter to load only events after a known version, which is useful when combined with a local cache.

##### `appendStream`

Append new events to a stream with optimistic concurrency control:

```kotlin
val car = events.fold(Car()) { c, e -> c + e.event }
val result = car.handle(CarCommand())
val newVersion = eventStore.withStreamType(CarStreamType)
    .appendStream(car1StreamId, expectedVersion = events.last().version, events = result)
```

Throws `StreamVersionMismatchException` if the stream has been modified since `expectedVersion` was read, allowing callers to retry with a fresh load.

##### `loadStreamForAppend`

Loads and appends atomically under a per-stream mutex, providing pessimistic concurrency control. If all writers use `loadStreamForAppend`, `StreamVersionMismatchException` cannot occur — at the cost of higher contention and lower write throughput.

The method receives the loaded events and an `appendStream` function that can be called at most once inside the block to write new events. The block's return value is propagated back to the caller, making it convenient to return an updated cache state.

Because the lock is held for the duration of the block, the method is designed to be cache-friendly: use `sinceVersion` to load only events not already in your cache, and return the new cache value from the block so it can be applied after the transaction commits:

```kotlin
var cache = eventStore.withStreamType(CarStreamType).loadStream(car1StreamId)

val newCacheValue = eventStore
    .withStreamType(CarStreamType)
    .loadStreamForAppend(car1StreamId, sinceVersion = cache.last().version) { loaded, appendStream ->
        val car = (cache + loaded).fold(Car()) { c, e -> c + e.event }
        val result = car.handle(CarCommand())
        // appendStream returns the newly appended envelopes with assigned versions
        cache + loaded + appendStream(result, emptyMap())
    }

// Cache is updated only after loadStreamForAppend returns (lock released, transaction committed)
cache = newCacheValue
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
