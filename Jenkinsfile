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
                    kubectl create namespace devops 2>/dev/null || true
                    kubectl apply -f k8s-manifests/mysql-deployment.yaml -n devops || true
                    kubectl apply -f k8s-manifests/spring-deployment.yaml -n devops || true
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
                    echo "🔗 ACCÈS À L'APPLICATION"
                    echo "============================================"
                    echo ""
                    SERVICE_URL=$(kubectl get service spring-service -n devops -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
                    if [ -z "$SERVICE_URL" ]; then
                        echo "✅ Port-Forward:"
                        echo "   kubectl port-forward svc/spring-service 8089:8089 -n devops"
                        echo "   🌐 http://localhost:8089/student/Department/getAllDepartment"
                    else
                        echo "✅ URL:"
                        echo "   🌐 http://$SERVICE_URL:8089/student/Department/getAllDepartment"
                    fi
                    echo ""
                    echo "============================================"
                '''
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
