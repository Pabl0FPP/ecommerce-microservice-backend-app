This folder contains basic Kubernetes manifests (Deployment + Service) for the ecommerce microservices.

Usage (minikube):

1. Start minikube (if not already running):
   minikube start --driver=docker

2. Apply manifests:
   kubectl apply -f k8s/

3. Wait for deployments to be ready:
   kubectl rollout status deployment/service-discovery
   kubectl rollout status deployment/cloud-config
   kubectl rollout status deployment/zipkin
   kubectl rollout status deployment/api-gateway

4. To expose the gateway to your host:
   kubectl port-forward svc/api-gateway 8080:8080
   # then access http://localhost:8080

Notes:
- These manifests are minimal and assume images are available in the environment (local Docker registry / pre-pulled). If using minikube's Docker daemon, run:
  eval $(minikube -p minikube docker-env)
  and then build/push images into that Docker environment.

- Eureka clients are configured to contact the service-discovery service via
  http://service-discovery:8761/eureka/

- If you want external exposure via NodePort or Ingress, modify the Service type accordingly.
