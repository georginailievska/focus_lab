#!/usr/bin/env bash
# Ја напаѓа сопствената апликација и проверува дали се брани.
# Бара `mvn spring-boot:run` на :8080 и празна база не е потребна.
#
# Употреба:  ./security-probe.sh
#            BASE=http://localhost:8080/api ./security-probe.sh

set -uo pipefail

BASE="${BASE:-http://localhost:8080/api}"
TS=$(date +%s)
PASS=0
FAIL=0

green() { printf '\033[32m  ✓ %s\033[0m\n' "$1"; PASS=$((PASS + 1)); }
red()   { printf '\033[31m  ✗ %s\033[0m\n' "$1"; FAIL=$((FAIL + 1)); }
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

head2 "1. Пристап без токен"
for path in /sessions /subjects /interests /me /users/1; do
  expect 401 "$(code GET "$path")" "GET $path без токен"
done
expect 401 "$(code GET /admin/stats)" "GET /admin/stats без токен"
expect 401 "$(code GET /mentor/sessions)" "GET /mentor/sessions без токен"

head2 "2. Измислен и искривен токен"
expect 401 "$(code GET /me "ова.не.е.токен")" "смет наместо токен"
FORGED='eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBmb2N1c2xhYi5tayIsInJvbGUiOiJBRE1JTiJ9.podpis'
expect 401 "$(code GET /admin/stats "$FORGED")" "токен потпишан со друга тајна"
expect 401 "$(code GET /admin/stats 'eyJhbGciOiJub25lIn0.eyJzdWIiOiJhZG1pbkBmb2N1c2xhYi5tayJ9.')" "токен со alg=none"

head2 "3. Регистрација"
STUDENT_EMAIL="probe-student-$TS@students.finki.ukim.mk"
MENTOR_EMAIL="probe-mentor-$TS@finki.ukim.mk"

expect 400 "$(code POST /auth/register '' \
  "{\"fullName\":\"Надвор\",\"email\":\"probe-$TS@gmail.com\",\"password\":\"lozinka123\",\"role\":\"STUDENT\"}")" \
  "email надвор од дозволените домени"

ADMIN_TRY=$(json POST /auth/register '' \
  "{\"fullName\":\"Лажен Админ\",\"email\":\"probe-admin-$TS@students.finki.ukim.mk\",\"password\":\"lozinka123\",\"role\":\"ADMIN\"}")
if echo "$ADMIN_TRY" | grep -q '"role":"ADMIN"'; then
  red "регистрација со улога ADMIN помина"
else
  green "улогата ADMIN не се добива преку регистрација"
fi

expect 400 "$(code POST /auth/register '' \
  "{\"fullName\":\"Кратка\",\"email\":\"probe-short-$TS@students.finki.ukim.mk\",\"password\":\"123\",\"role\":\"STUDENT\"}")" \
  "лозинка под 8 знаци"

STUDENT=$(json POST /auth/register '' \
  "{\"fullName\":\"Проба Студент\",\"email\":\"$STUDENT_EMAIL\",\"password\":\"lozinka123\",\"role\":\"STUDENT\"}")
STUDENT_TOKEN=$(field "$STUDENT" token)
STUDENT_ID=$(number "$STUDENT" id)

MENTOR=$(json POST /auth/register '' \
  "{\"fullName\":\"Проба Ментор\",\"email\":\"$MENTOR_EMAIL\",\"password\":\"lozinka123\",\"role\":\"MENTOR\"}")
MENTOR_TOKEN=$(field "$MENTOR" token)

if [ -z "$STUDENT_TOKEN" ] || [ -z "$MENTOR_TOKEN" ]; then
  red "не можам да регистрирам пробни сметки (можеби лимитот е веќе искористен) — прескокнувам понатаму"
  echo; echo "Поминати: $PASS, паднати: $FAIL"
  exit 1
fi
green "пробни сметки создадени"

head2 "4. Студент кон менторски и админ рути"
for path in /mentor/sessions /mentor/requests /mentor/colleagues /mentor/notes; do
  expect 403 "$(code GET "$path" "$STUDENT_TOKEN")" "студент → GET $path"
