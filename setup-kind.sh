#!/bin/bash

# Script para configurar kind cluster para microservicios

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Verificar que kind está instalado
if ! command -v kind &> /dev/null; then
    print_error "kind no está instalado. Por favor instálalo primero."
    exit 1
fi

print_success "kind está instalado: $(kind version)"

# Verificar que Docker está corriendo
if ! docker ps &> /dev/null; then
    print_error "Docker no está corriendo. Por favor inicia Docker."
    exit 1
fi

print_success "Docker está corriendo"

# Verificar si ya existe un cluster
CLUSTER_NAME="ecommerce"
if kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
    print_warning "El cluster '${CLUSTER_NAME}' ya existe"
    read -p "¿Quieres eliminarlo y crear uno nuevo? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "Eliminando cluster existente..."
        kind delete cluster --name ${CLUSTER_NAME}
        print_success "Cluster eliminado"
    else
        print_info "Usando cluster existente"
        exit 0
    fi
fi

# Crear el cluster
print_info "Creando cluster de kind '${CLUSTER_NAME}'..."
if [ -f "kind-cluster-config.yaml" ]; then
    kind create cluster --name ${CLUSTER_NAME} --config kind-cluster-config.yaml
else
    kind create cluster --name ${CLUSTER_NAME}
fi

if [ $? -eq 0 ]; then
    print_success "Cluster '${CLUSTER_NAME}' creado exitosamente"
else
    print_error "Error al crear el cluster"
    exit 1
fi

# Configurar kubectl
print_info "Configurando kubectl para usar el cluster de kind..."
kubectl cluster-info --context kind-${CLUSTER_NAME}

if [ $? -eq 0 ]; then
    print_success "kubectl configurado correctamente"
else
    print_error "Error al configurar kubectl"
    exit 1
fi

# Mostrar información del cluster
echo ""
print_info "Información del cluster:"
kubectl get nodes

echo ""
print_success "¡Cluster de kind configurado exitosamente!"
echo ""
print_info "Comandos útiles:"
echo "  Ver nodos:              kubectl get nodes"
echo "  Ver pods:              kubectl get pods --all-namespaces"
echo "  Eliminar cluster:       kind delete cluster --name ${CLUSTER_NAME}"
echo "  Ver clusters:          kind get clusters"
echo ""

