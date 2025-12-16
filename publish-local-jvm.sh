#!/usr/bin/env bash

./gradlew -Pversion=local-SNAPSHOT \
   publishJvmPublicationToMavenLocal \
   :hex-arch-adapters-spring6:publishToMavenLocal \

