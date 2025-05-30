pipeline {
  agent {
    docker {
      image 'leferez/jenkins-agent:latest'
      args '-u 0:0 -v /var/run/docker.sock:/var/run/docker.sock'
    }
  }

  environment {
    RESOURCE_GROUP = 'mi-grupo'          // Nombre del resource group
    CLUSTER_NAME = 'mi-cluster'       // Nombre del clúster AKS
    K8S_MANIFESTS_DIR = 'k8s'                   // Carpeta local en el repo
    AZURE_CREDENTIALS_ID = 'azure-service-principal'  // ID de las credenciales de Azure en Jenkins
    GH_TOKEN = credentials('github-token-txt')
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
        sh '''
          echo "Building the project..."
          mvn clean package -DskipTests
        '''
      }
    }

    stage('Unit and Integration Tests') {
      steps {
        sh '''
          echo "Running unit and integration tests..."
          mvn clean verify -DskipTests=false
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

            set -x
            echo "Attempting Azure login..."
            echo "AZ_CLIENT_ID is present (masked): $AZ_CLIENT_ID"
            echo "AZ_TENANT_ID is present (masked): $AZ_TENANT_ID"
            echo "Checking az version..."
            az --version
            echo "Attempting login now..."
            az login --service-principal -u "$AZ_CLIENT_ID" -p "$AZ_CLIENT_SECRET" --tenant "$AZ_TENANT_ID" --output none
            echo "Setting Azure subscription..."
            az account set --subscription "$AZ_SUBSCRIPTION_ID" --output none
            echo "Azure login and subscription set successfully."
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
    stage('Correr e2e') {
      when {
        expression { env.PROFILE == 'stage' }
      }
      steps {
        sh '''
          echo "Running E2E tests..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/core/newman-e2e-job.yaml
          kubectl wait --for=condition=complete job/newman-e2e-job --timeout=600s
          echo "Fetching Newman results..."
          kubectl logs job/newman-e2e-job
        '''
      }
    }

    stage('IP de api-gateway') {
      steps {
        sh '''
          echo "Waiting for API Gateway IP address..."
          sleep 30 # Esperar a que el balanceador de carga asigne la IP
          GATEWAY_IP=$(kubectl get service api-gateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
          echo "API Gateway URL: http://$GATEWAY_IP:8080"
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
          echo "Waiting for Locust IP address..."
          sleep 30 # Esperar a que el balanceador de carga asigne la IP
          LOCUST_IP=$(kubectl get service locust -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
          echo "Locust URL: http://$LOCUST_IP:8089"
        '''
      }
    }
    
    stage('Generar Release Notes') {
      when {
        expression { env.PROFILE == 'prod' }
      }
      steps {
        withCredentials([string(credentialsId: 'github-token-txt', variable: 'GH_TOKEN')]) {
          script {
            def now = new Date()
            def tag = now.format('MM.dd.HH.mm')
            def title = "Release ${tag}"
            def commitHash = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
            def commitMessage = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
            def recentCommits = sh(returnStdout: true, script: 'git log --oneline -n 5').trim()
            def changedFiles = sh(returnStdout: true, script: 'git diff --name-only 695a6d49a8fb5559b025e22017df1ec657104309..HEAD || echo "No changes detected"').trim()

            def notes = """
# 🚀 Release Notes - ${tag}

**📅 Fecha:** ${now.format('yyyy-MM-dd HH:mm:ss')}  
**🔑 Commit Actual:** ${commitHash}  

## 🆕 Último Commit
${commitMessage}

## 📜 Últimos 5 Commits
${recentCommits}

## 📂 Archivos Modificados
${changedFiles}

## ✅ Validaciones Automatizadas
- Pruebas unitarias en user-service
- Pruebas de integración en user-service y payment-service
- Pruebas End-to-End con Postman Newman
- Pruebas de carga con Locust en el puerto 8089
- Retornar las IPs de los servicios desplegados api-gateway y locust

## 🧩 Microservicios involucrados
- api-gateway
- cloud-config
- favourite-service
- locust (para pruebas de carga)
- newman (como un job de Kubernetes)
- order-service
- payment-service
- product-service
- proxy-client
- shipping-service
- user-service
- zipkin

"""

            // Ejecutar los comandos shell
            sh """
              git config user.email "ci-bot@example.com"
              git config user.name "CI Bot"
              git config --global url."https://oauth2:${GH_TOKEN}@github.com/".insteadOf "https://github.com/"
              
              git tag ${tag}
              git push origin ${tag}
              gh release create ${tag} --title "${title}" --notes "${notes}"
            """

            echo "✅ Release ${tag} creado con changelog personalizado"
          }
        }
      }
    }


  }
}