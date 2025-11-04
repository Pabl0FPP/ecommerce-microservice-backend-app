#!/bin/bash

# Script para ejecutar pruebas de microservicios
# Uso: ./run-tests.sh [unit|integration|e2e|all]

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# Función para ejecutar pruebas unitarias
run_unit_tests() {
    print_info "Ejecuta solo las pruebas unitarias (excluye IntegrationTest y E2ETest)..."
    echo ""
    
    # Ejecutar pruebas unitarias (excluyendo IntegrationTest y E2ETest)
    # Ejecutamos cada servicio por separado para mejor control
    local failed_services=()
    
    for service in payment-service favourite-service shipping-service order-service product-service user-service; do
        print_info "Ejecutando pruebas unitarias en $service..."
        ./mvnw test \
            -Dtest="*ServiceImplTest,*HelperTest,*UtilTest" \
            -DfailIfNoTests=false \
            -pl $service
        
        if [ $? -ne 0 ]; then
            failed_services+=($service)
        fi
    done
    
    if [ ${#failed_services[@]} -eq 0 ]; then
        print_success "Pruebas unitarias completadas exitosamente"
        return 0
    else
        print_error "Las pruebas unitarias fallaron en: ${failed_services[*]}"
        return 1
    fi
}

# Función para ejecutar pruebas de integración
run_integration_tests() {
    print_info "Ejecutando pruebas de integración..."
    print_warning "Asegúrate de que los servicios estén desplegados y corriendo"
    echo ""
    print_info "Orden de ejecución basado en dependencias:"
    print_info "  1. User Service (otros servicios lo necesitan)"
    print_info "  2. Product Service (otros servicios lo necesitan)"
    print_info "  3. Order Service (otros servicios lo necesitan)"
    print_info "  4. Favourite Service (necesita User y Product)"
    print_info "  5. Shipping Service (necesita Order y Product)"
    print_info "  6. Payment Service (necesita Order)"
    echo ""
    
    # Ejecutar pruebas de integración en orden lógico según dependencias
    local failed_services=()
    
    # Orden: servicios base primero, luego servicios que dependen de ellos
    local services_in_order=(
        "user-service"      # 1. Base - otros servicios lo necesitan
        "product-service"  # 2. Base - otros servicios lo necesitan
        "order-service"    # 3. Necesita User (via Cart)
        "favourite-service" # 4. Necesita User y Product
        "shipping-service" # 5. Necesita Order y Product
        "payment-service"  # 6. Necesita Order
    )
    
    for service in "${services_in_order[@]}"; do
        print_info "Ejecutando pruebas de integración en $service..."
        ./mvnw test \
            -Dtest="*IntegrationTest" \
            -DfailIfNoTests=false \
            -pl $service
        
        if [ $? -ne 0 ]; then
            failed_services+=($service)
            print_warning "Tests fallaron en $service - los siguientes servicios pueden verse afectados"
        else
            print_success "Tests pasaron en $service"
        fi
        echo ""
    done
    
    if [ ${#failed_services[@]} -eq 0 ]; then
        print_success "Pruebas de integración completadas exitosamente"
        return 0
    else
        print_error "Las pruebas de integración fallaron en: ${failed_services[*]}"
        return 1
    fi
}

# Función para ejecutar pruebas E2E
run_e2e_tests() {
    print_info "Ejecutando pruebas E2E (End-to-End)..."
    print_warning "Asegúrate de que todos los servicios estén desplegados y corriendo"
    echo ""
    
    # Ejecutar pruebas E2E (cuando estén implementadas)
    local failed_services=()
    local no_tests=()
    
    for service in payment-service favourite-service shipping-service order-service product-service user-service; do
        print_info "Ejecutando pruebas E2E en $service..."
        ./mvnw test \
            -Dtest="*E2ETest" \
            -DfailIfNoTests=false \
            -pl $service 2>&1 | grep -q "No tests were found"
        
        if [ $? -eq 0 ]; then
            print_warning "No hay pruebas E2E implementadas en $service"
            no_tests+=($service)
        else
            ./mvnw test \
                -Dtest="*E2ETest" \
                -DfailIfNoTests=false \
                -pl $service
            
            if [ $? -ne 0 ]; then
                failed_services+=($service)
            fi
        fi
    done
    
    if [ ${#failed_services[@]} -eq 0 ] && [ ${#no_tests[@]} -eq 6 ]; then
        print_warning "Las pruebas E2E aún no están implementadas"
        return 0
    elif [ ${#failed_services[@]} -eq 0 ]; then
        print_success "Pruebas E2E completadas exitosamente"
        return 0
    else
        print_error "Las pruebas E2E fallaron en: ${failed_services[*]}"
        return 1
    fi
}

# Función para ejecutar todas las pruebas
run_all_tests() {
    print_info "Ejecutando todas las pruebas..."
    echo ""
    
    local unit_result=0
    local integration_result=0
    local e2e_result=0
    
    # Ejecutar pruebas unitarias
    echo "=========================================="
    print_info "PASO 1/3: Pruebas Unitarias"
    echo "=========================================="
    run_unit_tests
    unit_result=$?
    echo ""
    
    # Ejecutar pruebas de integración
    echo "=========================================="
    print_info "PASO 2/3: Pruebas de Integración"
    echo "=========================================="
    run_integration_tests
    integration_result=$?
    echo ""
    
    # Ejecutar pruebas E2E
    echo "=========================================="
    print_info "PASO 3/3: Pruebas E2E"
    echo "=========================================="
    run_e2e_tests
    e2e_result=$?
    echo ""
    
    # Resumen final
    echo "=========================================="
    print_info "RESUMEN DE RESULTADOS"
    echo "=========================================="
    
    if [ $unit_result -eq 0 ]; then
        print_success "✓ Pruebas Unitarias: OK"
    else
        print_error "✗ Pruebas Unitarias: FALLIDAS"
    fi
    
    if [ $integration_result -eq 0 ]; then
        print_success "✓ Pruebas de Integración: OK"
    else
        print_error "✗ Pruebas de Integración: FALLIDAS"
    fi
    
    if [ $e2e_result -eq 0 ]; then
        print_success "✓ Pruebas E2E: OK"
    else
        print_warning "✗ Pruebas E2E: FALLIDAS o NO IMPLEMENTADAS"
    fi
    
    echo ""
    
    # Retornar código de salida según resultados
    if [ $unit_result -eq 0 ] && [ $integration_result -eq 0 ] && [ $e2e_result -eq 0 ]; then
        print_success "Todas las pruebas pasaron exitosamente!"
        return 0
    else
        print_error "Algunas pruebas fallaron"
        return 1
    fi
}

# Función para mostrar ayuda
show_help() {
    echo "Uso: ./run-tests.sh [unit|integration|e2e|all]"
    echo ""
    echo "Opciones:"
    echo "  unit         - Ejecuta solo las pruebas unitarias"
    echo "  integration  - Ejecuta solo las pruebas de integración"
    echo "  e2e          - Ejecuta solo las pruebas E2E (cuando estén implementadas)"
    echo "  all          - Ejecuta todas las pruebas (unitarias, integración y E2E)"
    echo ""
    echo "Ejemplos:"
    echo "  ./run-tests.sh unit"
    echo "  ./run-tests.sh integration"
    echo "  ./run-tests.sh e2e"
    echo "  ./run-tests.sh all"
    echo ""
}

# Main
case "${1:-all}" in
    unit)
        run_unit_tests
        ;;
    integration)
        run_integration_tests
        ;;
    e2e)
        run_e2e_tests
        ;;
    all)
        run_all_tests
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Opción inválida: $1"
        echo ""
        show_help
        exit 1
        ;;
esac

