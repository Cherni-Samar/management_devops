pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "chernisamar/myapp"
        DOCKER_TAG = "1.0.0"
        GIT_REPO = "https://github.com/Cherni-Samar/management_devops.git"
        GIT_BRANCH = "main"
        SONAR_PROJECT_KEY = "management_devops"
    }

    tools {
        maven 'Maven'
        jdk 'jdk'
    }

    stages {

        stage('RÉCUPÉRATION CODE') {
            steps {
                echo "📥 Récupération du code depuis GitHub..."
                echo "Repository: ${GIT_REPO}"
                echo "Branch: ${GIT_BRANCH}"

                git branch: "${GIT_BRANCH}",
                    url: "${GIT_REPO}"

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

        stage('MVN SONARQUBE TEST') {
            steps {
                echo "============================================"
                echo "🔍 Test stage SonarQube"
                echo "SONAR_PROJECT_KEY = management-devops"
                echo "============================================"

                sh """
                    mvn sonar:sonar \
                      -Dsonar.projectKey=management-devops \
                      -Dsonar.host.url=http://localhost:9000 \
                      -Dsonar.login=admin \
                      -Dsonar.password=sonar
                """
            }
        }

        stage('LIVRABLE') {
            steps {
                echo "📦 Création du livrable (JAR)..."
                sh "mvn package -DskipTests"
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
                     cd ${WORKSPACE}/k8s-manifests
                     kubectl apply -f mysql-deployment.yaml -n devops --validate=false
                     kubectl apply -f spring-deployment.yaml -n devops --validate=false
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
            echo "🔗 DockerHub: https://hub.docker. com/r/chernisamar/myapp"
            echo "📂 GitHub: ${GIT_REPO}"
            echo "🔍 SonarQube: http://localhost:9000/dashboard?id=${SONAR_PROJECT_KEY}"
            echo ""
        }
        failure {
            echo "============================================"
            echo "❌ LE PIPELINE A ÉCHOUÉ!"
            echo "============================================"
        }
        always {
            echo "🧹 Nettoyage..."
            sh "docker system prune -f || true"
        }
    }
}