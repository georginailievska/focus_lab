#!/usr/bin/env bash
# Ги поставува манифестите по ред: namespace, потоа конфигурација, потоа
# базата, па апликацијата. Редот е важен — подовите не смеат да се создадат
# пред ConfigMap-от и Secret-от што ги читаат.
#
# Не се користи `kubectl apply -f k8s/`: тоа би го зафатило и
# 02-secret.example.yaml со лажните вредности.

set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f 02-secret.yaml ]; then
  echo "Нема 02-secret.yaml."
  echo "Направи го така:  cp 02-secret.example.yaml 02-secret.yaml"
  echo "па смени ги четирите вредности во него."
  exit 1
fi

if grep -q "СМЕНИ-МЕ" 02-secret.yaml; then
  echo "02-secret.yaml сè уште ги има лажните вредности — смени ги прво."
  exit 1
fi

if grep -rq "DOCKERHUB_USERNAME" 04-backend.yaml 05-frontend.yaml; then
  echo "Во манифестите сè уште стои DOCKERHUB_USERNAME."
  echo "Смени го со своето име:"
  echo "  sed -i '' 's/DOCKERHUB_USERNAME/tvoe-ime/g' 04-backend.yaml 05-frontend.yaml"
  exit 1
fi

for file in 00-namespace.yaml 01-configmap.yaml 02-secret.yaml \
            03-postgres.yaml 04-backend.yaml 05-frontend.yaml 06-ingress.yaml; do
  echo "→ $file"
  kubectl apply -f "$file"
done

echo
echo "Чекам базата..."
kubectl -n focus-lab rollout status statefulset/postgres --timeout=180s

echo "Чекам бекендот (првиот старт создава шема, може да потрае)..."
kubectl -n focus-lab rollout status deployment/backend --timeout=300s

echo "Чекам фронтендот..."
kubectl -n focus-lab rollout status deployment/frontend --timeout=120s

echo
echo "Готово. Отвори http://focuslab.local"
