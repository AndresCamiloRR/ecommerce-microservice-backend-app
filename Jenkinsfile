pipeline {
  agent any

  environment {
    RESOURCE_GROUP = 'mi-grupo'          // Ejemplo: nombre real de tu resource group
    CLUSTER_NAME = 'mi-cluster'       // Ejemplo: nombre real de tu clúster AKS
    K8S_MANIFESTS_DIR = 'k8s'                   // Carpeta local en el repo
    AZURE_CREDENTIALS_ID = 'azure-service-principal'  // Este sí es el ID de la credencial de Jenkins
    JAVA_HOME = "C:\\Program Files\\Java\\jdk-17" // <--- IMPORTANT: SET YOUR JDK PATH HERE
  }

  stages {
    stage('User Input') {
      steps {
        script {
          env.PROFILE = input message: "Elige un perfil (prod / dev / stage)",
                              parameters: [choice(name: 'PROFILE', choices: ['prod', 'dev', 'stage'], description: 'Selecciona el perfil de despliegue')]
        }
      }
    }

    stage('Checkout') {
      steps {
        checkout scm
      }
    }
    
    stage('Build') {
      steps {
        bat '''
          echo "Building the project..."
          ./mvnw clean package -DskipTests
        '''
      }
    }

    stage('Unit and Integration Tests') {
      steps {
        bat '''
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
          bat '''
            echo "Logging in to Docker Hub..."
            echo %DOCKER_PASSWORD% | docker login -u %DOCKER_USERNAME% --password-stdin

            echo "Building and pushing Docker images..."
            docker-compose -f compose.yml build
            docker-compose -f compose.yml push

            echo "Logout from Docker Hub..."
            docker logout
          '''
        }
      }
    }
    

    stage('Login Azure') {
      steps {
        withCredentials([azureServicePrincipal(
          credentialsId: env.AZURE_CREDENTIALS_ID,
          subscriptionIdVariable: 'AZ_SUBSCRIPTION_ID',
          clientIdVariable: 'AZ_CLIENT_ID',
          clientSecretVariable: 'AZ_CLIENT_SECRET',
          tenantIdVariable: 'AZ_TENANT_ID'
        )]) {
          bat '''
            echo "Iniciando sesión en Azure..."
            az login --service-principal -u %AZ_CLIENT_ID% -p %AZ_CLIENT_SECRET% --tenant %AZ_TENANT_ID%
            az account set --subscription %AZ_SUBSCRIPTION_ID%
          '''
        }
      }
    }

    stage('Obtener credenciales AKS') {
      steps {
        bat '''
          echo "Instalando kubectl..."
          az aks install-cli
          echo "Obteniendo credenciales del clÃºster..."
          az aks get-credentials --resource-group %RESOURCE_GROUP% --name %CLUSTER_NAME% --overwrite-existing
          kubectl config current-context
        '''
      }
    }
    
    stage('Desplegar manifiestos') {
      steps {
        bat '''
          echo "Deploying Core Services..."
          echo "Deploying Zipkin..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/core/zipkin-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=zipkin --timeout=200s
          
          echo "Deploying Service Discovery..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/%PROFILE%/service-discovery-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=service-discovery --timeout=300s

          echo "Deploying Cloud Config..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/core/cloud-config-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=cloud-config --timeout=300s
        '''
      }
    }

    stage('Desplegar microservicios') {
      steps {
        bat """
          echo "Deploying Microservices..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/%PROFILE%/api-gateway-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/%PROFILE%/favourite-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=favourite-service --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/%PROFILE%/order-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=order-service --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/%PROFILE%/payment-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=payment-service --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/%PROFILE%/product-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=product-service --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/%PROFILE%/proxy-client-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=proxy-client --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/%PROFILE%/shipping-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=shipping-service --timeout=300s
          kubectl apply -f ${K8S_MANIFESTS_DIR}/%PROFILE%/user-service-deployment.yaml
          kubectl wait --for=condition=ready pod -l app=user-service --timeout=300s
        """
      }
    }
    stage('Correr e2e') {
      when {
        expression { env.PROFILE == 'dev' }
      }
      steps {
        bat '''
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
        bat '''
          echo "Deploying Locust..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/core/locust-deployment.yaml

          echo "Esperando a que el LoadBalancer asigne una IP externa..."
          for /L %%i in (1,1,30) do (
            for /f "tokens=*" %%j in ('kubectl get svc locust -o jsonpath="{.status.loadBalancer.ingress[0].ip}"') do set EXTERNAL_IP=%%j
            if defined EXTERNAL_IP (
              echo Locust estÃ¡ disponible en: http://%EXTERNAL_IP%:8089
              goto :locust_done
            )
            echo "Esperando IP externa... (%%i)"
            timeout /t 5 /nobreak > nul
          )

          echo "âš ï¸  No se obtuvo una IP externa para Locust tras esperar 150 segundos."
          exit /b 1
          
          :locust_done
        '''
      }
    }
  }
}