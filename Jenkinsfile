pipeline {
    agent {
        label 'kubernetes'
    }

    tools {
        // Herramientas necesarias
        jdk 'jdk11'
        maven 'maven3'
    }

    environment {
        // Variables de entorno
        MINIKUBE_HOME = tool name: 'minikube', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        KUBECTL_HOME = tool name: 'kubectl', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        PATH = "${MINIKUBE_HOME}:${KUBECTL_HOME}:${PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                // Obtener el código del repositorio
                checkout scm
            }
        }

        stage('Verify Minikube') {
            steps {
                script {
                    // Verificar que Minikube esté en ejecución
                    echo "Verificando que Minikube está en ejecución..."
                    def minikubeStatus = sh(script: 'minikube status', returnStdout: true).trim()
                    if (minikubeStatus.contains('host: Stopped')) {
                        echo "Iniciando Minikube..."
                        sh 'minikube start --memory=4096 --cpus=2'
                    } else {
                        echo "Minikube ya está en ejecución."
                    }
                }
            }
        }

        stage('Enable Addons') {
            steps {
                // Habilitar addons necesarios
                echo "Habilitando addons necesarios..."
                sh 'minikube addons enable ingress'
                sh 'minikube addons enable metrics-server'
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                // Cambiar al directorio k8s y aplicar las configuraciones en orden
                dir('k8s') {
                    echo "Aplicando configuraciones de Kubernetes en orden..."

                    // 1. Zipkin
                    echo "Desplegando Zipkin..."
                    sh 'kubectl apply -f zipkin-deployment.yaml'
                    sh 'kubectl wait --for=condition=available --timeout=300s deployment/zipkin'

                    // 2. Service Discovery (Eureka)
                    echo "Desplegando Service Discovery (Eureka)..."
                    sh 'kubectl apply -f service-discovery-deployment.yaml'
                    sh 'kubectl wait --for=condition=available --timeout=300s deployment/service-discovery'

                    // 3. Config Server
                    echo "Desplegando Cloud Config Server..."
                    sh 'kubectl apply -f cloud-config-deployment.yaml'
                    sh 'kubectl wait --for=condition=available --timeout=300s deployment/cloud-config'

                    // 4. Resto de servicios (primero aplicar todos)
                    def microservices = [
                        "api-gateway", // API Gateway se aplica aquí pero se espera al final
                        "user-service",
                        "product-service",
                        "order-service",
                        "payment-service",
                        "shipping-service",
                        "favourite-service",
                        "proxy-client"
                    ]

                    microservices.each { serviceName ->
                        echo "Desplegando ${serviceName}..."
                        sh "kubectl apply -f ${serviceName}-deployment.yaml"
                    }

                    // Esperar a que los microservicios (excepto api-gateway) estén listos
                    def servicesToWaitFor = [
                        "user-service",
                        "product-service",
                        "order-service",
                        "payment-service",
                        "shipping-service",
                        "favourite-service",
                        "proxy-client"
                    ]

                    servicesToWaitFor.each { serviceName ->
                        echo "Esperando a que el deployment ${serviceName} esté disponible..."
                        sh "kubectl wait --for=condition=available --timeout=600s deployment/${serviceName}"
                    }
                    
                    // Esperar específicamente por el api-gateway al final
                    echo "Esperando a que el deployment api-gateway esté disponible..."
                    sh 'kubectl wait --for=condition=available --timeout=600s deployment/api-gateway'

                    // 5. Ingress
                    echo "Desplegando Ingress..."
                    sh 'kubectl apply -f ecommerce-ingress.yaml'
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                // Verificar el estado del despliegue
                echo "Esperando a que los servicios estén listos..."
                sleep(time: 30, unit: 'SECONDS')
                sh 'kubectl get pods'
                sh 'kubectl get services'
            }
        }

        stage('Service Access URLs') {
            steps {
                script {
                    // Obtener URLs de acceso y mostrarlas
                    echo "Generando URLs de acceso a los servicios..."
                    
                    def apiGatewayUrl = sh(script: 'minikube service api-gateway --url', returnStdout: true).trim()
                    def zipkinUrl = sh(script: 'minikube service zipkin --url', returnStdout: true).trim()
                    def eurekaUrl = sh(script: 'minikube service service-discovery --url', returnStdout: true).trim()
                    
                    echo "URLs de acceso a los servicios:"
                    echo "API Gateway: ${apiGatewayUrl}"
                    echo "Zipkin: ${zipkinUrl}"
                    echo "Eureka Service Discovery: ${eurekaUrl}"
                }
            }
        }
    }

    post {
        success {
            // Acciones posteriores al despliegue exitoso
            echo "Despliegue completado exitosamente!"
            echo "Para acceder a los servicios, use las URLs mostradas en la etapa 'Service Access URLs'"
        }
        failure {
            // Acciones en caso de error en el despliegue
            echo "El despliegue ha fallado. Revise los logs para más detalles."
        }
        always {
            // Limpiar recursos o enviar notificaciones
            echo "Proceso de despliegue finalizado."
        }
    }
}
