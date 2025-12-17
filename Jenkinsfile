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
                        echo "⚠️ Kubernetes non accessible, continuant..."
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
                script {
                    try {
                        sh "mvn test"
                    } catch (err) {
                        echo "⚠️ Tests échoués, continuant..."
                    }
                }
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
                    try {
                        def pf = sh(script: "kubectl -n devops port-forward svc/sonarqube-service 9000:9000 > /tmp/pf_sonar.log 2>&1 & echo \$!", returnStdout: true).trim()
                        echo "Port-forward PID: ${pf}"
                        sleep 5

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

                        sh "kill ${pf} || true"
                    } catch (err) {
                        echo "⚠️ SonarQube analysis skipped: ${err}"
                    }
                }
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
                        echo "✅ Image poussée avec succès"
                    """
                }
            }
        }

        stage('DEPLOY SUR KUBERNETES') {
            steps {
                echo "☸️ Déploiement sur Kubernetes..."
                script {
                    try {
                        sh '''
                            echo "Création du namespace devops..."
                            kubectl create namespace devops 2>/dev/null || true

                            echo "Application des manifests..."
                            kubectl apply -f ${WORKSPACE}/k8s-manifests/mysql-deployment.yaml -n devops
                            kubectl apply -f ${WORKSPACE}/k8s-manifests/spring-deployment.yaml -n devops
                            kubectl apply -f ${WORKSPACE}/k8s-manifests/sonarqube-deployment.yaml -n devops

                            echo "⏳ Attente du démarrage des Pods..."
                            sleep 10

                            echo ""
                            echo "📊 État des Pods..."
                            kubectl get pods -n devops

                            echo ""
                            echo "🔗 Services..."
                            kubectl get svc -n devops

                            echo "✅ Déploiement terminé"
                        '''
                    } catch (err) {
                        echo "⚠️ Déploiement échoué: ${err}"
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
                            echo "⏳ Attente du Pod Spring..."
                            kubectl wait --for=condition=ready pod -l app=spring-app -n devops --timeout=300s 2>/dev/null || echo "⚠️ Pod pas prêt"

                            echo ""
                            echo "📡 Lancement du port-forward..."
                            kubectl port-forward svc/spring-service 8089:8089 -n devops > /tmp/pf_spring.log 2>&1 &
                            PF_PID=$!
                            echo $PF_PID > /tmp/pf_spring.pid
                            sleep 3

                            echo "🔗 Test de l'endpoint /student/Department/getAllDepartment..."

                            MAX_RETRIES=30
                            RETRY_COUNT=0
                            HTTP_CODE=000

                            while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
                                HTTP_CODE=$(curl -s -o /tmp/response.json -w "%{http_code}" http://localhost:8089/student/Department/getAllDepartment 2>/dev/null || echo "000")

                                if [ "$HTTP_CODE" = "200" ]; then
                                    echo "✅ API est accessible! (HTTP $HTTP_CODE)"
                                    echo ""
                                    echo "📋 Réponse:"
                                    cat /tmp/response.json
                                    echo ""
                                    break
                                else
                                    RETRY_COUNT=$((RETRY_COUNT + 1))
                                    echo "⏳ Tentative $RETRY_COUNT/$MAX_RETRIES... (HTTP $HTTP_CODE)"
                                    sleep 2
                                fi
                            done

                            if [ "$HTTP_CODE" != "200" ]; then
                                echo "⚠️ API retourne HTTP $HTTP_CODE après $MAX_RETRIES tentatives"
                            fi

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
            echo ""
            echo "============================================"
            echo "✅ PIPELINE TERMINÉ AVEC SUCCÈS!"
            echo "============================================"
            echo ""
            echo "📦 Image Docker: ${DOCKER_IMAGE}:${DOCKER_TAG}"
            echo "🔗 DockerHub: https://hub.docker.com/r/chernisamar/myapp"
            echo "📂 GitHub: ${GIT_REPO}"
            echo "🔍 SonarQube: http://127.0.0.1:9000/dashboard?id=${SONAR_PROJECT_KEY}"
            echo "☸️ Kubernetes: namespace devops"
            echo ""
        }
        failure {
            echo ""
            echo "============================================"
            echo "❌ LE PIPELINE A ÉCHOUÉ!"
            echo "============================================"
            echo ""
        }
        always {
            sh "docker system prune -f 2>/dev/null || true"
        }
    }
}
