#!/usr/bin/env bash
# scripts/build_and_push_services.sh
# Uso: editar la lista SERVICES en el script o pasar nombres como argumentos.
# Para cada servicio se hará:
#  1) ./mvnw -f {servicio}/pom.xml clean package
#  2) docker build -t ${DOCKER_USER}/{servicio}:${VERSION} --build-arg PROJECT_VERSION=${VERSION} -f {servicio}/Dockerfile {servicio}
#  3) docker push ${DOCKER_USER}/{servicio}:${VERSION}

set -o pipefail
# Cambia esto por tu usuario en Docker Hub si quieres
DOCKER_USER_DEFAULT="pablofpp"
VERSION_DEFAULT="0.1.0"

# --- CONFIGURACIÓN: edita esta lista si quieres valores por defecto embebidos ---
# Si pasas servicios por argumento, éstos tienen prioridad sobre esta lista.
SERVICES=(
  user-service
  shipping-service
  product-service
  order-service
  payment-service
  favourite-service
  proxy-client
)
# ----------------------------------------------------------------------------------

usage() {
  cat <<EOF
Usage: $0 [options] [service1 service2 ...]

Options:
  -u, --user <docker-user>    Docker user/namespace (default: ${DOCKER_USER_DEFAULT})
  -v, --version <version>     Image version / PROJECT_VERSION (default: ${VERSION_DEFAULT})
  -k, --keep-going            Continue processing next services even if one fails
  -n, --dry-run               Print commands without executing
  -h, --help                  Show this help

If you provide service names as positional arguments, they will override the internal SERVICES list.
To permanently change the list, edit the SERVICES array at the top of the script.
EOF
}

# Parse args
DOCKER_USER="${DOCKER_USER_DEFAULT}"
VERSION="${VERSION_DEFAULT}"
KEEP_GOING=0
DRY_RUN=0
POSITIONAL=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -u|--user)
      DOCKER_USER="$2"; shift 2;;
    -v|--version)
      VERSION="$2"; shift 2;;
    -k|--keep-going)
      KEEP_GOING=1; shift;;
    -n|--dry-run)
      DRY_RUN=1; shift;;
    -h|--help)
      usage; exit 0;;
    --) shift; break;;
    -* ) echo "Unknown option: $1"; usage; exit 1;;
    * ) POSITIONAL+=("$1"); shift;;
  esac
done

if [[ ${#POSITIONAL[@]} -gt 0 ]]; then
  SERVICES=(${POSITIONAL[@]})
fi

echo "Docker user: ${DOCKER_USER}"
echo "Image version: ${VERSION}"
echo "Services to process: ${SERVICES[*]}"
if [[ ${DRY_RUN} -eq 1 ]]; then
  echo "DRY RUN mode: no se ejecutarán comandos"
fi

# Helper to run or print commands
run_cmd() {
  if [[ ${DRY_RUN} -eq 1 ]]; then
    echo "+ $*"
    return 0
  else
    echo "-> Ejecutando: $*"
    eval "$@"
    return $?
  fi
}

# For each service
for svc in "${SERVICES[@]}"; do
  echo "\n===== Procesando: ${svc} ====="

  # Check directory
  if [[ ! -d "${svc}" ]]; then
    echo "ERROR: directorio ./${svc} no existe. Saltando."
    if [[ ${KEEP_GOING} -eq 1 ]]; then
      continue
    else
      exit 1
    fi
  fi

  # 1) Maven build
  # Preferir wrapper local si existe, sino wrapper en repo raíz, sino mvn
  if [[ -x "${svc}/mvnw" ]]; then
    MVN_CMD="${svc}/mvnw -f ${svc}/pom.xml clean package -DskipTests=false"
  elif [[ -x ./mvnw ]]; then
    MVN_CMD="./mvnw -f ${svc}/pom.xml clean package -DskipTests=false"
  else
    MVN_CMD="mvn -f ${svc}/pom.xml clean package -DskipTests=false"
  fi

  if ! run_cmd ${MVN_CMD}; then
    echo "ERROR: mvn build falló para ${svc}"
    if [[ ${KEEP_GOING} -eq 1 ]]; then
      continue
    else
      exit 1
    fi
  fi

  # 2) Docker build
  DOCKERFILE_PATH="${svc}/Dockerfile"
  if [[ ! -f "${DOCKERFILE_PATH}" ]]; then
    echo "ERROR: Dockerfile no encontrado en ./${DOCKERFILE_PATH}. Saltando push/build para ${svc}."
    if [[ ${KEEP_GOING} -eq 1 ]]; then
      continue
    else
      exit 1
    fi
  fi

  IMAGE_TAG="${DOCKER_USER}/${svc}:${VERSION}"
  DOCKER_BUILD_CMD=(docker build -t "${IMAGE_TAG}" --build-arg PROJECT_VERSION="${VERSION}" -f "${DOCKERFILE_PATH}" "${svc}")

  if ! run_cmd "${DOCKER_BUILD_CMD[@]}"; then
    echo "ERROR: docker build falló para ${svc}"
    if [[ ${KEEP_GOING} -eq 1 ]]; then
      continue
    else
      exit 1
    fi
  fi

  # 3) Docker push
  if ! run_cmd docker push "${IMAGE_TAG}"; then
    echo "ERROR: docker push falló para ${svc}"
    if [[ ${KEEP_GOING} -eq 1 ]]; then
      continue
    else
      exit 1
    fi
  fi

  echo "==> ${svc} procesado correctamente: ${IMAGE_TAG}"
done

echo "\nTodos los servicios procesados."
