#!/bin/bash

# Script para hacer port-forward de todos los servicios en kind

set -e

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar si kubectl está disponible
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl no está instalado"
    exit 1
fi

# Verificar si el cluster está disponible
if ! kubectl cluster-info &> /dev/null; then
    print_error "No hay cluster de Kubernetes disponible"
    exit 1
fi

print_info "Iniciando port-forward para todos los servicios..."
echo ""

# Función para hacer port-forward en background
port_forward() {
    local service=$1
    local local_port=$2
    local service_port=$3
    
    print_info "Port-forwarding $service: localhost:$local_port -> $service:$service_port"
    kubectl port-forward svc/$service $local_port:$service_port > /dev/null 2>&1 &
    local pid=$!
    echo $pid
}

# Matar procesos de port-forward anteriores
print_info "Limpiando port-forwards anteriores..."
pkill -f "kubectl port-forward" || true
sleep 1

# Array de PIDs para gestionar
PIDS=()

# Port-forward de servicios core
print_success "Servicios Core:"
PIDS+=($(port_forward service-discovery 8761 8761))
sleep 1
PIDS+=($(port_forward cloud-config 9296 9296))
sleep 1
PIDS+=($(port_forward zipkin 9411 9411))
sleep 1

# Port-forward de servicios de negocio
print_success "Servicios de Negocio:"
PIDS+=($(port_forward api-gateway 8080 8080))
sleep 1
PIDS+=($(port_forward user-service 8700 8700))
sleep 1
PIDS+=($(port_forward product-service 8500 8500))
sleep 1
PIDS+=($(port_forward order-service 8300 8300))
sleep 1
PIDS+=($(port_forward payment-service 8400 8400))
sleep 1
PIDS+=($(port_forward shipping-service 8600 8600))
sleep 1
PIDS+=($(port_forward favourite-service 8800 8800))
sleep 1
PIDS+=($(port_forward proxy-client 8900 8900))
sleep 1

echo ""
print_success "✅ Port-forwards iniciados"
echo ""
echo -e "${BLUE}📋 URLs de acceso:${NC}"
echo ""
echo "  Core Services:"
echo "    - Eureka:        http://localhost:8761"
echo "    - Cloud Config:  http://localhost:9296"
echo "    - Zipkin:        http://localhost:9411"
echo ""
echo "  Business Services:"
echo "    - API Gateway:   http://localhost:8080"
echo "    - User Service:  http://localhost:8700"
echo "    - Product Service: http://localhost:8500"
echo "    - Order Service: http://localhost:8300"
echo "    - Payment Service: http://localhost:8400"
echo "    - Shipping Service: http://localhost:8600"
echo "    - Favourite Service: http://localhost:8800"
echo "    - Proxy Client: http://localhost:8900"
echo ""
print_warning "⚠️  Los port-forwards están corriendo en background"
print_warning "⚠️  Para detenerlos, ejecuta: pkill -f 'kubectl port-forward'"
echo ""
print_info "Para verificar que funcionan:"
echo "  curl http://localhost:8761"
echo ""

# Esperar a que el usuario presione Ctrl+C
trap "echo ''; print_info 'Deteniendo port-forwards...'; pkill -f 'kubectl port-forward' || true; print_success 'Port-forwards detenidos'; exit 0" INT TERM

print_info "Port-forwards corriendo. Presiona Ctrl+C para detener..."
echo ""

# Mantener el script corriendo
while true; do
    sleep 1
done

