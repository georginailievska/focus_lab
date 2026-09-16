#!/usr/bin/env bash
# Стартува бекендот со вредностите од `.env`.
#
# `mvn spring-boot:run` сам по себе НЕ го чита `.env` — тој фајл го користи само
# docker compose. Без вчитување, Spring ги зема default-ите од `application.yml`,
# а тие се placeholder-и што стојат во репото и `SecretsGuard` ги одбива.
#
# Употреба:  ./run-backend.sh
#            ./run-backend.sh mailpit     # SMTP кон локален фаќач на email

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$ROOT/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "Нема $ENV_FILE. Копирај го .env.example и пополни ги вредностите." >&2
  exit 1
fi

# set -a: сè што се дефинира до `set +a` станува променлива на околината,
# која Spring потоа ја чита како ${JWT_SECRET} и слично.
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

if [ "${1:-}" = "mailpit" ]; then
  export MAIL_HOST=localhost MAIL_PORT=1025 MAIL_AUTH=false MAIL_STARTTLS=false
  echo "SMTP кон Mailpit на localhost:1025 (веб: http://localhost:8025)"
fi

cd "$ROOT/backend"
exec mvn spring-boot:run
