#!/usr/bin/env bash
# Ја напаѓа сопствената апликација и проверува дали се брани.
# Бара бекенд на :8080 (пушти го со ./run-backend.sh).
#
# Употреба:  ./security-probe.sh
#            BASE=http://localhost:8080/api ./security-probe.sh
#
# Ако скриптата се удри во ограничувањето на барањата, рестартирај го бекендот:
# броењето е во меморија и се брише при старт.

set -uo pipefail

BASE="${BASE:-http://localhost:8080/api}"
TS=$(date +%s)
PASS=0
FAIL=0

green() { printf '\033[32m  ✓ %s\033[0m\n' "$1"; PASS=$((PASS + 1)); }
red()   { printf '\033[31m  ✗ %s\033[0m\n' "$1"; FAIL=$((FAIL + 1)); }
note()  { printf '    %s\n' "$1"; }
head2() { printf '\n\033[1m%s\033[0m\n' "$1"; }

# code METHOD PATH [TOKEN] [BODY]
code() {
  local method="$1" path="$2" token="${3:-}" body="${4:-}"
  local args=(-s -o /dev/null -w '%{http_code}' -X "$method" "$BASE$path")

  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")

  curl "${args[@]}"
}

json() {
  local method="$1" path="$2" token="${3:-}" body="${4:-}"
  local args=(-s -X "$method" "$BASE$path")

  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")

  curl "${args[@]}"
}

register() {
  local email="$1" role="$2" password="${3:-lozinka123}" name="${4:-Проба}"
  json POST /auth/register '' \
    "{\"fullName\":\"$name\",\"email\":\"$email\",\"password\":\"$password\",\"role\":\"$role\"}"
}

register_code() {
  local email="$1" role="$2" password="${3:-lozinka123}"
  code POST /auth/register '' \
    "{\"fullName\":\"Проба\",\"email\":\"$email\",\"password\":\"$password\",\"role\":\"$role\"}"
}

field() { echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
number() { echo "$1" | grep -o "\"$2\":[0-9]*" | head -1 | grep -o '[0-9]*$'; }

expect() {
  local want="$1" got="$2" what="$3"
  if [ "$got" = "$want" ]; then green "$what ($got)"; else red "$what — очекував $want, добив $got"; fi
}

expect_one_of() {
  local want="$1" got="$2" what="$3"
  if [[ " $want " == *" $got "* ]]; then green "$what ($got)"; else red "$what — очекував едно од [$want], добив $got"; fi
}

head2 "0. Сервер"
if [ "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/health")" != "200" ]; then
  red "Серверот не одговара на $BASE/health — пушти го бекендот прво"
  exit 1
fi
green "серверот одговара"

# ---------------------------------------------------------------- пробни сметки
# Сметките се создаваат прво: сè останато зависи од нив, а тестот за
# ограничување на барањата оди последен, за да не си го потроши буџетот сам.
head2 "1. Пробни сметки"
STUDENT_EMAIL="probe-student-$TS@students.finki.ukim.mk"
MENTOR_EMAIL="probe-mentor-$TS@finki.ukim.mk"
TOKEN_EMAIL="probe-token-$TS@students.finki.ukim.mk"

STUDENT=$(register "$STUDENT_EMAIL" STUDENT)
STUDENT_TOKEN=$(field "$STUDENT" token)
STUDENT_ID=$(number "$STUDENT" id)

MENTOR=$(register "$MENTOR_EMAIL" MENTOR)
MENTOR_TOKEN=$(field "$MENTOR" token)

if [ -z "$STUDENT_TOKEN" ] || [ -z "$MENTOR_TOKEN" ]; then
  red "не можам да создадам пробни сметки"
  note "Одговор: $(echo "$STUDENT" | head -c 160)"
  note "Ако пишува 429, рестартирај го бекендот — броењето е во меморија."
  echo
  printf 'Поминати: %s, паднати: %s\n' "$PASS" "$FAIL"
  exit 1
fi
green "пробен студент и ментор создадени"

head2 "2. Пристап без токен"
for path in /sessions /subjects /interests /me /users/1 /admin/stats /mentor/sessions; do
  expect 401 "$(code GET "$path")" "GET $path без токен"
done

head2 "3. Измислен и искривен токен"
expect 401 "$(code GET /me 'ова.не.е.токен')" "смет наместо токен"
FORGED='eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBmb2N1c2xhYi5tayIsInJvbGUiOiJBRE1JTiJ9.podpis'
expect 401 "$(code GET /admin/stats "$FORGED")" "токен потпишан со друга тајна"
NONE='eyJhbGciOiJub25lIn0.eyJzdWIiOiJhZG1pbkBmb2N1c2xhYi5tayJ9.'
expect 401 "$(code GET /admin/stats "$NONE")" "токен со alg=none"
expect 401 "$(code GET /me "${STUDENT_TOKEN}x")" "токен со променета последна буква"

head2 "4. Правила при регистрација"
expect 400 "$(register_code "probe-$TS@gmail.com" STUDENT)" "email надвор од дозволените домени"
expect 400 "$(register_code "probe-short-$TS@students.finki.ukim.mk" STUDENT 123)" "лозинка под 8 знаци"
expect 409 "$(register_code "$STUDENT_EMAIL" STUDENT)" "иста email адреса двапати"

ADMIN_TRY=$(register "probe-admin-$TS@students.finki.ukim.mk" ADMIN)
if echo "$ADMIN_TRY" | grep -q '"role":"ADMIN"'; then
  red "регистрација со улога ADMIN помина"
else
  green "улогата ADMIN не се добива преку регистрација"
fi

head2 "5. Студент кон менторски и админ рути"
for path in /mentor/sessions /mentor/requests /mentor/colleagues /mentor/notes; do
  expect 403 "$(code GET "$path" "$STUDENT_TOKEN")" "студент → GET $path"
done
expect 403 "$(code GET /admin/stats "$STUDENT_TOKEN")" "студент → GET /admin/stats"
expect 403 "$(code POST /sessions "$STUDENT_TOKEN" '{"title":"x"}')" "студент → POST /sessions"

head2 "6. Неодобрен ментор"
expect 403 "$(code GET /mentor/colleagues "$MENTOR_TOKEN")" "неодобрен ментор → списокот ментори"
NEW_SESSION='{"title":"Проба","subjectId":1,"mentorIds":[],"mode":"ONLINE","location":"x","startTime":"2026-12-01T10:00:00","endTime":"2026-12-01T12:00:00","tags":[]}'
expect_one_of "403 400" "$(code POST /sessions "$MENTOR_TOKEN" "$NEW_SESSION")" "неодобрен ментор → нова сесија"

head2 "7. Туѓи податоци по id"
expect 403 "$(code GET "/users/$((STUDENT_ID + 1))" "$STUDENT_TOKEN")" "студент → профил на друг студент"

ME=$(json GET /me "$STUDENT_TOKEN")
if echo "$ME" | grep -q "$STUDENT_EMAIL"; then
  green "своjот профил си го гледа со email"
else
  red "своjот профил не враќа email"
fi

OTHER=$(json GET /users/1 "$STUDENT_TOKEN")
if echo "$OTHER" | grep -qE '"email":"[^"]+"'; then
  red "туѓ профил враќа email"
else
  green "туѓ профил е без email"
fi

if echo "$OTHER" | grep -qE '"interests":\[[^]]'; then
  red "туѓ профил враќа интереси"
else
  green "туѓ профил е без интереси"
fi

head2 "8. Линкот за средба"
SESSIONS=$(json GET /sessions "$STUDENT_TOKEN")
if echo "$SESSIONS" | grep -qE '"location":"[^"]+"'; then
  red "листата сесии враќа location на студент што не е прифатен"
  note "$(echo "$SESSIONS" | grep -oE '"location":"[^"]+"' | head -2)"
else
  green "location не се враќа на неприфатен студент"
fi

head2 "9. Заглавја и течење на податоци"
HEADERS=$(curl -s -D - -o /dev/null "$BASE/health")
for header in "X-Content-Type-Options" "Content-Security-Policy" "Referrer-Policy" "Permissions-Policy"; do
  if echo "$HEADERS" | grep -qi "$header"; then green "заглавје $header"; else red "нема заглавје $header"; fi
done

if echo "$HEADERS" | grep -qiE "^server:.*(tomcat|[0-9]+\.[0-9]+)|X-Powered-By"; then
  red "заглавјата ја кажуваат технологијата или верзијата"
else
  green "нема заглавје што ја кажува верзијата"
fi

BOOM=$(json GET "/sessions/9999999" "$STUDENT_TOKEN")
if echo "$BOOM" | grep -qiE "exception|\.java|org\.spring|at mk\.focuslab"; then
  red "грешката враќа внатрешни детали"
  note "$(echo "$BOOM" | head -c 160)"
else
  green "грешката не враќа внатрешни детали"
fi

head2 "10. Токен по промена на лозинка"
NEW=$(register "$TOKEN_EMAIL" STUDENT)
NEW_TOKEN=$(field "$NEW" token)

if [ -n "$NEW_TOKEN" ]; then
  expect 200 "$(code GET /me "$NEW_TOKEN")" "нов токен важи"
  CHANGE='{"currentPassword":"lozinka123","newPassword":"novaLozinka123"}'
  expect 200 "$(code POST /me/password "$NEW_TOKEN" "$CHANGE")" "лозинката е сменета"
  expect 401 "$(code GET /me "$NEW_TOKEN")" "стариот токен по промена на лозинка"
else
  red "не можам да создадам сметка за овој тест"
  note "Одговор: $(echo "$NEW" | head -c 160)"
fi

# Оди последен: троши го буџетот на најави за оваа адреса
head2 "11. Ограничување на најави"
BLOCKED_AT=0
for attempt in $(seq 1 30); do
  STATUS=$(code POST /auth/login '' "{\"email\":\"$STUDENT_EMAIL\",\"password\":\"pogresna$attempt\"}")

  if [ "$STATUS" = "429" ]; then
    BLOCKED_AT=$attempt
    break
  fi

  if [ "$STATUS" != "401" ]; then
    red "неочекуван одговор на погрешна најава: $STATUS"
    break
  fi
done

if [ "$BLOCKED_AT" -gt 0 ]; then
  green "пробивањето е блокирано по $BLOCKED_AT обиди (429)"
else
  red "триесет погрешни најави поминаа без блокирање"
fi

head2 "Резултат"
printf 'Поминати: \033[32m%s\033[0m, паднати: \033[31m%s\033[0m\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ] || exit 1
