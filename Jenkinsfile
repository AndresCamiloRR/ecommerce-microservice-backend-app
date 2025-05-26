pipeline {
  agent {
    docker {
      image 'mcr.microsoft.com/azure-cli'  // imagen oficial con az y kubectl
      args '-u 0:0'
    }
  }

  environment {
    RESOURCE_GROUP = 'mi-grupo'          // Ejemplo: nombre real de tu resource group
    CLUSTER_NAME = 'mi-cluster'       // Ejemplo: nombre real de tu clúster AKS
    K8S_MANIFESTS_DIR = 'k8s'                   // Carpeta local en el repo
    AZURE_CREDENTIALS_ID = 'azure-service-principal'  // Este sí es el ID de la credencial de Jenkins
    NEWMAN_IMAGE_NAME = 'yourdockerhubusername/ecommerce-newman-runner' // ¡¡CAMBIA ESTO por tu usuario de Docker Hub y nombre de imagen!!
    NEWMAN_IMAGE_TAG = "latest" // O puedes usar algo como "${env.BUILD_NUMBER}"
    NEWMAN_REPORTS_DIR = 'newman-reports' // Directorio para los reportes de Newman
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
        expression { env.PROFILE == 'dev' }
      }
      steps {
        sh '''
          echo "Running E2E tests..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/core/newman-e2e-job.yaml
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
        '''
      }
    }

    stage('Build E2E Test Image') {
      agent {
        docker {
          image 'docker/compose:1.29.2' // Imagen con cliente Docker
          args '-v /var/run/docker.sock:/var/run/docker.sock' // Montar el socket de Docker
        }
      }
      steps {
        script {
          // Asegurar que el directorio newman exista en el workspace
          sh "mkdir -p ${env.WORKSPACE}/newman"
          // Copiar la colección de Postman al directorio 'newman' que será el contexto de build de Docker
          sh "cp \\"${env.WORKSPACE}/Ecommerce e2e.postman_collection.json\\" \\"${env.WORKSPACE}/newman/Ecommerce e2e.postman_collection.json\\""
          
          // Iniciar sesión en Docker Hub (u otro registro)
          // Asegúrate de que 'docker-hub-credentials' exista en Jenkins
          docker.withRegistry('https://index.docker.io/v1/', 'docker-hub-credentials') {
            // Construir la imagen de Docker. El Dockerfile está en newman/Dockerfile.
            // El contexto de build es el directorio 'newman'.
            def customImage = docker.build("${env.NEWMAN_IMAGE_NAME}:${env.NEWMAN_IMAGE_TAG}", "-f ${env.WORKSPACE}/newman/Dockerfile ${env.WORKSPACE}/newman")
            customImage.push()
            echo "Newman Docker image pushed: ${env.NEWMAN_IMAGE_NAME}:${env.NEWMAN_IMAGE_TAG}"
          }
        }
      }
    }

    stage('Run E2E Tests in Kubernetes') {
      // Esta etapa se ejecuta después de que los microservicios se hayan desplegado.
      // Utiliza el agente de Azure CLI que ya tiene kubectl.
      steps {
        script {
          def podName = "newman-e2e-tests-${env.BUILD_NUMBER}"
          // El namespace de Kubernetes se establece a 'default'.
          def k8sNamespace = "default" 

          sh """
            echo "Running E2E tests with Newman in Kubernetes in namespace ${k8sNamespace}..."

            # Crear y ejecutar el pod de Newman.
            # La colección se llama 'collection.json' dentro de la imagen (ver Dockerfile).
            # No se pasa la variable 'prefix' a Newman ya que está definida en la colección.
            kubectl run ${podName} \\
              --image=${env.NEWMAN_IMAGE_NAME}:${env.NEWMAN_IMAGE_TAG} \\
              --namespace=${k8sNamespace} \\
              --restart=Never \\
              --command -- /bin/sh -c "mkdir -p /reports && newman run collection.json -r cli,htmlextra --reporter-htmlextra-export /reports/report.html"

            echo "Waiting for pod ${podName} to complete in namespace ${k8sNamespace}..."
            kubectl wait --for=condition=Succeeded pod/${podName} --namespace=${k8sNamespace} --timeout=600s

            echo "Copying reports from pod ${podName} from namespace ${k8sNamespace}..."
            # Asegurar que el directorio local para los reportes exista
            mkdir -p ${env.WORKSPACE}/${env.NEWMAN_REPORTS_DIR}
            kubectl cp ${k8sNamespace}/${podName}:/reports/report.html ${env.WORKSPACE}/${env.NEWMAN_REPORTS_DIR}/report.html

            echo "Displaying logs from pod ${podName} from namespace ${k8sNamespace}..."
            kubectl logs ${podName} --namespace=${k8sNamespace}

            echo "Deleting pod ${podName} from namespace ${k8sNamespace}..."
            kubectl delete pod ${podName} --namespace=${k8sNamespace}
          """
          // Archivar los reportes HTML
          archiveArtifacts artifacts: "${env.NEWMAN_REPORTS_DIR}/report.html", fingerprint: true
        }
      }
      post {
        always {
          // Publicar el reporte HTML en Jenkins
          publishHTML([
            allowMissing: true, 
            alwaysLinkToLastBuild: true, 
            keepAll: true, 
            reportDir: env.NEWMAN_REPORTS_DIR, 
            reportFiles: 'report.html', 
            reportName: "Newman E2E (${env.PROFILE}) Test Report"
          ])
        }
        // Podrías añadir aquí lógica para marcar el build como fallido si las pruebas fallan,
        // por ejemplo, analizando los logs de Newman o si Newman puede generar un archivo de estado.
      }
    }
  }
}