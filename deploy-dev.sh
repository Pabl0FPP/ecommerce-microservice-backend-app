#!/usr/bin/env bash
# deploy-dev.sh
# Script para desplegar todos los servicios en Kubernetes (kind/minikube) con perfil DEV

# set -e  # Desactivado para que no falle el script con timeouts

echo "🚀 Desplegando servicios en Kubernetes con perfil DEV..."
echo ""

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 1. Verificar que el cluster de Kubernetes esté disponible
echo -e "${YELLOW}1. Verificando cluster de Kubernetes...${NC}"
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}❌ No hay cluster de Kubernetes disponible.${NC}"
    echo -e "${BLUE}Para kind: kind create cluster --name ecommerce${NC}"
    echo -e "${BLUE}Para minikube: minikube start${NC}"
    exit 1
fi

# Detectar si es kind o minikube
CLUSTER_TYPE=""
if kubectl config current-context | grep -q "kind"; then
    CLUSTER_TYPE="kind"
    echo -e "${GREEN}✅ Cluster de kind detectado${NC}"
elif kubectl config current-context | grep -q "minikube"; then
    CLUSTER_TYPE="minikube"
    echo -e "${GREEN}✅ Cluster de minikube detectado${NC}"
else
    CLUSTER_TYPE="kubernetes"
    echo -e "${GREEN}✅ Cluster de Kubernetes detectado${NC}"
fi

echo -e "${BLUE}Contexto: $(kubectl config current-context)${NC}"
echo ""

# 2. Desplegar infraestructura primero (Eureka debe estar primero)
echo -e "${YELLOW}2. Desplegando infraestructura...${NC}"
kubectl apply -f k8s/eureka-deployment.yml
kubectl apply -f k8s/cloud-config-deployment.yml
kubectl apply -f k8s/zipkin-deployment.yml
echo -e "${GREEN}✅ Infraestructura desplegada${NC}"
echo ""

# 3. Esperar a que Eureka esté listo (crítico)
echo -e "${YELLOW}3. Esperando a que Eureka esté listo...${NC}"
echo "Esto puede tardar varios minutos mientras se descargan las imágenes..."
kubectl rollout status deployment/service-discovery --timeout=1800s || echo "⚠️  Eureka aún está iniciando. Puedes verificar con: kubectl get pods"
echo -e "${GREEN}✅ Comando de espera completado${NC}"
echo ""

# 4. Esperar a que Cloud Config esté listo
echo -e "${YELLOW}4. Esperando a que Cloud Config esté listo...${NC}"
kubectl rollout status deployment/cloud-config --timeout=1800s || echo "⚠️  Cloud Config aún está iniciando. Puedes verificar con: kubectl get pods"
echo ""

# 5. Desplegar API Gateway
echo -e "${YELLOW}5. Desplegando API Gateway...${NC}"
kubectl apply -f k8s/api-gateway-deployment.yml
echo ""

# 6. Desplegar todos los microservicios
echo -e "${YELLOW}6. Desplegando microservicios...${NC}"
kubectl apply -f k8s/order-deployment.yml
kubectl apply -f k8s/user-deployment.yml
kubectl apply -f k8s/product-deployment.yml
kubectl apply -f k8s/payment-deployment.yml
kubectl apply -f k8s/shipping-deployment.yml
kubectl apply -f k8s/favourite-deployment.yml
kubectl apply -f k8s/proxy-client-deployment.yml
echo -e "${GREEN}✅ Todos los microservicios desplegados${NC}"
echo ""

# 7. Verificar estado de los pods
echo -e "${YELLOW}7. Estado de los pods:${NC}"
kubectl get pods
echo ""

# 8. Esperar a que los servicios críticos estén listos
echo -e "${YELLOW}8. Esperando a que los servicios estén listos...${NC}"
echo "Esto puede tardar varios minutos mientras se descargan las imágenes..."
echo "Verificando estado de los servicios (timeout de 30 minutos)..."
kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=1800s 2>/dev/null || echo "⚠️  API Gateway aún está iniciando"
kubectl wait --for=condition=ready pod -l app=order-service --timeout=1800s 2>/dev/null || echo "⚠️  Order Service aún está iniciando"
kubectl wait --for=condition=ready pod -l app=user-service --timeout=1800s 2>/dev/null || echo "⚠️  User Service aún está iniciando"
echo ""
echo -e "${BLUE}💡 Tip: Puedes monitorear el progreso con: kubectl get pods -w${NC}"

echo ""
echo -e "${GREEN}✅ Despliegue completado${NC}"
echo ""
echo -e "${BLUE}📋 Comandos útiles:${NC}"
echo ""
echo "Ver pods:"
echo "   kubectl get pods"
echo ""
echo "Ver servicios:"
echo "   kubectl get svc"
echo ""
if [ "$CLUSTER_TYPE" = "kind" ]; then
    echo "Ver servicios con NodePort (kind):"
    echo "   kubectl get svc -o wide"
    echo "   Los servicios NodePort estarán en puertos 30000-32767"
    echo ""
fi
echo "Ver Eureka:"
echo "   kubectl port-forward svc/service-discovery 8761:8761"
echo "   Luego abrir: http://localhost:8761"
echo ""
echo "Ver API Gateway:"
echo "   kubectl port-forward svc/api-gateway 8080:8080"
echo "   Luego abrir: http://localhost:8080"
echo ""
echo "Ver logs de un servicio:"
echo "   kubectl logs -f deployment/order-service"
echo ""
if [ "$CLUSTER_TYPE" = "kind" ]; then
    echo -e "${YELLOW}💡 Tip: Con kind puedes acceder directamente a los NodePorts${NC}"
    echo "   Obtén la IP del nodo: kubectl get nodes -o wide"
    echo "   Accede a los servicios en: http://<node-ip>:<nodeport>"
    echo ""
fi

