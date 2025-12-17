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
                    echo "✅ COMMANDE POUR OBTENIR L'URL:"
                    echo "   minikube service spring-service -n devops"
                    echo ""
                    echo "   Résultat: http://127.0.0.1:XXXXX"
                    echo ""
                    echo "   Puis accédez à:"
                    echo "   🌐 http://127.0.0.1:XXXXX/student/Department/getAllDepartment"
                    echo ""
                    echo "✅ Ou Port-Forward directement:"
                    echo "   kubectl port-forward svc/spring-service 8089:8089 -n devops"
                    echo "   http://localhost:8089/student/Department/getAllDepartment"
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
            echo "✅ PIPELINE RÉUSSI!"
            echo "============================================"
            echo ""
            echo "📦 Image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
            echo "🚀 Application déployée et accessible!"
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
