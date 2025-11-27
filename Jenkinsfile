pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "chernisamar/myapp"
        DOCKER_TAG = "1.0.0"
        GIT_REPO = "https://github.com/Cherni-Samar/management_devops.git"
        GIT_BRANCH = "main"
    }

    tools {
        maven 'Maven 3.8. 6'
        jdk 'JDK 17'
    }

    stages {

        stage('RÉCUPÉRATION CODE') {
            steps {
                echo "📥 Récupération du code depuis GitHub..."
                echo "Repository: ${GIT_REPO}"
                echo "Branch: ${GIT_BRANCH}"

                // Checkout explicite avec le repo GitHub
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

        stage('LIVRABLE') {
            steps {
                echo "📦 Création du livrable (JAR)..."
                sh "mvn package -DskipTests"
            }

        }

        stage('BUILD DOCKER') {
            steps {
                echo "🐳 Construction de l'image Docker..."
                sh """
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                """
            }
        }

        stage('PUSH DOCKERHUB') {
            steps {
                echo "📤 Push vers DockerHub..."
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
    }

    post {
        success {
            echo "✅ PIPELINE TERMINÉ AVEC SUCCÈS !"
            echo "📦 Image Docker: ${DOCKER_IMAGE}:${DOCKER_TAG}"
            echo "🔗 DockerHub: https://hub.docker.com/r/chernisam/myapp"
            echo "📂 GitHub: ${GIT_REPO}"
        }
        failure {
            echo "❌ LE PIPELINE A ÉCHOUÉ !"
        }
        always {
            echo "🧹 Nettoyage..."
            sh "docker system prune -f || true"
        }
    }
}