done
expect 403 "$(code GET /admin/stats "$STUDENT_TOKEN")" "студент → GET /admin/stats"
expect 403 "$(code GET /admin/users "$STUDENT_TOKEN")" "студент → GET /admin/users (и не постои)"
expect 403 "$(code POST /sessions "$STUDENT_TOKEN" '{"title":"x"}')" "студент → POST /sessions"

head2 "5. Неодобрен ментор"
expect 403 "$(code GET /mentor/colleagues "$MENTOR_TOKEN")" "неодобрен ментор → списокот ментори"
expect_one_of "403 400" "$(code POST /sessions "$MENTOR_TOKEN" \
  '{"title":"Проба","subjectId":1,"mentorIds":[],"mode":"ONLINE","location":"x","startTime":"2026-12-01T10:00:00","endTime":"2026-12-01T12:00:00","tags":[]}')" \
  "неодобрен ментор → закажување сесија"

head2 "6. Туѓи податоци по id (IDOR)"
expect 403 "$(code GET "/users/$((STUDENT_ID + 1))" "$STUDENT_TOKEN")" "студент → профил на друг студент"
expect_one_of "200 403 404" "$(code GET /users/1 "$STUDENT_TOKEN")" "студент → профил со id 1"

ME=$(json GET /me "$STUDENT_TOKEN")
if echo "$ME" | grep -q "$STUDENT_EMAIL"; then
  green "своjот профил си го гледа со email"
else
  red "своjот профил не враќа email"
fi

OTHER=$(json GET /users/1 "$STUDENT_TOKEN")
if echo "$OTHER" | grep -q '"email":"[^n]'; then
  red "туѓ профил враќа email"
else
  green "туѓ профил е без email"
fi

head2 "7. Линкот за средба"
SESSIONS=$(json GET /sessions "$STUDENT_TOKEN")
if echo "$SESSIONS" | grep -o '"location":"[^"]*"' | grep -q .; then
  red "листата сесии враќа location на студент што не е прифатен"
else
  green "location не се враќа на неприфатен студент"
fi

head2 "8. Ограничување на најави"
LAST=""
for attempt in $(seq 1 12); do
  LAST=$(code POST /auth/login '' "{\"email\":\"$STUDENT_EMAIL\",\"password\":\"pogresna$attempt\"}")
done
expect 429 "$LAST" "12 погрешни најави → блокирање"

head2 "9. Промена на лозинка го поништува стариот токен"
NEW=$(json POST /auth/register '' \
  "{\"fullName\":\"Проба Токен\",\"email\":\"probe-token-$TS@students.finki.ukim.mk\",\"password\":\"lozinka123\",\"role\":\"STUDENT\"}")
NEW_TOKEN=$(field "$NEW" token)

if [ -n "$NEW_TOKEN" ]; then
  expect 200 "$(code GET /me "$NEW_TOKEN")" "нов токен важи"
  CHANGED=$(code POST /me/password "$NEW_TOKEN" \
    '{"currentPassword":"lozinka123","newPassword":"novaLozinka123"}')
  expect 200 "$CHANGED" "лозинката е сменета"
  expect 401 "$(code GET /me "$NEW_TOKEN")" "стариот токен по промена на лозинка"
else
  red "не можам да создадам сметка за овој тест (лимит на регистрации)"
fi

head2 "10. Заглавја и течење на податоци"
HEADERS=$(curl -s -D - -o /dev/null "$BASE/health")
for header in "X-Content-Type-Options" "Content-Security-Policy" "Referrer-Policy"; do
  if echo "$HEADERS" | grep -qi "$header"; then green "заглавје $header"; else red "нема заглавје $header"; fi
done
if echo "$HEADERS" | grep -qi "^server:.*tomcat\|X-Powered-By"; then
  red "заглавјата ја кажуваат технологијата и верзијата"
else
  green "нема заглавје што ја кажува верзијата"
fi

BOOM=$(json GET "/sessions/nema-takva" "$STUDENT_TOKEN")
if echo "$BOOM" | grep -qi "exception\|\.java\|org\.spring"; then
  red "грешката враќа внатрешни детали: $(echo "$BOOM" | head -c 120)"
else
  green "грешката не враќа внатрешни детали"
fi

head2 "Резултат"
printf 'Поминати: \033[32m%s\033[0m, паднати: \033[31m%s\033[0m\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ] || exit 1
