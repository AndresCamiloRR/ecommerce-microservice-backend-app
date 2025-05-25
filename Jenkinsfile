pipeline {
  agent any

  environment {
    RESOURCE_GROUP = 'mi-grupo'          // Ejemplo: nombre real de tu resource group
    CLUSTER_NAME = 'mi-cluster'       // Ejemplo: nombre real de tu clúster AKS
    K8S_MANIFESTS_DIR = 'k8s'                   // Carpeta local en el repo
    AZURE_CREDENTIALS_ID = 'azure-service-principal'  // ID de la credencial de Jenkins
    PROFILE_CREDENTIAL_ID   = 'profile'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build') {
      steps {
        sh '''
          echo "Building the project..."
          ./mvnw clean package -DskipTests
        '''
      }
    }

    stage('Unit and Integration Tests') {
      steps {
        sh '''
          echo "Running unit and integration tests..."
          ./mvnw clean verify -DskipTests=false
        '''
      }
    }

    stage('Build and Push Docker Images') {
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
    /*
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

    stage('Deploy Core Services') {
      steps {
        sh """
          echo "Deploying Core Services..."
          echo "Deploying Zipkin..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/zipkin-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=zipkin --timeout=200s

          echo "Deploying Service Discovery..."
          kubectl apply -f <(envsubst < "${K8S_MANIFESTS_DIR}/service-discovery-deployment.yaml")
          kubectl wait --for=condition=ready pod -l app=service-discovery --timeout=300s

          echo "Deploying Cloud Config..."
          kubectl apply -f <(envsubst < "${K8S_MANIFESTS_DIR}/cloud-config-deployment.yaml")
          kubectl wait --for=condition=ready pod -l app=cloud-config --timeout=300s
        """
      }
    }

    stage('Deploy Remaining Services') {
      steps {
        script {
          def remainingServices = [
            'api-gateway-deployment.yaml',
            'favourite-service-deployment.yaml',
            'order-service-deployment.yaml',
            'payment-service-deployment.yaml',
            'product-service-deployment.yaml',
            'proxy-client-deployment.yaml',
            'shipping-service-deployment.yaml',
            'user-service-deployment.yaml'
          ]

          sh "echo Deploying Remaining Services..."
          for (serviceManifest in remainingServices) {
            sh "kubectl apply -f <(PROFILE=${env.PROFILE_CREDENTIAL_ID} envsubst < \\"${K8S_MANIFESTS_DIR}/${serviceManifest}\\")"
            // Optional: Add individual waits here if needed, e.g.:
            // def appName = serviceManifest.split('-deployment.yaml')[0]
            // sh "kubectl wait --for=condition=ready pod -l app=${appName} --timeout=180s"
          }
          sh "echo All services have been applied. Monitoring pod status..."
          sh "kubectl get pods -w"
        }
      }
    }
    */
  }
}
