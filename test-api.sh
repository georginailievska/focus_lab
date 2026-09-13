#!/usr/bin/env bash
# Рачен интеграциски тест на backend-от; бара `mvn spring-boot:run` на :8080.
set -e

BASE="http://localhost:8080/api"
TS=$(date +%s)   # за да скриптата може да се пушта повторно без "email веќе постои"

extract() {          # extract "<json>" fieldName -> string вредност
  echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | cut -d'"' -f4
}
extract_num() {       # extract_num "<json>" fieldName -> број
  echo "$1" | grep -o "\"$2\":[0-9]*" | head -1 | grep -o '[0-9]*$'
}

echo "== 1) Регистрација студент =="
STUDENT_REG=$(curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"fullName\":\"Test Student\",\"email\":\"teststudent$TS@students.finki.ukim.mk\",\"password\":\"password123\",\"role\":\"STUDENT\"}")
echo "$STUDENT_REG"
STUDENT_TOKEN=$(extract "$STUDENT_REG" token)

echo; echo "== 2) Регистрација ментор (PENDING) =="
MENTOR_REG=$(curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"fullName\":\"Test Mentor\",\"email\":\"testmentor$TS@finki.ukim.mk\",\"password\":\"password123\",\"role\":\"MENTOR\"}")
echo "$MENTOR_REG"
MENTOR_ID=$(extract_num "$MENTOR_REG" id)

echo; echo "== 3) Login admin =="
ADMIN_LOGIN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@focuslab.mk","password":"ChangeMe123!"}')
echo "$ADMIN_LOGIN"
ADMIN_TOKEN=$(extract "$ADMIN_LOGIN" token)

echo; echo "== 4) Admin го одобрува менторот (id=$MENTOR_ID) =="
curl -s -X PATCH "$BASE/admin/mentors/$MENTOR_ID/approve" -H "Authorization: Bearer $ADMIN_TOKEN"
echo

echo; echo "== 5) Login ментор (сега APPROVED) =="
MENTOR_LOGIN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"testmentor$TS@finki.ukim.mk\",\"password\":\"password123\"}")
echo "$MENTOR_LOGIN"
MENTOR_TOKEN=$(extract "$MENTOR_LOGIN" token)

echo; echo "== 6) Листа предмети =="
SUBJECTS=$(curl -s "$BASE/subjects" -H "Authorization: Bearer $MENTOR_TOKEN")
echo "$SUBJECTS"
SUBJECT_ID=$(extract_num "$SUBJECTS" id)

echo; echo "== 7) Ментор креира сесија (subjectId=$SUBJECT_ID) =="
SESSION=$(curl -s -X POST "$BASE/sessions" \
  -H "Authorization: Bearer $MENTOR_TOKEN" -H "Content-Type: application/json" \
  -d "{\"title\":\"Test Session\",\"description\":\"API test\",\"subjectId\":$SUBJECT_ID,\"mentorIds\":[$MENTOR_ID],\"mode\":\"ONLINE\",\"location\":\"https://teams.microsoft.com/test\",\"startTime\":\"2026-12-01T14:00:00\",\"endTime\":\"2026-12-01T15:30:00\",\"tags\":[\"test\"]}")
echo "$SESSION"
SESSION_ID=$(extract_num "$SESSION" id)

echo; echo "== 8) Студент се пријавува на сесија (id=$SESSION_ID) =="
APPLY=$(curl -s -X POST "$BASE/sessions/$SESSION_ID/apply" -H "Authorization: Bearer $STUDENT_TOKEN")
echo "$APPLY"
APPLICATION_ID=$(extract_num "$APPLY" id)

echo; echo "== 9) Ментор ги гледа pending барањата =="
curl -s "$BASE/mentor/requests" -H "Authorization: Bearer $MENTOR_TOKEN"
echo

echo; echo "== 10) Ментор ја прифаќа пријавата (id=$APPLICATION_ID) =="
curl -s -X PATCH "$BASE/sessions/applications/$APPLICATION_ID" \
  -H "Authorization: Bearer $MENTOR_TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"ACCEPTED"}'
echo

echo; echo "== 11) Финална состојба (треба applicantsCount=1, approvedCount=1) =="
curl -s "$BASE/sessions/$SESSION_ID" -H "Authorization: Bearer $STUDENT_TOKEN"
echo
