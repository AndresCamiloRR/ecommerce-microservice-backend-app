pipeline {
    agent any

    environment {
        REMOTE_USER        = 'ssh-username'
        REMOTE_HOST        = 'ssh-hostname'
        SSH_PASSWORD_ID    = 'ssh-password'
        K8S_DIR            = 'ssh-k8s'
    }

    stages {
        stage('Deploy Core Services') {
            steps {
                // Obtener credenciales de Jenkins
                withCredentials([
                        string(credentialsId: env.REMOTE_USER_ID,     variable: 'REMOTE_USER'),
                        string(credentialsId: env.REMOTE_HOST_ID,     variable: 'REMOTE_HOST'),
                        string(credentialsId: env.SSH_PASSWORD_ID,    variable: 'SSH_PASSWORD'),
                        string(credentialsId: env.K8S_DIR_ID,         variable: 'K8S_DIR')
                    ]) {
                    script {
                        //  Definir el comando base para SSH
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
                            kubectl wait --for=condition=ready pod -l app=service-discovery --timeout=200s'
                        """
                        sh """
                            ${baseCmd} 'cd ${env.K8S_DIR} && \
                            echo Deploying Cloud Config... && \
                            kubectl apply -f cloud-config-deployment.yaml && \
                            echo Waiting for Cloud Config to be ready... && \
                            kubectl wait --for=condition=ready pod -l app=cloud-config --timeout=200s'
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
