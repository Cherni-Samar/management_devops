pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "chernisam/myapp"
        DOCKER_TAG = "1.0.0"
    }

    tools {
        maven 'Maven 3.8.6'
        jdk 'JDK 17'
    }

    stages {

        stage('RÉCUPÉRATION CODE') {
            steps {
                echo "📥 Récupération du code depuis GitHub..."
                checkout scm
                sh "git log -1 --oneline"
            }
        }

        stage('BUILD') {
            steps {
                echo "🔨 Compilation du projet..."
                sh "mvn -version"
                sh "mvn clean compile -DskipTests"
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
                sh "ls -lh target/*.jar"
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('BUILD DOCKER') {
            steps {
                echo "🐳 Construction de l'image Docker..."
                sh """
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                """
                sh "docker images ${DOCKER_IMAGE}"
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
        }
        failure {
            echo "❌ LE PIPELINE A ÉCHOUÉ !"
        }
    }
}
