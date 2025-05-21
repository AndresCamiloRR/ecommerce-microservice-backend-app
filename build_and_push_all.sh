#!/bin/bash

# Solicitar el nombre de usuario de Docker Hub
read -p "Ingresa tu nombre de usuario de Docker Hub: " DOCKERHUB_USERNAME

# Verificar si se ingresó el nombre de usuario
if [ -z "$DOCKERHUB_USERNAME" ]; then
  echo "El nombre de usuario de Docker Hub no puede estar vacío."
  exit 1
fi

# Definir los servicios para construir y subir (nombres de los directorios)
SERVICES=(
  "api-gateway"
  "cloud-config"
  "favourite-service"
  "order-service"
  "payment-service"
  "product-service"
  "proxy-client"
  "service-discovery"
  "shipping-service"
  "user-service"
)

# Definir una etiqueta por defecto (puedes cambiarla si es necesario, ej: v1.0.0)
TAG="latest"

# Obtener la ruta del directorio actual (raíz del proyecto)
BASE_DIR=$(pwd)

# Iterar sobre cada servicio
for SERVICE in "${SERVICES[@]}"; do
  echo "----------------------------------------------------"
  echo "Procesando servicio: $SERVICE"
  echo "----------------------------------------------------"

  SERVICE_DIR="$BASE_DIR/$SERVICE"
  IMAGE_NAME="$DOCKERHUB_USERNAME/$SERVICE:$TAG"

  # Verificar si el directorio del servicio existe
  if [ ! -d "$SERVICE_DIR" ]; then
    echo "Directorio $SERVICE_DIR no encontrado. Saltando este servicio."
    continue
  fi

  # Navegar al directorio del servicio
  cd "$SERVICE_DIR" || { echo "Error al navegar al directorio $SERVICE_DIR"; exit 1; }

  # Verificar si existe un Dockerfile
  if [ ! -f "Dockerfile" ]; then
    echo "Dockerfile no encontrado en $SERVICE_DIR. Saltando este servicio."
    cd "$BASE_DIR" || exit 1
    continue
  fi

  # Construir la imagen Docker
  echo "Construyendo imagen $IMAGE_NAME..."
  docker build -t "$IMAGE_NAME" .
  if [ $? -ne 0 ]; then
    echo "Error al construir la imagen Docker para $SERVICE"
    cd "$BASE_DIR" || exit 1
    exit 1 # Salir si la construcción falla
  fi

  # Subir la imagen Docker a Docker Hub
  echo "Subiendo imagen $IMAGE_NAME a Docker Hub..."
  docker push "$IMAGE_NAME"
  if [ $? -ne 0 ]; then
    echo "Error al subir la imagen Docker para $SERVICE"
    # Opcionalmente, puedes decidir si continuar con otros servicios o salir
    cd "$BASE_DIR" || exit 1
    exit 1 # Salir si la subida falla
  fi

  # Volver al directorio raíz del proyecto
  cd "$BASE_DIR" || { echo "Error al volver al directorio raíz"; exit 1; }
  echo "Servicio $SERVICE procesado exitosamente."
done

echo "----------------------------------------------------"
echo "¡Todas las imágenes han sido construidas y subidas exitosamente!"
echo "----------------------------------------------------"

# Dar permisos de ejecución al script (opcional, si lo haces desde otro script)
# chmod +x build_and_push_all.sh
