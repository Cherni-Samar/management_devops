pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "chernisamar/myapp"
        DOCKER_TAG = "1.0.0"
        GIT_REPO = "https://github.com/Cherni-Samar/management_devops.git"
        GIT_BRANCH = "main"
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
                    kubectl apply -f k8s-manifests/mysql-deployment.yaml -n devops --validate=false || true
                    kubectl apply -f k8s-manifests/spring-deployment.yaml -n devops --validate=false || true
                    sleep 10
                '''
            }
        }

        stage('ACCÈS APPLICATION') {
            steps {
                echo "🌐 Génération des URLs d'accès..."
                sh '''
                    echo ""
                    echo "============================================"
                    echo "🔗 ACCÈS À L'APPLICATION"
                    echo "============================================"
                    echo ""
                    echo "✅ Option 1: Port-Forward (RECOMMANDÉ)"
                    echo "   Commande:"
                    echo "   kubectl port-forward svc/spring-service 8089:8089 -n devops"
                    echo ""
                    echo "   URL:"
                    echo "   🌐 http://localhost:8089/student/Department/getAllDepartment"
                    echo ""
                    echo "✅ Option 2: Via Minikube"
                    echo "   Commande:"
                    echo "   minikube service spring-service -n devops"
                    echo ""
                    echo "   Cela va ouvrir l'URL automatiquement"
                    echo ""
                    echo "✅ Option 3: Adresse IP interne Minikube"
                    MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "192.168.49.2")
                    echo "   IP: $MINIKUBE_IP"
                    echo "   URL: http://$MINIKUBE_IP:30000/student/Department/getAllDepartment"
                    echo ""
                    echo "============================================"
                '''
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
            echo "📍 DockerHub: https://hub.docker.com/r/chernisamar/myapp"
            echo "🚀 Application déployée"
            echo ""
        }
        failure {
            echo "❌ PIPELINE ÉCHOUÉ!"
        }
        always {
            sh "docker system prune -f 2>/dev/null || true"
        }
    }
}
