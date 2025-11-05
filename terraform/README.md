# Terraform Infrastructure as Code for Kubernetes Cluster

Esta carpeta contiene la infraestructura como código (IaC) para desplegar un cluster de Kubernetes en Azure AKS siguiendo las mejores prácticas de DevOps.

## Estructura

```
terraform/
├── main.tf                    # Configuración principal
├── variables.tf               # Variables globales
├── outputs.tf                 # Outputs del módulo principal
├── versions.tf                # Versiones de Terraform y providers
├── backend.example.hcl        # Ejemplo de configuración de backend
├── environments/              # Configuraciones por entorno
│   ├── dev/
│   │   └── terraform.tfvars
│   ├── staging/
│   │   └── terraform.tfvars
│   └── prod/
│       └── terraform.tfvars
└── modules/                   # Módulos reutilizables
    ├── resource-group/        # Resource Group
    ├── networking/            # VNet y Subnets
    ├── kubernetes-cluster/    # Cluster AKS
    └── monitoring/           # Log Analytics
```

## Prerequisitos

1. **Azure CLI** instalado y configurado
   ```bash
   az login
   az account set --subscription "your-subscription-id"
   ```

2. **Terraform** >= 1.5.0
   ```bash
   terraform version
   ```

3. **Permisos de Azure**:
   - Owner o Contributor en la suscripción
   - Permisos para crear Resource Groups, VNets, AKS, etc.

## Configuración Inicial

### 1. Configurar Backend (Azure Storage)

Crea un storage account para el estado de Terraform:

```bash
# Crear resource group para el estado
az group create --name rg-terraform-state --location eastus

# Crear storage account
az storage account create \
  --name stterraformstate \
  --resource-group rg-terraform-state \
  --location eastus \
  --sku Standard_LRS

# Crear container
az storage container create \
  --name terraform-state \
  --account-name stterraformstate
```

Copia el archivo de ejemplo y configura tus valores:

```bash
cp backend.example.hcl backend.hcl
# Edita backend.hcl con tus valores
```

### 2. Configurar Variables por Entorno

Las variables específicas de cada entorno están en `environments/{env}/terraform.tfvars`.

Edita según tus necesidades antes de desplegar.

## Uso

### Inicializar Terraform

```bash
cd terraform
terraform init -backend-config=backend.hcl
```

### Desplegar en Dev

```bash
# Plan
terraform plan -var-file=environments/dev/terraform.tfvars

# Apply
terraform apply -var-file=environments/dev/terraform.tfvars
```

### Desplegar en Staging

```bash
terraform plan -var-file=environments/staging/terraform.tfvars
terraform apply -var-file=environments/staging/terraform.tfvars
```

### Desplegar en Prod

```bash
terraform plan -var-file=environments/prod/terraform.tfvars
terraform apply -var-file=environments/prod/terraform.tfvars
```

### Obtener Configuración de Kubernetes

Después del despliegue, obtén el kubeconfig:

```bash
az aks get-credentials --resource-group rg-ecommerce-dev --name aks-ecommerce-dev
```

O desde Terraform:

```bash
terraform output -raw kube_config > ~/.kube/config-aks-dev
export KUBECONFIG=~/.kube/config-aks-dev
```

## Módulos

### Resource Group
Crea un Resource Group con tags estándar.

### Networking
- Crea VNet con subnets
- Configura Network Security Groups
- Asocia NSG con subnets de AKS

### Kubernetes Cluster
- Crea cluster AKS con configuración optimizada
- Node pools con auto-scaling
- RBAC habilitado
- Integración con Azure Monitor

### Monitoring
- Log Analytics Workspace
- Integración con Container Insights

## Mejores Prácticas Implementadas

✅ **Separación por entornos**: Configuraciones independientes para dev/staging/prod  
✅ **Módulos reutilizables**: Código DRY y mantenible  
✅ **State remoto**: Backend en Azure Storage para colaboración  
✅ **Versionado**: Versiones fijas de providers y Terraform  
✅ **Tags consistentes**: Tagging estándar en todos los recursos  
✅ **RBAC**: Kubernetes RBAC y Azure RBAC configurados  
✅ **Network Policies**: Azure CNI con network policies  
✅ **Auto-scaling**: Node pools con auto-scaling habilitado  
✅ **Monitoring**: Log Analytics integrado  
✅ **Security**: NSG configurado, subnets segregadas  
✅ **Naming conventions**: Nombres consistentes y descriptivos  

## Variables Importantes

- `environment`: dev, staging, o prod
- `kubernetes_version`: Versión de Kubernetes
- `default_node_pool`: Configuración del node pool por defecto
- `additional_node_pools`: Node pools adicionales para workloads específicos
- `rbac_enabled`: Habilita Kubernetes RBAC
- `azure_rbac_enabled`: Habilita Azure RBAC (recomendado para prod)

## Outputs

Después del despliegue, puedes obtener:

- `cluster_name`: Nombre del cluster
- `cluster_fqdn`: FQDN del cluster
- `kube_config`: Configuración de Kubernetes (sensitive)
- `resource_group_name`: Nombre del resource group
- `vnet_id`: ID de la VNet

## Destruir Infraestructura

⚠️ **CUIDADO**: Esto eliminará todos los recursos

```bash
terraform destroy -var-file=environments/dev/terraform.tfvars
```

## Troubleshooting

### Error: Backend configuration not found
- Asegúrate de tener `backend.hcl` configurado
- O configura las variables de entorno del backend

### Error: Authentication failed
- Ejecuta `az login`
- Verifica permisos con `az account show`

### Error: Resource group already exists
- Usa un nombre único o elimina el resource group existente

## Mejoras Futuras

- [ ] Integración con Azure Key Vault para secrets
- [ ] Application Gateway para ingress
- [ ] Azure Container Registry (ACR)
- [ ] Azure Policy para compliance
- [ ] Disaster Recovery configuration
- [ ] Multi-region deployment

