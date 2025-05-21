pipeline {
    agent {
        docker {
            // org.apache.maven.plugins:maven-compiler-plugin:3.8.1:compile
            image 'maven:3.8.1-jdk-11-slim'
            args '-u root' // Run as root to install packages
        }
    }

    environment {
        REMOTE_USER_ID          = 'ssh-username'
        REMOTE_HOST_ID          = 'ssh-hostname'
        SSH_PASSWORD_ID         = 'ssh-password'
        PROFILE_CREDENTIAL_ID   = 'profile'
    }

    stages {
        stage('Prepare Environment') {
            steps {
                sh 'apt-get update && apt-get install -y git sshpass'
            }
        }

        stage('Clone') {
            steps {
                withCredentials([
                    string(credentialsId: "${REMOTE_USER_ID}", variable: 'REMOTE_USER'),
                    string(credentialsId: "${REMOTE_HOST_ID}", variable: 'REMOTE_HOST'),
                    string(credentialsId: "${SSH_PASSWORD_ID}", variable: 'SSH_PASSWORD'),
                    string(credentialsId: "${PROFILE_CREDENTIAL_ID}", variable: 'PROFILE')
                ]) {
                    script {
                        def baseCmd = "sshpass -p '${SSH_PASSWORD}' ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} bash -c"

                        sh """
                            if [ -d "ecommerce-microservice-backend-app" ]; then \
                                echo Repository already exists! && \
                                cd ecommerce-microservice-backend-app && \
                                git pull; \
                            else \
                                echo Cloning repository... && \
                                git clone https://github.com/AndresCamiloRR/ecommerce-microservice-backend-app.git && \
                                cd ecommerce-microservice-backend-app && \
                                echo Repository cloned!;
                        """
                    }
                }
            }
        }

        stage('Unit Tests') {
            steps {
                withCredentials([
                    string(credentialsId: "${REMOTE_USER_ID}", variable: 'REMOTE_USER'),
                    string(credentialsId: "${REMOTE_HOST_ID}", variable: 'REMOTE_HOST'),
                    string(credentialsId: "${SSH_PASSWORD_ID}", variable: 'SSH_PASSWORD')
                ]) {
                    script {
                        def baseCmd = "sshpass -p '${SSH_PASSWORD}' ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} bash -c"
                        sh """
                            ${baseCmd} "cd ecommerce-microservice-backend-app && \\
                                echo Running unit tests...
                        """
                    }
                }
            }
        }

        stage('Deploy Core Services') {
            steps {
                withCredentials([
                    string(credentialsId: "${REMOTE_USER_ID}", variable: 'REMOTE_USER'),
                    string(credentialsId: "${REMOTE_HOST_ID}", variable: 'REMOTE_HOST'),
                    string(credentialsId: "${SSH_PASSWORD_ID}", variable: 'SSH_PASSWORD'),
                    string(credentialsId: "${PROFILE_CREDENTIAL_ID}", variable: 'PROFILE')
                ]) {
                    script {
                        def baseCmd = "sshpass -p '${SSH_PASSWORD}' ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} bash -c"

                        sh "${baseCmd} 'echo Starting deployment of core services for profile: ${PROFILE}...'"

                        sh "${baseCmd} 'minikube start --cpus=2 --memory=4096'"

                        def k8sDir = "ecommerce-microservice-backend-app/k8s"

                        sh "${baseCmd} 'cd ${k8sDir} && echo Deploying Zipkin... && kubectl apply -f zipkin-deployment.yaml && kubectl wait --for=condition=ready pod -l app=zipkin --timeout=60s'"

                        sh "${baseCmd} 'cd ${k8sDir} && export PROFILE=${PROFILE} && envsubst < service-discovery-deployment.yaml | kubectl apply -f - && kubectl wait --for=condition=ready pod -l app=service-discovery --timeout=200s'"

                        sh "${baseCmd} 'cd ${k8sDir} && export PROFILE=${PROFILE} && envsubst < cloud-config-deployment.yaml | kubectl apply -f - && kubectl wait --for=condition=ready pod -l app=cloud-config --timeout=200s'"
                    }
                }
            }
        }

        stage('Deploy Remaining Services') {
            steps {
                withCredentials([
                    string(credentialsId: "${REMOTE_USER_ID}", variable: 'REMOTE_USER'),
                    string(credentialsId: "${REMOTE_HOST_ID}", variable: 'REMOTE_HOST'),
                    string(credentialsId: "${SSH_PASSWORD_ID}", variable: 'SSH_PASSWORD'),
                    string(credentialsId: "${PROFILE_CREDENTIAL_ID}", variable: 'PROFILE')
                ]) {
                    script {
                        def baseCmd = "sshpass -p '${SSH_PASSWORD}' ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} bash -c"
                        def k8sDir = "ecommerce-microservice-backend-app/k8s"
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

                        sh "${baseCmd} 'echo Core services are now ready. Deploying remaining services for profile: ${PROFILE}... && cd ${k8sDir}'"

                        for (svc in services) {
                            sh "${baseCmd} 'cd ${k8sDir} && export PROFILE=${PROFILE} && envsubst < ${svc} | kubectl apply -f -'"
                        }

                        sh "${baseCmd} 'echo All services have been deployed for profile: ${PROFILE}! && kubectl get pods -w'"
                    }
                }
            }
        }

        stage('Integration, E2E and Stress Tests') {
            steps {
                script {
                    if (env.PROFILE != 'dev') {
                        echo "Running tests for profile: ${env.PROFILE}..."
                        // Aquí puedes insertar ejecución de pruebas reales
                    } else {
                        echo "Skipping tests for profile: ${env.PROFILE}..."
                    }
                }
            }
        }
    }

    post {
        failure {
            echo 'Deployment failed. Check console output for errors.'
            withCredentials([
                string(credentialsId: "${REMOTE_USER_ID}", variable: 'REMOTE_USER'),
                string(credentialsId: "${REMOTE_HOST_ID}", variable: 'REMOTE_HOST'),
                string(credentialsId: "${SSH_PASSWORD_ID}", variable: 'SSH_PASSWORD')
            ]) {
                script {
                    def baseCmd = "sshpass -p '${SSH_PASSWORD}' ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST}"
                    sh "${baseCmd} 'echo Destroying minikube... && minikube delete --all --purge'"
                }
            }
        }

        success {
            echo 'Deployment completed successfully.'
        }
    }
}
