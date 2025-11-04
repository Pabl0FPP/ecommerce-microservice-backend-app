#!/bin/bash

# Script para iniciar los servicios de microservicios en orden
# 1. Inicia los servicios core (core.yml)
# 2. Espera 3 minutos
# 3. Inicia el resto de servicios (compose.yml)

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Función para imprimir mensajes
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

print_step() {
    echo -e "${CYAN}[STEP]${NC} $1"
}

# Función para contar regresivamente
countdown() {
    local seconds=$1
    print_info "Esperando $seconds segundos..."
    
    for ((i=seconds; i>=1; i--)); do
        printf "\r${CYAN}[COUNTDOWN]${NC} $i segundos restantes... "
        sleep 1
    done
    printf "\r${GREEN}[READY]${NC} Continuando...\n"
}

# Función para verificar si docker compose está disponible
check_docker_compose() {
    if command -v docker &> /dev/null; then
        print_success "Docker está instalado"
        
        # Verificar si usa 'docker compose' (v2) o 'docker-compose' (v1)
        if docker compose version &> /dev/null; then
            COMPOSE_CMD="docker compose"
            print_success "Usando: docker compose (v2)"
        elif command -v docker-compose &> /dev/null; then
            COMPOSE_CMD="docker-compose"
            print_success "Usando: docker-compose (v1)"
        else
            print_error "No se encontró docker compose"
            exit 1
        fi
    else
        print_error "Docker no está instalado o no está en el PATH"
        exit 1
    fi
}

# Función para verificar si el archivo existe
check_file_exists() {
    if [ ! -f "$1" ]; then
        print_error "El archivo $1 no existe"
        exit 1
    fi
}

# Función para iniciar servicios core
start_core_services() {
    print_step "PASO 1/2: Iniciando servicios core (core.yml)..."
    echo ""
    
    check_file_exists "core.yml"
    
    print_info "Ejecutando: $COMPOSE_CMD -f core.yml up -d"
    $COMPOSE_CMD -f core.yml up -d
    
    if [ $? -eq 0 ]; then
        print_success "Servicios core iniciados exitosamente"
        echo ""
        
        # Mostrar estado de los contenedores core
        print_info "Estado de los contenedores core:"
        $COMPOSE_CMD -f core.yml ps
        echo ""
        return 0
    else
        print_error "Error al iniciar servicios core"
        return 1
    fi
}

# Función para esperar
wait_for_services() {
    print_step "PASO 2/2: Esperando 3 minutos para que los servicios core estén listos..."
    echo ""
    print_warning "Los servicios core (Eureka, Config Server, etc.) necesitan tiempo para inicializar"
    echo ""
    
    # Esperar 3 minutos (180 segundos)
    countdown 180
    echo ""
    
    print_success "Tiempo de espera completado"
    echo ""
}

# Función para iniciar todos los servicios
start_all_services() {
    print_step "PASO 3/3: Iniciando todos los servicios (compose.yml)..."
    echo ""
    
    check_file_exists "compose.yml"
    
    print_info "Ejecutando: $COMPOSE_CMD up -d"
    $COMPOSE_CMD up -d
    
    if [ $? -eq 0 ]; then
        print_success "Todos los servicios iniciados exitosamente"
        echo ""
        
        # Mostrar estado de todos los contenedores
        print_info "Estado de todos los contenedores:"
        $COMPOSE_CMD ps
        echo ""
        return 0
    else
        print_error "Error al iniciar servicios"
        return 1
    fi
}

# Función para mostrar resumen
show_summary() {
    echo "=========================================="
    print_success "RESUMEN"
    echo "=========================================="
    echo ""
    print_info "Servicios iniciados:"
    echo ""
    print_info "Contenedores corriendo:"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    echo ""
    print_info "Para ver los logs de un servicio específico:"
    echo "  $COMPOSE_CMD logs -f <nombre-del-servicio>"
    echo ""
    print_info "Para detener todos los servicios:"
    echo "  $COMPOSE_CMD down"
    echo ""
    print_info "Para ver el estado de los servicios:"
    echo "  $COMPOSE_CMD ps"
    echo ""
}

# Main
main() {
    echo "=========================================="
    print_info "INICIANDO SERVICIOS DE MICROSERVICIOS"
    echo "=========================================="
    echo ""
    
    # Verificar docker compose
    check_docker_compose
    echo ""
    
    # Iniciar servicios core
    if ! start_core_services; then
        print_error "Falló al iniciar servicios core. Abortando."
        exit 1
    fi
    
    # Esperar 3 minutos
    wait_for_services
    
    # Iniciar todos los servicios
    if ! start_all_services; then
        print_error "Falló al iniciar servicios. Los servicios core ya están corriendo."
        exit 1
    fi
    
    # Mostrar resumen
    echo ""
    show_summary
    
    print_success "¡Todos los servicios han sido iniciados exitosamente!"
}

# Ejecutar main
main

