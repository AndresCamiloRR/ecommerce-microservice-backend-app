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
}

  stages {
    stage('Checkout código') {
      steps {
        checkout scm
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
          echo "Desplegando recursos de Kubernetes..."
          kubectl apply -f ${K8S_MANIFESTS_DIR}/zipkin-deployment.yaml
        '''
      }
    }
  }
}