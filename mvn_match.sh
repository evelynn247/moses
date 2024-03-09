#!/usr/bin/env bash
set -x
cd ./moses-base-api && mvn clean install -DskipTests 
cd ../moses-common && mvn clean install -DskipTests
cd ../moses-exp && mvn clean install -DskipTests
cd ../moses-match && mvn clean install -DskipTests

