# Поставување на FOCUS Lab во Kubernetes

Чекор по чекор, од празна машина до апликација што работи во кластер.
Командите се за macOS; на Linux е исто, освен `sed -i ''` што станува `sed -i`.

## Што има тука

| Фајл | Што прави |
|---|---|
| `00-namespace.yaml` | namespace `focus-lab` |
| `01-configmap.yaml` | конфигурација што не е тајна |
| `02-secret.example.yaml` | образец за тајните (вистинскиот фајл не се комитира) |
| `03-postgres.yaml` | StatefulSet + headless Service за базата, со свој диск |
| `04-backend.yaml` | Deployment + Service за REST API-то |
| `05-frontend.yaml` | Deployment + Service за nginx со изградениот интерфејс |
| `06-ingress.yaml` | едно влезно место: `/api` → бекенд, `/` → фронтенд |
| `apply.sh` | ги поставува по ред и чека да се подигнат |

---

## 0. Предуслови

- **Docker Desktop** — веќе го имаш
- **kubectl** — доаѓа со Docker Desktop; провери со `kubectl version --client`
- **Docker Hub сметка** — hub.docker.com

## 1. Кластер

Docker Desktop → **Settings** → **Kubernetes** → штиклирај **Enable Kubernetes** → **Apply & Restart**.
Трае неколку минути први пат.

```bash
kubectl config use-context docker-desktop
kubectl get nodes
```

Треба да видиш еден node во состојба `Ready`.

## 2. Ingress контролер

Kubernetes сам по себе не знае што е Ingress — треба контролер што ќе го чита:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/cloud/deploy.yaml

kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s
```

## 3. Домаќинот во /etc/hosts

`focuslab.local` не постои во DNS, па се додава рачно:

```bash
echo "127.0.0.1  focuslab.local" | sudo tee -a /etc/hosts
```

## 4. Имиџи

На Apple Silicon имиџите се градат локално — оние од CI се `linux/amd64` и
под емулација би биле премногу бавни. Docker Desktop го дели складот на
имиџи со Kubernetes, па `imagePullPolicy: IfNotPresent` ги наоѓа веднаш.

Од коренот на проектот (замени го `georgina1`):

```bash
docker build -t georgina1/focus-lab-backend:latest ./backend
docker build -t georgina1/focus-lab-frontend:latest ./frontend
```

## 5. Име во манифестите

```bash
sed -i '' 's/DOCKERHUB_USERNAME/georgina1/g' k8s/04-backend.yaml k8s/05-frontend.yaml
```

## 6. Тајните

```bash
cp k8s/02-secret.example.yaml k8s/02-secret.yaml
```

Отвори го `k8s/02-secret.yaml` и смени ги четирите вредности. Две правила:

- `JWT_SECRET` — најмалку 32 знаци, случајна низа. Направи ја со
  `openssl rand -base64 48`
- `ADMIN_PASSWORD` — не смее да биде `ChangeMe123!`

`SecretsGuard` во апликацијата одбива да стартува ако ова не е испочитувано.
Фајлот е во `.gitignore` и не оди на GitHub.

## 7. Поставување

```bash
./k8s/apply.sh
```

Скриптата проверува дали тајните и имињата на имиџите се наместени, па ги
поставува манифестите по ред и чека да се подигнат. Првиот старт на бекендот
трае подолго — Hibernate ја создава шемата во празната база.

## 8. Провери дека работи

```bash
kubectl -n focus-lab get all
kubectl -n focus-lab get pvc,ingress,configmap,secret
kubectl -n focus-lab logs deployment/backend --tail=30
```

Отвори **http://focuslab.local** и најави се со `ADMIN_EMAIL` од ConfigMap-от
и `ADMIN_PASSWORD` од Secret-от.

Дека Secret-от навистина се чита се докажува со тоа што апликацијата воопшто
стартувала: со тајна од репото `SecretsGuard` ја запира.

## 9. За елаборатот

Слики што вреди да се земат:

```bash
kubectl -n focus-lab get pods -o wide          # подовите со nodes
kubectl -n focus-lab get svc                   # трите сервиси
kubectl -n focus-lab get ingress               # правилата за рутирање
kubectl -n focus-lab get pvc                   # дискот на базата
kubectl -n focus-lab describe statefulset postgres
kubectl get namespaces                         # focus-lab покрај останатите
```

Плус слика од самата апликација на `http://focuslab.local` и од
Actions страницата на GitHub.

## Бришење

```bash
kubectl delete namespace focus-lab
```

Со тоа се бришат сите објекти. Дискот на базата (PVC) не се брише со
namespace-от во секој кластер — провери со `kubectl get pv`.

---

## Ако нешто не тргне

**Подот е `ImagePullBackOff`** — името на имиџот не се совпаѓа. Провери со
`kubectl -n focus-lab describe pod <име>` и дали `docker images | grep focus-lab`
го покажува локално изградениот имиџ.

**`exec format error`** — имиџ од друга архитектура. Изгради локално (чекор 4).

**Фронтендот паѓа со `host not found in upstream`** — Service-от за бекендот
мора да се вика точно `backend`, бидејќи `nginx.conf` го разрешува тоа име при
стартување. Ако е тоа во ред, подот паднал само затоа што стартувал пред
Service-от: `kubectl -n focus-lab rollout restart deployment/frontend`.

**Бекендот е во `CrashLoopBackOff`** — прочитај го логот:
`kubectl -n focus-lab logs deployment/backend --previous`. Најчесто е
`SecretsGuard` (слаба тајна) или базата уште не е готова.

**Postgres не стартува, се буни за `lost+found`** — `PGDATA` мора да биде
поддиректориум, што е веќе поставено во `03-postgres.yaml`.

**Отворањето на `focuslab.local` дава 404** — Ingress контролерот не е готов
или редот во `/etc/hosts` го нема. Провери
`kubectl -n ingress-nginx get svc ingress-nginx-controller`; треба
`EXTERNAL-IP` да е `localhost`.

**Качување слика дава 413** — прилозите се до 22 MB, а тоа е дозволено преку
`proxy-body-size` во Ingress-от. Ако е променет, врати го.
