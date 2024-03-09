set -x
cd ./moses-base-api && mvn clean install -DskipTests 
cd ../moses-common && mvn clean install -DskipTests
cd ../moses-queue && mvn clean install -DskipTests

