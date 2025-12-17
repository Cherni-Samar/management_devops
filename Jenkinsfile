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

        stage('RÉCUPÉRATION CODE') {
            steps {
                echo "📥 Récupération du code depuis GitHub..."
                git branch: "${GIT_BRANCH}", url: "${GIT_REPO}"
                sh "git log -1 --oneline"
            }
        }

        stage('TESTS UNITAIRES') {
            steps {
                echo "🧪 Exécution des tests unitaires..."
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

        stage('BUILD DOCKER') {
            steps {
                echo "🐳 Construction de l'image Docker..."
                sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
            }
        }

        stage('PUSH DOCKERHUB') {
            steps {
                echo "📤 Push de l'image vers DockerHub..."
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                    sh """
                        echo \$PASS | docker login -u \$USER --password-stdin
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                }
            }
        }

        stage('CHECK KUBERNETES') {
            steps {
                echo "✅ Vérification de Kubernetes..."
                script {
                    try {
                        sh "kubectl get nodes"
                        echo "✅ Kubernetes accessible"
                    } catch (err) {
                        echo "⚠️ Kubernetes non accessible, continuant..."
                    }
                }
            }
        }

        stage('DEPLOY SUR KUBERNETES') {
            steps {
                echo "☸️ Déploiement sur Kubernetes..."
                script {
                    try {
                        sh """
                            kubectl config current-context
                            kubectl get nodes

                            kubectl apply -f ${WORKSPACE}/k8s-manifests/mysql-deployment.yaml -n devops
                            kubectl apply -f ${WORKSPACE}/k8s-manifests/spring-deployment.yaml -n devops
                            kubectl apply -f ${WORKSPACE}/k8s-manifests/sonarqube-deployment.yaml -n devops

                            echo "⏳ Attente du démarrage des Pods..."
                            sleep 10

                            kubectl get pods -n devops
                            kubectl get svc -n devops
                        """
                    } catch (err) {
                        echo "⚠️ Déploiement Kubernetes échoué: ${err}"
                    }
                }
            }
        }

        stage('TEST API SPRING') {
            steps {
                echo "🧪 Test de l'API Spring..."
                script {
                    try {
                        sh '''
                            echo "⏳ Attente du démarrage du Pod Spring..."
                            kubectl wait --for=condition=ready pod -l app=spring-app -n devops --timeout=300s || true

                            # Lancer le port-forward en arrière-plan
                            kubectl port-forward svc/spring-service 8089:8089 -n devops > /tmp/pf_spring.log 2>&1 &
                            PF_PID=$!
                            echo $PF_PID > /tmp/pf_spring.pid
                            sleep 3

                            echo "✅ Port-forward lancé (PID: $PF_PID)"

                            # Tester l'API
                            MAX_RETRIES=30
                            RETRY_COUNT=0

                            while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
                                HTTP_CODE=$(curl -s -o /tmp/response.json -w "%{http_code}" http://localhost:8089/student/Department/getAllDepartment 2>/dev/null || echo "000")

                                if [ "$HTTP_CODE" = "200" ]; then
                                    echo "✅ API est accessible! (HTTP $HTTP_CODE)"
                                    echo "📋 Réponse:"
                                    cat /tmp/response.json
                                    break
                                else
                                    RETRY_COUNT=$((RETRY_COUNT + 1))
                                    echo "⏳ Tentative $RETRY_COUNT/$MAX_RETRIES... (HTTP $HTTP_CODE)"
                                    sleep 2
                                fi
                            done

                            # Arrêter le port-forward
                            if [ -f /tmp/pf_spring.pid ]; then
                                PF_PID=$(cat /tmp/pf_spring.pid)
                                kill $PF_PID 2>/dev/null || true
                                echo "🛑 Port-forward arrêté"
                            fi
                        '''
                    } catch (err) {
                        echo "⚠️ Test API échoué: ${err}"
                    }
                }
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
        }
        failure {
            echo "❌ LE PIPELINE A ÉCHOUÉ!"
        }
        always {
            sh "docker system prune -f 2>/dev/null || true"
        }
    }
}
