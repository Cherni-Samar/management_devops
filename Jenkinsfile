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
        SONAR_URL = "http://127.0.0.1:30900" // NodePort exposé de SonarQube
    }

    tools {
        maven 'Maven'
        jdk 'jdk'
    }

    stages {

     stage('CHECK MINIKUBE') {
         steps {
             script {
                 sh '''
                 if ! minikube status > /dev/null 2>&1; then
                     echo "❌ Minikube n'est pas démarré. Veuillez démarrer Minikube avant le pipeline."
                     exit 1
                 else
                     echo "✅ Minikube est démarré."
                 fi
                 '''
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
                echo "🔍 Analyse SonarQube via NodePort Minikube..."
                script {
                    // NodePort de SonarQube
                    def sonarNodePort = sh(
                        script: "kubectl get svc sonarqube-service -n devops -o jsonpath='{.spec.ports[0].nodePort}'",
                        returnStdout: true
                    ).trim()

                    def sonarUrl = "http://127.0.0.1:${sonarNodePort}"
                    echo "Sonar running at: ${sonarUrl}"

                    // Attendre que Sonar soit UP
                    timeout(time: 5, unit: 'MINUTES') {
                        waitUntil {
                            def status = sh(
                                script: "curl -s ${sonarUrl}/api/system/status || echo DOWN",
                                returnStdout: true
                            ).trim()
                            echo "⏳ Waiting for SonarQube... Status: ${status}"
                            return status.contains("UP")
                        }
                    }

                    // Analyse Maven
                    sh """
                        mvn sonar:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                          -Dsonar.projectName='Management DevOps' \
                          -Dsonar.host.url=${sonarUrl} \
                          -Dsonar.login=${SONAR_LOGIN} \
                          -Dsonar.password=${SONAR_PASSWORD} \
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
                sh '''
                    export KUBECONFIG=/var/lib/jenkins/.kube/config
                    kubectl config current-context
                    kubectl get nodes

                    kubectl apply -f /var/lib/jenkins/workspace/pipeline-testProjectDevops/k8s-manifests/mysql-deployment.yaml -n devops
                    kubectl apply -f /var/lib/jenkins/workspace/pipeline-testProjectDevops/k8s-manifests/spring-deployment.yaml -n devops

                    kubectl get pods -n devops
                    kubectl get svc -n devops
                '''
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
            echo "🔍 SonarQube: ${SONAR_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
        }
        failure {
            echo "❌ LE PIPELINE A ÉCHOUÉ!"
        }
        always {
            sh "docker system prune -f || true"
        }
    }
}
