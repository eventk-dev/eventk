#!/usr/bin/env bash

./gradlew -Pversion=0.0.1-SNAPSHOT --parallel --max-workers 8 publishAllPublicationsToMavenCentralRepository
