pipeline {
    agent any

    environment {
        // La máquina remota debe tener instalado SSHpass, SSH, Docker y Minikube
        REMOTE_USER_ID        = 'ssh-username' // Puede ser tu propio username
        REMOTE_HOST_ID        = 'ssh-hostname' // Puede ser tu propio hostname
        SSH_PASSWORD_ID    = 'ssh-password' // Puede ser tu propia contraseña
        PROFILE_CREDENTIAL_ID = 'profile' // ID for the new credential
    }

    stages {

        stage('Clone - Build - Generate Docker Images') {
            steps {
                // Obtener credenciales de Jenkins
                withCredentials([
                        string(credentialsId: env.REMOTE_USER_ID,     variable: 'REMOTE_USER'),
                        string(credentialsId: env.REMOTE_HOST_ID,     variable: 'REMOTE_HOST'),
                        string(credentialsId: env.SSH_PASSWORD_ID,    variable: 'SSH_PASSWORD'),
                        string(credentialsId: env.PROFILE_CREDENTIAL_ID, variable: 'PROFILE')
                    ]) {
                    script {
                        def baseCmd = "sshpass -p \"${SSH_PASSWORD}\" ssh -o StrictHostKeyChecking=no ${env.REMOTE_USER}@${env.REMOTE_HOST}"
                        // Verificar si el directorio ya existe, si exite pull si no existe clone
                        sh "${baseCmd} 'if [ -d \"ecommerce-microservice-backend-app\" ]; then echo Repository already exists! && cd ecommerce-microservice-backend-app && git pull; else echo Cloning repository... && git clone https://github.com/AndresCamiloRR/ecommerce-microservice-backend-app.git && cd ecommerce-microservice-backend-app && echo Repository cloned!; fi'"

                        // Buildear y generar las imágenes de Docker
                        sh "${baseCmd} '.\\mvnw clean package -DskipTests && echo Project built successfully! && docker compose build && echo Docker images generated successfully!'"
                    }
                }
            }
        }

        stage('Unit Tests') {
            steps {
                // Obtener credenciales de Jenkins
                // Obtener credenciales de Jenkins
                withCredentials([
                        string(credentialsId: env.REMOTE_USER_ID,     variable: 'REMOTE_USER'),
                        string(credentialsId: env.REMOTE_HOST_ID,     variable: 'REMOTE_HOST'),
                        string(credentialsId: env.SSH_PASSWORD_ID,    variable: 'SSH_PASSWORD')
                    ]) {
                    script {
                        def baseCmd = "sshpass -p \"${SSH_PASSWORD}\" ssh -o StrictHostKeyChecking=no ${env.REMOTE_USER}@${env.REMOTE_HOST}"
                        sh "${baseCmd} 'cd ecommerce-microservice-backend-app
                            && echo Running unit tests... && \\
                            .\\mvnw test -DskipTests=false && echo Unit tests completed successfully!'"
                    }
                }
            }
        }

        stage('Deploy Core Services') {
            steps {
                // Obtener credenciales de Jenkins
                withCredentials([
                        string(credentialsId: env.REMOTE_USER_ID,     variable: 'REMOTE_USER'),
                        string(credentialsId: env.REMOTE_HOST_ID,     variable: 'REMOTE_HOST'),
                        string(credentialsId: env.SSH_PASSWORD_ID,    variable: 'SSH_PASSWORD'),
                        string(credentialsId: env.PROFILE_CREDENTIAL_ID, variable: 'PROFILE') // Get profile from credentials
                    ]) {
                    script {
                        //  Definir el comando base para SSH
                        def baseCmd = "sshpass -p \"${SSH_PASSWORD}\" ssh -o StrictHostKeyChecking=no ${env.REMOTE_USER}@${env.REMOTE_HOST}"
                        sh "${baseCmd} 'echo Starting deployment of core services for profile: ${PROFILE}...'"

                        // Montar minikube
                        sh "${baseCmd} 'echo Mounting minikube... && \\
                            minikube start --cpus=no-limit --memory=no-limit'"

                        // Ir al directorio de Kubernetes
                        sh "${baseCmd} 'cd ecommerce-microservice-backend-app/k8s && echo Changing directory to Kubernetes folder...'"

                        // Desplegar servicios de Zipkin, Service Discovery y Cloud Config
                        sh """
                            ${baseCmd} 'echo Deploying Zipkin... && \\
                            kubectl apply -f zipkin-deployment.yaml && \\
                            echo Waiting for Zipkin to be ready... && \\
                            kubectl wait --for=condition=ready pod -l app=zipkin --timeout=60s'
                        """
                        sh """
                            ${baseCmd} 'export PROFILE=${PROFILE} && echo Deploying Service Discovery... && \\
                            envsubst < service-discovery-deployment.yaml | kubectl apply -f - && \\
                            echo Waiting for Service Discovery to be ready... && \\
                            kubectl wait --for=condition=ready pod -l app=service-discovery --timeout=200s'
                        """
                        sh """
                            ${baseCmd} 'export PROFILE=${PROFILE} && echo Deploying Cloud Config... && \\
                            envsubst < cloud-config-deployment.yaml | kubectl apply -f - && \\
                            echo Waiting for Cloud Config to be ready... && \\
                            kubectl wait --for=condition=ready pod -l app=cloud-config --timeout=200s'
                        """
                    }
                }
            }
        }

        stage('Deploy Remaining Services') {
            steps {
                withCredentials([
                        string(credentialsId: env.REMOTE_USER_ID,     variable: 'REMOTE_USER'),
                        string(credentialsId: env.REMOTE_HOST_ID,     variable: 'REMOTE_HOST'),
                        string(credentialsId: env.SSH_PASSWORD_ID,    variable: 'SSH_PASSWORD'),
                        string(credentialsId: env.PROFILE_CREDENTIAL_ID, variable: 'PROFILE') // Get profile from credentials
                    ]) {
                    script {
                        def baseCmd = "sshpass -p \\"${SSH_PASSWORD}\\" ssh -o StrictHostKeyChecking=no ${env.REMOTE_USER}@${env.REMOTE_HOST}"
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

                        sh "${baseCmd} 'echo Core services are now ready. Deploying remaining services for profile: ${PROFILE}...'"

                        // Ir al directorio de Kubernetes
                        sh "${baseCmd} 'cd ecommerce-microservice-backend-app/k8s && echo Changing directory to Kubernetes folder...'"

                        for (svc in services) {
                            sh "${baseCmd} 'export PROFILE=${PROFILE} && envsubst < ${svc} | kubectl apply -f -'"
                        }
                        sh "${baseCmd} 'echo All services have been deployed for profile: ${PROFILE}! && echo You can monitor the status using: kubectl get pods -w'"
                    }
                }
            }
        }

        stage('Integration, E2E and Stress Tests') {
            steps {
                script {
                    // Si el perfil no es "dev", entonces se ejecutan los tests
                    if (env.PROFILE != 'dev') {
                        echo "Running tests for profile: ${env.PROFILE}..."
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
            // Destruir minikube
            script {
                def baseCmd = "sshpass -p \\"${SSH_PASSWORD}\\" ssh -o StrictHostKeyChecking=no ${env.REMOTE_USER}@${env.REMOTE_HOST}"
                sh "${baseCmd} 'echo Destroying minikube... && minikube delete --all --purge'"
            }
        }
        success {
            echo 'Deployment completed successfully.'
        }
    }
}
