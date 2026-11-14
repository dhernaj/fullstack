#!/bin/zsh
# Validación adicional sin Docker en un esquema propio; requiere prepararlo antes.
set -eu
cd "$(dirname "$0")/../academia-api"
set -a
source ./.env.local
lab4_pg_password="$DB_PASSWORD"
lab4_pg_user="$DB_USERNAME"
source ../.env
export DB_PASSWORD="$lab4_pg_password"
export DB_USER="$lab4_pg_user"
export DB_URL='jdbc:postgresql://127.0.0.1:5432/academia_db?currentSchema=laboratorio4_prod_prueba'
export CORS_ORIGINS=http://localhost:4200
export SPRING_PROFILES_ACTIVE=prod
# Incluso si se solicita demo por error, @Profile("!prod") debe impedir su carga.
export APP_DEMO_ENABLED=true
set +a
exec java -jar target/academia-api-0.0.1-SNAPSHOT.jar \
  --server.address=127.0.0.1 --server.port=8084 \
  --spring.flyway.default-schema=laboratorio4_prod_prueba \
  --spring.flyway.schemas=laboratorio4_prod_prueba \
  --spring.jpa.properties.hibernate.default_schema=laboratorio4_prod_prueba
