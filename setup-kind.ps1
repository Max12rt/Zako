# KIND-based setup for ZAKO observability stack
# Run from C:\Users\crulp\zako (as Administrator):
#   .\setup-kind.ps1

$ErrorActionPreference = 'Continue'
$env:PATH += ";$env:USERPROFILE\bin"

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Ok($msg)   { Write-Host "    $msg" -ForegroundColor Green }

# 1. Create KIND cluster
Step "Creating KIND cluster 'zako'"
$existing = kind get clusters 2>&1 | Select-String "zako"
if ($existing) {
    Ok "Cluster 'zako' already exists, skipping"
} else {
    kind create cluster --name zako --config kind-cluster.yaml
}

# 2. Install ingress-nginx for KIND
Step "Installing ingress-nginx"
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/kind/deploy.yaml
Write-Host "  Waiting for ingress-nginx to be ready..." -ForegroundColor DarkGray
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=120s
Start-Sleep -Seconds 15  # wait for admission webhook to register

# 3. Install metrics-server
Step "Installing metrics-server"
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl patch deployment metrics-server -n kube-system --type=json -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

# 4. Build images and load into KIND
Step "Building monolith image"
docker build -t zako/monolith:latest ./monolith
kind load docker-image zako/monolith:latest --name zako

Step "Building frontend image"
docker build -t zako/frontend:latest ./frontend
kind load docker-image zako/frontend:latest --name zako

# 5. Apply manifests
Step "Applying K8s manifests"
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/10-postgres-secret.yaml
kubectl apply -f k8s/15-app-secret.yaml
kubectl apply -f k8s/11-postgres-statefulset.yaml
kubectl apply -f k8s/20-monolith-config.yaml
kubectl apply -f k8s/21-monolith.yaml
kubectl apply -f k8s/30-frontend.yaml
kubectl apply -f k8s/50-prometheus.yaml
kubectl apply -f k8s/51-grafana.yaml
kubectl apply -f k8s/52-jaeger.yaml
kubectl apply -f k8s/60-hpa.yaml
kubectl apply -f k8s/40-ingress.yaml

# 6. Wait for monolith
Step "Waiting for monolith to be ready (up to 3 min)"
kubectl rollout status deployment/monolith -n zako --timeout=180s

# 7. Add hosts entry
Step "Checking hosts file for zako.localhost"
$hostsFile = "$env:SystemRoot\System32\drivers\etc\hosts"
if (-not (Select-String -Path $hostsFile -Pattern "zako.localhost" -Quiet)) {
    Add-Content -Path $hostsFile -Value "`n127.0.0.1 zako.localhost" -Encoding ASCII
    Ok "Added 127.0.0.1 zako.localhost to hosts"
} else {
    Ok "zako.localhost already in hosts"
}

Step "Done! Access points (port 8080 because port 80 needs admin):"
Write-Host "  App:      http://zako.localhost:8080"            -ForegroundColor Green
Write-Host "  API:      http://zako.localhost:8080/api/trips"  -ForegroundColor Green
Write-Host "  Grafana:  http://zako.localhost:8080/grafana"    -ForegroundColor Green
Write-Host "  Jaeger:   http://zako.localhost:8080/jaeger"     -ForegroundColor Green
Write-Host ""
Write-Host "  k6 smoke: k6 run --env BASE_URL=http://zako.localhost:8080 k6/smoke-test.js" -ForegroundColor DarkGray
Write-Host "  HPA:      kubectl get hpa -n zako -w" -ForegroundColor DarkGray
