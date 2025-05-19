pipeline {
    agent any

    environment {
        REMOTE_USER        = 'andre'
        REMOTE_HOST        = '192.168.56.1'
        SSH_PASSWORD_ID    = 'ssh-password-andre'
        K8S_DIR            = 'C:\\Cositas\\ecommerce-microservice-backend-app\\k8s'
    }

    stages {
        stage('Deploy Core Services') {
            steps {
                // Use SSH password (Secret Text)
                withCredentials([string(credentialsId: env.SSH_PASSWORD_ID, variable: 'SSH_PASSWORD')]) {
                    script {
                        // Usamos sshpass con la contraseña
                        def baseCmd = "sshpass -p \"${SSH_PASSWORD}\" ssh -o StrictHostKeyChecking=no ${env.REMOTE_USER}@${env.REMOTE_HOST}"
                        sh "${baseCmd} 'echo Starting deployment of core services...'"
                        sh """
                            ${baseCmd} 'cd ${env.K8S_DIR} && \
                            echo Deploying Zipkin... && \
                            kubectl apply -f zipkin-deployment.yaml && \
                            echo Waiting for Zipkin to be ready... && \
                            kubectl wait --for=condition=ready pod -l app=zipkin --timeout=60s'
                        """
                        sh """
                            ${baseCmd} 'cd ${env.K8S_DIR} && \
                            echo Deploying Service Discovery... && \
                            kubectl apply -f service-discovery-deployment.yaml && \
                            echo Waiting for Service Discovery to be ready... && \
                            kubectl wait --for=condition=ready pod -l app=service-discovery --timeout=180s'
                        """
                        sh """
                            ${baseCmd} 'cd ${env.K8S_DIR} && \
                            echo Deploying Cloud Config... && \
                            kubectl apply -f cloud-config-deployment.yaml && \
                            echo Waiting for Cloud Config to be ready... && \
                            kubectl wait --for=condition=ready pod -l app=cloud-config --timeout=180s'
                        """
                    }
                }
            }
        }

        stage('Deploy Remaining Services') {
            steps {
                withCredentials([string(credentialsId: env.SSH_PASSWORD_ID, variable: 'SSH_PASSWORD')]) {
                    script {
                        def baseCmd = "sshpass -p \"${SSH_PASSWORD}\" ssh -o StrictHostKeyChecking=no ${env.REMOTE_USER}@${env.REMOTE_HOST}"
                        def services = [
                            'api-gateway-deployment.yaml',
                            'favourite-service-deployment.yaml',
                            'order-service-deployment.yaml',
                            'payment-service-deployment.yaml',
                            'product-service-deployment.yaml',
                            'proxy-client-deployment.yaml',
                            'shipping-service-deployment.yaml',
                            'user-service-deployment.yaml'
                        ]

                        sh "${baseCmd} 'echo Core services are now ready. Deploying remaining services...'"
                        for (svc in services) {
                            sh "${baseCmd} 'cd ${env.K8S_DIR} && kubectl apply -f ${svc}'"
                        }
                        sh "${baseCmd} 'cd ${env.K8S_DIR} && kubectl apply -f ecommerce-ingress.yaml'"
                        sh "${baseCmd} 'echo All services have been deployed! && echo You can monitor the status using: kubectl get pods -w'"
                    }
                }
            }
        }
    }

    post {
        failure {
            echo 'Deployment failed. Check console output for errors.'
        }
        success {
            echo 'Deployment completed successfully.'
        }
    }
}
