pipeline {
  agent {
    docker {
      image 'mcr.microsoft.com/azure-cli'  // imagen oficial con az y kubectl
      args '-u 0:0'
    }
  }

  options {
    skipDefaultCheckout true // Deshabilitamos el checkout automático
  }

  environment {
    RESOURCE_GROUP = 'mi-grupo'          // Ejemplo: nombre real de tu resource group
    CLUSTER_NAME = 'mi-cluster'       // Ejemplo: nombre real de tu clúster AKS
    K8S_MANIFESTS_DIR = 'k8s'                   // Carpeta local en el repo
    AZURE_CREDENTIALS_ID = 'azure-service-principal'  // Este sí es el ID de la credencial de Jenkins
    NEWMAN_IMAGE_NAME = 'yourdockerhubusername/ecommerce-newman-runner' // ¡¡CAMBIA ESTO por tu usuario de Docker Hub y nombre de imagen!!
    NEWMAN_IMAGE_TAG = "latest" // O puedes usar algo como "${env.BUILD_NUMBER}"
    NEWMAN_REPORTS_DIR = 'newman-reports' // Directorio para los reportes de Newman
    GIT_TRACE = "1" // Habilita logs detallados de Git
    GIT_CURL_VERBOSE = "1" // Habilita logs detallados de Git para operaciones HTTP
  }

  stages {
    stage('Prepare Workspace') {
      steps {
        echo "Cleaning workspace..."
        cleanWs()
        echo "Checking out SCM..."
        checkout scm
        sh 'echo "Git checkout complete. Current directory:" && pwd && ls -la .git'
      }
    }

    stage('User Input') {
      steps {
        script {
          env.PROFILE = input message: "Elige un perfil (prod / dev / stage)",
                              parameters: [choice(name: 'PROFILE', choices: ['prod', 'dev', 'stage'], description: 'Selecciona el perfil de despliegue')]
        }
      }
    }

    /*
    stage('Build') {
      agent {
        docker {
          image 'maven:3.9.6-eclipse-temurin-11' // Maven + JDK 11
        }
      }
      steps {
        sh '''
          echo "Building the project..."
          mvn clean package -DskipTests
        '''
      }
    }

    stage('Unit and Integration Tests') {
      agent {
        docker {
          image 'maven:3.9.6-eclipse-temurin-11' // Maven + JDK 11
        }
      }
      steps {
        sh '''
          echo "Running unit and integration tests..."
          mvn clean verify -DskipTests=false
        '''
      }
    }

    stage('Build and Push Docker Images') {
      agent {
        docker {
          image 'docker/compose:1.29.2' // o una versión que incluya ambas cosas
          args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
      }
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'docker-hub-credentials',
          usernameVariable: 'DOCKER_USERNAME',
          passwordVariable: 'DOCKER_PASSWORD'
        )]) {
          sh '''
            echo "Logging in to Docker Hub..."
            echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

            echo "Building and pushing Docker images..."
            docker-compose -f compose.yml build
            docker-compose -f compose.yml push

            echo "Logout from Docker Hub..."
            docker logout
          '''
        }
      }
    }
    */

    stage('Login Azure') {
      steps {
        withCredentials([azureServicePrincipal(
          credentialsId: env.AZURE_CREDENTIALS_ID,
          subscriptionIdVariable: 'AZ_SUBSCRIPTION_ID',
          clientIdVariable: 'AZ_CLIENT_ID',
          clientSecretVariable: 'AZ_CLIENT_SECRET',
          tenantIdVariable: 'AZ_TENANT_ID'
        )]) {
          sh '''
            echo "Iniciando sesión en Azure..."
            az login --service-principal -u $AZ_CLIENT_ID -p $AZ_CLIENT_SECRET --tenant $AZ_TENANT_ID
            az account set --subscription $AZ_SUBSCRIPTION_ID
          '''
        }
      }
    }

    stage('Obtener credenciales AKS') {
      steps {
        sh '''
          echo "Instalando kubectl..."
          az aks install-cli
          echo "Obteniendo credenciales del clúster..."
          az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --overwrite-existing
          kubectl config current-context
        '''
      }
    }
    /*
    stage('Desplegar manifiestos') {
      steps {
        sh '''
          echo "Deploying Core Services..."
          echo "Deploying Zipkin..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/core/zipkin-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=zipkin --timeout=200s
          
          echo "Deploying Service Discovery..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/$PROFILE/service-discovery-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=service-discovery --timeout=300s

          echo "Deploying Cloud Config..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/core/cloud-config-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=cloud-config --timeout=300s
        '''
      }
    }

    stage('Desplegar microservicios') {
      steps {
        sh """
          echo "Deploying Microservices..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/${PROFILE}/api-gateway-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/${PROFILE}/favourite-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=favourite-service --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/${PROFILE}/order-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=order-service --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/${PROFILE}/payment-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=payment-service --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/${PROFILE}/product-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=product-service --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/${PROFILE}/proxy-client-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=proxy-client --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/${PROFILE}/shipping-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=shipping-service --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/${PROFILE}/user-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=user-service --timeout=300s
        """
      }
    }
    */
    stage('Correr e2e') {
      when {
        expression { env.PROFILE == 'dev' }
      }
      steps {
        sh '''
          echo "Running E2E tests..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/core/newman-e2e-job.yaml
          kubectl wait --for=condition=complete job/newman-e2e-job --timeout=600s
          echo "Fetching Newman results..."
          kubectl logs job/newman-e2e-tests
        '''
      }
    }
    
    stage('Desplegar Locust') {
      when {
        expression { env.PROFILE == 'dev' || env.PROFILE == 'stage' }
      }
      steps {
        sh '''
          echo "Deploying Locust..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/core/locust-deployment.yaml

          echo "Esperando a que el LoadBalancer asigne una IP externa..."
          for i in {1..30}; do
            EXTERNAL_IP=$(kubectl get svc locust -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
            if [ -n "$EXTERNAL_IP" ]; then
              echo "Locust está disponible en: http://$EXTERNAL_IP:8089"
              break
            fi
            echo "Esperando IP externa... ($i)"
            sleep 5
          done

          if [ -z "$EXTERNAL_IP" ]; then
            echo "⚠️  No se obtuvo una IP externa para Locust tras esperar 150 segundos."
            exit 1
          fi
        '''
      }
    }
  }
}