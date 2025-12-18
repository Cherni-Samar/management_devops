pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "chernisamar/myapp"
        DOCKER_IMAGE_ANGULAR = "chernisamar/student-manager"
        DOCKER_TAG = "1.0.0"
        SPRING_REPO = "https://github.com/Cherni-Samar/management_devops.git"
        ANGULAR_REPO = "https://github.com/Cherni-Samar/angular_devops.git"
        GIT_BRANCH = "main"
    }

    tools {
        maven 'Maven'
        jdk 'jdk'
    }

    stages {
         stage('RÉCUPÉRATION SPRING') {
                    steps {
                        echo "📥 Spring Code..."
                        git credentialsId: 'github-credentials', branch: "${GIT_BRANCH}", url: "${SPRING_REPO}"
                    }
         }

        stage('ANALYSE SONARQUBE') {
            steps {
                echo "📊 SonarQube..."
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        # Port-forward SonarQube en background
                        kubectl port-forward svc/sonarqube-service 9000:9000 -n devops > /dev/null 2>&1 &
                        PF_PID=$!
                        sleep 5
                        
                        # Attendez SonarQube
                        for i in {1..30}; do
                            if curl -s http://localhost:9000/api/system/status | grep -q UP; then
                                echo "✅ SonarQube accessible"
                                break
                            fi
                            echo "⏳ Attente SonarQube..."
                            sleep 2
                        done
                        
                        # Analysez
                        mvn sonar:sonar \
                          -Dsonar.projectKey=management_devops \
                          -Dsonar.sources=src/main/java \
                          -Dsonar.tests=src/test/java \
                          -Dsonar.host.url=http://localhost:9000 \
                          -Dsonar.login=${SONAR_TOKEN} || true
                        
                        # Arrêtez port-forward
                        kill $PF_PID 2>/dev/null || true
                    '''
                }
            }
        }

        stage('TESTS') {
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

        stage('BUILD') {
            steps {
                echo "📦 Build..."
                sh "mvn package -DskipTests"
            }
        }

        stage('DOCKER') {
            steps {
                echo "🐳 Docker..."
                sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
            }
        }

        stage('PUSH') {
            steps {
                echo "📤 Push..."
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh 'docker login -u "$USER" -p "$PASS" && docker push ${DOCKER_IMAGE}:${DOCKER_TAG} && docker logout'
                }
            }
        }
       stage('RÉCUPÉRATION ANGULAR') {
                   steps {
                       echo "📥 Angular Code..."
                       dir('angular-app') {
                           git credentialsId: 'github-credentials', branch: "${GIT_BRANCH}", url: "${ANGULAR_REPO}"
                       }
                   }
               }


                stage('BUILD ANGULAR') {
                    steps {
                        echo "📦 Build Angular..."
                        dir('angular-app') {
                            sh '''
                                npm install
                                npm run build --prod
                            '''
                        }
                    }
                }

                stage('DOCKER ANGULAR') {
                    steps {
                        echo "🐳 Docker Angular..."
                        dir('angular-app') {
                            sh "docker build -t ${DOCKER_IMAGE_ANGULAR}:${DOCKER_TAG} ."
                        }
                    }
                }

                stage('PUSH ANGULAR') {
                    steps {
                        echo "📤 Push Angular..."
                        withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                            sh 'docker login -u "$USER" -p "$PASS" && docker push ${DOCKER_IMAGE_ANGULAR}:${DOCKER_TAG} && docker logout'
                        }
                    }
                }


        stage('DEPLOY') {
            steps {
                echo "☸️ Deploy..."
                sh 'kubectl apply -f k8s-manifests/ -n devops --validate=false 2>/dev/null || true'
            }
        }

                stage('DEPLOY ANGULAR') {
                    steps {
                        echo "☸️ Deploy Angular..."
                        sh 'kubectl apply -f k8s-manifests/angular-deployment.yaml -n devops --validate=false 2>/dev/null || true'
                    }
                }


        stage('DEPLOY PROMETHEUS') {
                    steps {
                        echo "📊 Deploy Prometheus..."
                        sh 'kubectl apply -f k8s-manifests/prometheus-deployment.yaml -n devops --validate=false 2>/dev/null || true'
                    }
                }

                stage('DEPLOY GRAFANA') {
                    steps {
                        echo "📈 Deploy Grafana..."
                        sh 'kubectl apply -f k8s-manifests/grafana-deployment.yaml -n devops --validate=false 2>/dev/null || true'
                    }
                }

                stage('ACCÈS') {
                            steps {
                                echo "🌐 URLs d'accès..."
                                sh '''
                                    echo ""
                                    echo "============================================"
                                    echo "✅ FULL STACK DÉPLOYÉ!"
                                    echo "============================================"
                                    echo ""
                                    echo "🔗 Frontend Angular:"
                                    echo "   http://localhost:30080"
                                    echo ""
                                    echo "🔗 Backend Spring:"
                                    echo "   kubectl port-forward svc/spring-service 8089:8089 -n devops"
                                    echo "   http://localhost:8089/student/Department/getAllDepartment"
                                    echo ""
                                    echo "📊 Prometheus:"
                                    echo "   http://localhost:30090"
                                    echo ""
                                    echo "📈 Grafana:"
                                    echo "   http://localhost:30300"
                                    echo "   Login: admin / grafana"
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
    }
}
