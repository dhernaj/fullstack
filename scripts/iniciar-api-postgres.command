#!/bin/zsh
set -eu
cd "$(dirname "$0")/../../academia-api-guia2"
set -a
source ./.env.local
set +a
export SPRING_PROFILES_ACTIVE=postgres
export APP_DEMO_ENABLED=true
exec java -jar target/academia-api-0.0.1-SNAPSHOT.jar \
  '--spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/academia_db?currentSchema=laboratorio3_angular' \
  --spring.jpa.properties.hibernate.default_schema=laboratorio3_angular \
  --server.port=8080
