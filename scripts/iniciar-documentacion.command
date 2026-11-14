#!/bin/zsh
# Vista local de Swagger con datos y credenciales ficticios; no usa PostgreSQL.
set -eu
cd "$(dirname "$0")/../academia-api"
export LAB_ADMIN_PASSWORD='solo-demo-swagger-admin-2026'
export LAB_DOCENTE_PASSWORD='solo-demo-swagger-docente-2026'
export LAB_ESTUDIANTE_PASSWORD='solo-demo-swagger-estudiante-2026'
exec ./mvnw -B spring-boot:run \
  -Dspring-boot.run.useTestClasspath=true \
  '-Dspring-boot.run.arguments=--server.port=8083 --server.address=127.0.0.1 --spring.application.name=academia-api-doc-preview --spring.profiles.active=doc-preview --spring.datasource.url=jdbc:h2:mem:documentacion --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --app.demo.enabled=true --app.jwt.secret=Y2xhdmUtcHVibGljYS1leGNsdXNpdmEtcGFyYS1wcnVlYmFzLW5vLXVzYXItZW4tcHJvZHVjY2lvbi0yMDI2 --app.jwt.expiracion-minutos=15'
