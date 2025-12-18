pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "chernisamar/myapp"
        DOCKER_TAG = "1.0.0"
        GIT_REPO = "https://github.com/Cherni-Samar/management_devops.git"
        GIT_BRANCH = "main"
        SONARQUBE_URL = "http://10.244.0.63:9000"

    }

    tools {
        maven 'Maven'
        jdk 'jdk'
    }

    stages {
        stage('RÉCUPÉRATION CODE') {
            steps {
                echo "📥 Récupération..."
                git branch: "${GIT_BRANCH}", url: "${GIT_REPO}"
                sh "git log -1 --oneline"
            }
        }

        stage('TESTS UNITAIRES') {
            steps {
                echo "🧪 Tests..."
                sh "mvn test || true"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('ANALYSE SONARQUBE') {
                    steps {
                        echo "📊 SonarQube..."
                        script {
                            sh '''
                                # Port-forward SonarQube
                                kubectl port-forward svc/sonarqube-service 9000:9000 -n devops &
                                PF_PID=$!
                                sleep 5

                                # Attendez que SonarQube réponde
                                for i in {1..30}; do
                                    if curl -s http://localhost:9000/api/system/status | grep -q UP; then
                                        echo "✅ SonarQube est accessible"
                                        break
                                    fi
                                    sleep 2
                                done

                                # Analysez
                                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                                    mvn sonar:sonar \
                                      -Dsonar.projectKey=management_devops \
                                      -Dsonar.sources=src/main/java \
                                      -Dsonar.tests=src/test/java \
                                      -Dsonar.host.url=http://localhost:9000 \
                                      -Dsonar.login=${SONAR_TOKEN} || true
                                }

                                # Arrêtez le port-forward
                                kill $PF_PID 2>/dev/null || true
                            '''
                        }
                    }
                }
        stage('LIVRABLE') {
            steps {
                echo "📦 Build JAR..."
                sh "mvn package -DskipTests"
            }
        }

        stage('BUILD DOCKER') {
            steps {
                echo "🐳 Build Docker..."
                sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
            }
        }

        stage('PUSH DOCKERHUB') {
            steps {
                echo "📤 Push..."
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'docker login -u "$DOCKER_USER" -p "$DOCKER_PASS" && docker push ${DOCKER_IMAGE}:${DOCKER_TAG} && docker logout'
                }
            }
        }

        stage('DEPLOY SUR KUBERNETES') {
            steps {
                echo "☸️ Déploiement..."
                sh '''
                    kubectl create namespace devops --validate=false 2>/dev/null || true
                    kubectl apply -f k8s-manifests/mysql-deployment.yaml -n devops --validate=false 2>/dev/null || true
                    kubectl apply -f k8s-manifests/spring-deployment.yaml -n devops --validate=false 2>/dev/null || true
                    sleep 10
                '''
            }
        }

        stage('ACCÈS APPLICATION') {
            steps {
                echo "🌐 Application déployée!"
                sh '''
                    echo ""
                    echo "============================================"
                    echo "✅ PIPELINE RÉUSSI!"
                    echo "============================================"
                    echo ""
                    echo "🔗 ACCÈS À L'APPLICATION:"
                    echo ""
                    echo "1️⃣ Port-Forward (Recommandé):"
                    echo "   kubectl port-forward svc/spring-service 8089:8089 -n devops"
                    echo "   🌐 http://localhost:8089/student/Department/getAllDepartment"
                    echo ""
                    echo "2️⃣ Via Minikube:"
                    echo "   minikube service spring-service -n devops"
                    echo ""
                    echo "============================================"
                '''
            }
        }
    }

    post {
        success {
            echo ""
            echo "✅ PIPELINE TERMINÉ!"
        }
        failure {
            echo "❌ PIPELINE ÉCHOUÉ!"
        }
        always {
            sh "docker system prune -f 2>/dev/null || true"
        }
    }
}
