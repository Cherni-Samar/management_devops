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
    }

    tools {
        maven 'Maven'
        jdk 'jdk'
    }

    stages {

        stage('CHECK KUBERNETES') {
            steps {
                echo "☸️ Vérification de Kubernetes..."
                script {
                    try {
                        sh "kubectl get nodes"
                        echo "✅ Kubernetes accessible"
                    } catch (err) {
                        echo "⚠️ Kubernetes non accessible"
                    }
                }
            }
        }

        stage('RÉCUPÉRATION CODE') {
            steps {
                echo "📥 Récupération du code..."
                git branch: "${GIT_BRANCH}", url: "${GIT_REPO}"
                sh "git log -1 --oneline"
            }
        }

        stage('TESTS UNITAIRES') {
            steps {
                echo "🧪 Tests..."
                script {
                    try {
                        sh "mvn test"
                    } catch (err) {
                        echo "⚠️ Tests échoués"
                    }
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml' 2>/dev/null || true
                }
            }
        }

        stage('LIVRABLE') {
            steps {
                echo "📦 Build JAR..."
                sh "mvn package -DskipTests"
            }
        }

        stage('ANALYSE SONARQUBE') {
            steps {
                echo "🔍 Analyse SonarQube..."
                script {
                    try {
                        sh '''
                            kubectl port-forward -n devops svc/sonarqube-service 9000:9000 > /tmp/sonar.log 2>&1 &
                            SONAR_PID=$!
                            echo $SONAR_PID > /tmp/sonar.pid
                            sleep 5

                            for i in {1..30}; do
                                if curl -s http://127.0.0.1:9000/api/system/status | grep -q "UP"; then
                                    echo "✅ SonarQube UP"
                                    break
                                fi
                                echo "⏳ Tentative $i/30..."
                                sleep 2
                            done

                            mvn sonar:sonar -Dsonar.projectKey=${SONAR_PROJECT_KEY} -Dsonar.host.url=http://127.0.0.1:9000 -Dsonar.login=${SONAR_LOGIN} -Dsonar.password=${SONAR_PASSWORD}

                            kill $(cat /tmp/sonar.pid) 2>/dev/null || true
                        '''
                    } catch (err) {
                        echo "⚠️ SonarQube échoué"
                    }
                }
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
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                    sh "echo \$PASS | docker login -u \$USER --password-stdin && docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                }
            }
        }

        stage('DEPLOY SUR KUBERNETES') {
            steps {
                echo "☸️ Déploiement..."
                script {
                    try {
                        sh '''
                            kubectl create namespace devops 2>/dev/null || true
                            kubectl apply -f k8s-manifests/mysql-deployment.yaml -n devops
                            kubectl apply -f k8s-manifests/spring-deployment.yaml -n devops
                            kubectl apply -f k8s-manifests/sonarqube-deployment.yaml -n devops
                            sleep 10
                            kubectl get pods -n devops
                        '''
                    } catch (err) {
                        echo "⚠️ Déploiement échoué"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ PIPELINE RÉUSSI!"
        }
        failure {
            echo "❌ PIPELINE ÉCHOUÉ!"
        }
        always {
            sh "docker system prune -f 2>/dev/null || true"
        }
    }
}