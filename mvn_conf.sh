#!/usr/bin/env bash
set -x
cd ./moses-base-api && mvn clean install -DskipTests 
cd ../moses-common && mvn clean install -DskipTests
cd ../moses-conf && mvn clean install -DskipTests

