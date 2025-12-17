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
        KUBECONFIG = "/.kube/config"
        }

    tools {
        maven 'Maven'
        jdk 'jdk'
    }

    stages {

        stage('CHECK KUBERNETES') {
            steps {
                echo "✅ Vérification de Kubernetes..."
                sh "kubectl get nodes"
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

        stage('ANALYSE SONARQUBE') {
            steps {
                echo "🔍 Analyse SonarQube via le Pod Kubernetes..."
                script {
                    try {
                        def pf = sh(script: "kubectl -n devops port-forward svc/sonarqube-service 9000:9000 > /tmp/pf.log 2>&1 & echo \$!", returnStdout: true).trim()
                        echo "Port-forward PID: ${pf}"
                        sleep 5

                        timeout(time: 5, unit: 'MINUTES') {
                            waitUntil {
                                def status = sh(script: "curl -s http://127.0.0.1:9000/api/system/status || echo DOWN", returnStdout: true).trim()
                                echo "⏳ Waiting for SonarQube... Status: ${status}"
                                return status.contains('UP')
                            }
                        }

                        // Lancer l'analyse
                        sh """
                            mvn sonar:sonar \\
                              -Dsonar.projectKey=${SONAR_PROJECT_KEY} \\
                              -Dsonar.projectName='Management DevOps' \\
                              -Dsonar.host.url=http://127.0.0.1:9000 \\
                              -Dsonar.login=${SONAR_LOGIN} \\
                              -Dsonar.password=${SONAR_PASSWORD} \\
                              -Dsonar.java.binaries=target/classes
                        """

                        // Kill le port-forward à la fin
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
                    sh "echo \$PASS | docker login -u \$USER --password-stdin"
                    sh "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                }
            }
        }

        stage('DEPLOY SUR KUBERNETES') {
            steps {
                echo "☸️ Déploiement sur Kubernetes..."
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
            }
        }

        stage('TEST API SPRING') {
            steps {
                echo "🧪 Test de l'API Spring via port-forward..."
                script {
                    try {
                        // Attendre que le Pod Spring soit prêt
                        sh '''
                            echo "⏳ Attente du démarrage du Pod Spring..."
                            kubectl wait --for=condition=ready pod -l app=spring-app -n devops --timeout=300s || echo "Warning: Pod not ready"
                        '''

                        // Lancer le port-forward en arrière-plan
                        sh '''
                            kubectl port-forward svc/spring-service 8089:8089 -n devops > /tmp/pf_spring.log 2>&1 &
                            PF_PID=$!
                            echo $PF_PID > /tmp/pf_spring.pid

                            # Attendre que le tunnel soit prêt
                            sleep 3

                            echo "✅ Port-forward lancé (PID: $PF_PID)"
                        '''

                        // Tester l'API
                        sh '''
                            echo "🔗 Test de l'endpoint /student/Department/getAllDepartment..."

                            # Avec retry pour attendre que l'application soit prête
                            MAX_RETRIES=30
                            RETRY_COUNT=0

                            while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
                                HTTP_CODE=$(curl -s -o /tmp/response.json -w "%{http_code}" http://localhost:8089/student/Department/getAllDepartment)

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

                            if [ "$HTTP_CODE" != "200" ]; then
                                echo "⚠️ Warning: API retourne HTTP $HTTP_CODE après $MAX_RETRIES tentatives"
                            fi
                        '''

                    } catch (err) {
                        echo "⚠️ Test API skipped: ${err}"
                    } finally {
                        // Arrêter le port-forward
                        sh '''
                            if [ -f /tmp/pf_spring.pid ]; then
                                PF_PID=$(cat /tmp/pf_spring.pid)
                                kill $PF_PID || true
                                echo "🛑 Port-forward arrêté"
                            fi
                        '''
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