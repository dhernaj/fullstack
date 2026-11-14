#!/bin/zsh
set -eu
cd "$(dirname "$0")"
source ./.env.local
export APP_DEMO_ENABLED="${APP_DEMO_ENABLED:-true}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-h2}"
if [[ "$SPRING_PROFILES_ACTIVE" == "postgres" ]]; then
  PG_BIN=/usr/local/opt/postgresql@17/bin
  PG_DATA=/usr/local/var/postgresql@17
  if ! "$PG_BIN/pg_ctl" -D "$PG_DATA" status >/dev/null 2>&1; then
    LC_ALL=en_US.UTF-8 "$PG_BIN/pg_ctl" -D "$PG_DATA" -l "$PG_DATA/server.log" -o '-h 127.0.0.1' start
  fi
fi
exec ./mvnw spring-boot:run
