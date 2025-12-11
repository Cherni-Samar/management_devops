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
        SONAR_URL = "http://${minikubeIp}:30900"
    }

    tools {
        maven 'Maven'
        jdk 'jdk'
    }

    stages {

     stage('CHECK MINIKUBE') {
              steps {
                  script {
                      // Check the actual status of the Minikube host
                      def status = sh(script: 'minikube status -f "{{.Host}}" || echo "Stopped"', returnStdout: true).trim()

                      if (status == "Running") {
                          echo "✅ Minikube est déjà démarré."
                      } else {
                          echo "🚀 Minikube n'est pas démarré (Statut: ${status}). Démarrage en cours..."
                          // Use the appropriate driver for your environment (e.g., --driver=docker)
                          sh 'minikube start --driver=docker'

                          // Wait for the API server to be reachable
                          sh 'kubectl cluster-info || true'
                          echo "✅ Minikube a été démarré avec succès."
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
                        echo "🔍 Analyse SonarQube via NodePort Minikube..."

                        script {
                            // 1. Get the Minikube IP
                            def minikubeIp = sh(
                                script: 'minikube ip',
                                returnStdout: true
                            ).trim()

                            def sonarNodePort = "30900"
                            def actualSonarUrl = "http://${minikubeIp}:${sonarNodePort}"

                            echo "SonarQube est accessible à l'URL : ${actualSonarUrl}"

                            // 2. Wait for SonarQube to be UP using the correct IP
                            timeout(time: 5, unit: 'MINUTES') {
                                waitUntil {
                                    def status = sh(
                                        script: "curl -s ${actualSonarUrl}/api/system/status || echo DOWN",
                                        returnStdout: true
                                    ).trim()
                                    echo "⏳ Waiting for SonarQube... Status: ${status}"
                                    return status.contains('UP')
                                }
                            }

                            // 3. Launch the Sonar scanner
                            sh """
                                mvn sonar:sonar \\
                                  -Dsonar.projectKey=${SONAR_PROJECT_KEY} \\
                                  -Dsonar.projectName='Management DevOps' \\
                                  -Dsonar.host.url=${actualSonarUrl} \\ // Use the dynamic IP
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
