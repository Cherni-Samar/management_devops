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
                echo "📥 Récupération du code..."
                git branch: "${GIT_BRANCH}", url: "${GIT_REPO}"
                sh "git log -1 --oneline"
            }
        }

        stage('CHECK KUBERNETES') {
            steps {
                echo "☸️ Vérification de Kubernetes..."
                script {
                    try {
                        sh 'kubectl get nodes'
                        echo "✅ Kubernetes OK"
                    } catch (err) {
                        echo "⚠️ Kubernetes non dispo, on continue"
                    }
                }
            }
        }

        stage('TESTS UNITAIRES') {
            steps {
                echo "🧪 Tests..."
                sh "mvn test"
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('BUILD') {
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
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                    sh 'echo $PASS | docker login -u $USER --password-stdin && docker push ${DOCKER_IMAGE}:${DOCKER_TAG}'
                }
            }
        }

        stage('DEPLOY') {
            steps {
                echo "☸️ Déploiement..."
                script {
                    try {
                        sh '''
                            kubectl create namespace devops 2>/dev/null || true
                            kubectl apply -f k8s-manifests/mysql-deployment.yaml -n devops
                            kubectl apply -f k8s-manifests/spring-deployment.yaml -n devops
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
    }
}