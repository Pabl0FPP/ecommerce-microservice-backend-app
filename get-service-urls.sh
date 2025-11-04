#!/bin/bash

# Script para obtener las URLs de acceso a los servicios en kind

echo "🔍 Obteniendo información de acceso a los servicios..."
echo ""

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Obtener IP del host
HOST_IP=$(hostname -I | awk '{print $1}')
echo -e "${BLUE}📍 IP del host: ${HOST_IP}${NC}"
echo ""

# Verificar si es kind
if kubectl config current-context | grep -q "kind"; then
    echo -e "${GREEN}✅ Cluster de kind detectado${NC}"
    echo ""
    echo -e "${YELLOW}📋 URLs de acceso a los servicios (NodePort):${NC}"
    echo ""
    
    # Obtener servicios con NodePort
    kubectl get svc -o json | jq -r '.items[] | select(.spec.type=="NodePort") | 
        "\(.metadata.name):\n  - Localhost: http://localhost:\(.spec.ports[0].nodePort)\n  - IP Host: http://'$HOST_IP':\(.spec.ports[0].nodePort)\n  - Cluster IP: http://\(.spec.clusterIP):\(.spec.ports[0].port)\n"' 2>/dev/null || {
        # Si jq no está instalado, usar awk
        echo "Servicio | NodePort | URL Localhost | URL IP Host"
        echo "---------|----------|---------------|-------------"
        kubectl get svc -o wide | grep NodePort | awk -v ip="$HOST_IP" '{
            split($5, ports, ":");
            split(ports[2], nodeport, "/");
            svc = $1;
            np = nodeport[1];
            print svc " | " np " | http://localhost:" np " | http://" ip ":" np
        }'
    }
else
    echo -e "${YELLOW}⚠️  No es un cluster de kind${NC}"
    echo "Para acceder a los servicios, usa port-forward:"
    echo "  kubectl port-forward svc/<service-name> <local-port>:<service-port>"
fi

echo ""
echo -e "${BLUE}💡 Tip: Con kind puedes acceder directamente a los NodePorts usando:${NC}"
echo "   - localhost:<nodeport>"
echo "   - 127.0.0.1:<nodeport>"
echo "   - <tu-ip-local>:<nodeport>"
echo ""

