#!/usr/bin/env bash

./gradlew -Pversion=$1 --parallel --max-workers 8 \
    clean \
    publishJvmPublicationToMavenCentralRepository \
    :hex-arch-adapters-spring6:publish \
