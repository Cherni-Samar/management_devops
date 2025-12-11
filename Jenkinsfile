pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "chernisamar/myapp"
        DOCKER_TAG = "1.0.0"
        GIT_REPO = "https://github.com/Cherni-Samar/management_devops.git"
        GIT_BRANCH = "main"
        SONAR_PROJECT_KEY = "management_devops"
        SONAR_LOGIN = "admin"
        SONAR_PASSWORD = "sonar"
        KUBECONFIG = "/var/lib/jenkins/.kube/config"
        }

    tools {
        maven 'Maven'
        jdk 'jdk'
    }

    stages {

        stage('CHECK KUBERNETES') {
            steps {
                script {
                    try {
                        sh "kubectl --kubeconfig=/var/lib/jenkins/.kube/config get nodes"
                        echo "✅ Kubernetes accessible via kubeconfig"
                    } catch (err) {
                        error "❌ Kubernetes non accessible. Assurez-vous que Minikube est démarré."
                    }
                }
            }
        }


        stage('RÉCUPÉRATION CODE') {
            steps {
                echo "📥 Récupération du code depuis GitHub..."
                git branch: "${GIT_BRANCH}", url: "${GIT_REPO}"
                sh "git log -1 --oneline"
                sh "ls -la"
            }
        }

        stage('TESTS UNITAIRES') {
            steps {
                echo "🧪 Exécution des tests..."
                sh "mvn test"
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('LIVRABLE') {
            steps {
                echo "📦 Création du livrable (JAR)..."
                sh "mvn package -DskipTests"
            }
        }

        stage('ANALYSE SONARQUBE') {
            steps {
                echo "🔍 Analyse SonarQube via le Pod Kubernetes..."
                script {
                    sh 'kubectl --kubeconfig=$KUBECONFIG -n devops port-forward svc/sonarqube-service 9000:9000 &'
                    sleep 10
                    timeout(time: 5, unit: 'MINUTES') {
                        waitUntil {
                            def status = sh(script: "curl -s http://127.0.0.1:9000/api/system/status || echo DOWN", returnStdout: true).trim()
                            echo "⏳ Waiting for SonarQube... Status: ${status}"
                            return status.contains('UP')
                        }
                    }
                    sh """
                        mvn sonar:sonar \\
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \\
                          -Dsonar.projectName='Management DevOps' \\
                          -Dsonar.host.url=http://127.0.0.1:9000 \\
                          -Dsonar.login=${SONAR_LOGIN} \\
                          -Dsonar.password=${SONAR_PASSWORD} \\
                          -Dsonar.java.binaries=target/classes
                    """
                }
            }
        }

        stage('BUILD DOCKER') {
            steps {
                sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
            }
        }

        stage('PUSH DOCKERHUB') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                    sh "echo \$PASS | docker login -u \$USER --password-stdin"
                    sh "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                }
            }
        }

        stage('DEPLOY SUR KUBERNETES') {
            steps {
                echo "☸️ Déploiement sur Kubernetes..."
                sh """
                    export KUBECONFIG=/var/lib/jenkins/.kube/config
                    kubectl config current-context
                    kubectl get nodes

                    # Appliquer les manifests
                    kubectl apply -f ${WORKSPACE}/k8s-manifests/mysql-deployment.yaml -n devops
                    kubectl apply -f ${WORKSPACE}/k8s-manifests/spring-deployment.yaml -n devops
                    kubectl apply -f ${WORKSPACE}/k8s-manifests/sonarqube-deployment.yaml -n devops

                    # Attendre que les pods soient prêts
                    kubectl wait --for=condition=Ready pod -l app=spring-app -n devops --timeout=180s
                    kubectl wait --for=condition=Ready pod -l app=sonarqube -n devops --timeout=180s

                    # Récupérer les URL via minikube
                    SPRING_URL=\$(minikube service spring-service -n devops --url)
                    SONAR_URL=\$(minikube service sonarqube-service -n devops --url)

                    echo "✅ Spring Boot URL: \$SPRING_URL"
                    echo "✅ SonarQube URL: \$SONAR_URL"
                """
            }
        }

    }

    post {
        success {
            echo "============================================"
            echo "✅ PIPELINE TERMINÉ AVEC SUCCÈS!"
            echo "============================================"
            echo ""
            echo "📦 Image Docker: ${DOCKER_IMAGE}:${DOCKER_TAG}"
            echo "🔗 DockerHub: https://hub.docker.com/r/chernisamar/myapp"
            echo "📂 GitHub: ${GIT_REPO}"
            echo "🔍 SonarQube: http://127.0.0.1:9000/dashboard?id=${SONAR_PROJECT_KEY}"
        }
        failure {
            echo "❌ LE PIPELINE A ÉCHOUÉ!"
        }
        always {
            sh "docker system prune -f || true"
        }
    }
}
