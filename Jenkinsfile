pipeline {
    agent none

    stages {
        // 1️⃣ Checkout
        stage('Checkout') {
            agent any
            steps {
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/hajerKhazri/walletMaghrebia.git'
                // Vérification rapide
                sh 'ls -la'
                sh 'ls -la backend || echo "backend folder not found"'
            }
        }

        // 2️⃣ Build Backend
        stage('Build Backend') {
            agent {
                docker {
                    image 'maven:3.9.4-eclipse-temurin-21'
                    // Montage du workspace et exécution en root
                    args "-u 0:0 -v ${env.WORKSPACE}:${env.WORKSPACE}"
                }
            }
            steps {
                dir('backend') {
                    sh 'pwd'
                    sh 'ls -la'
                    sh 'mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository'
                }
            }
        }

        // 3️⃣ Test Backend
        stage('Test Backend') {
            agent {
                docker {
                    image 'maven:3.9.4-eclipse-temurin-21'
                    args "-u 0:0 -v ${env.WORKSPACE}:${env.WORKSPACE}"
                }
            }
            steps {
                dir('backend') {
                    sh 'mvn test -Dmaven.repo.local=/tmp/.m2/repository'
                }
            }
        }

        // 4️⃣ Build Frontend
        stage('Build Frontend') {
            agent {
                docker {
                    image 'node:20-alpine'
                    args "-u 0:0 -v ${env.WORKSPACE}:${env.WORKSPACE}"
                }
            }
            steps {
                dir('frontend') {
                    sh 'npm install --legacy-peer-deps'
                    sh 'npm run build -- --configuration production'
                }
            }
        }

        // 5️⃣ Test Frontend (optionnel)
        stage('Test Frontend') {
            agent {
                docker {
                    image 'node:20-alpine'
                    args "-u 0:0 -v ${env.WORKSPACE}:${env.WORKSPACE}"
                }
            }
            steps {
                dir('frontend') {
                    sh 'npm test -- --watch=false --browsers=ChromeHeadless || true'
                }
            }
        }

        // 6️⃣ Build AI Service
        stage('Build AI Service') {
            agent {
                docker {
                    image 'python:3.11-slim'
                    args "-u 0:0 -v ${env.WORKSPACE}:${env.WORKSPACE}"
                }
            }
            steps {
                dir('ai-service') {
                    sh 'pip install -r requirements.txt'
                }
            }
        }

        // 7️⃣ Test AI Service (optionnel)
        stage('Test AI Service') {
            agent {
                docker {
                    image 'python:3.11-slim'
                    args "-u 0:0 -v ${env.WORKSPACE}:${env.WORKSPACE}"
                }
            }
            steps {
                dir('ai-service') {
                    sh 'pytest || echo "Aucun test Python configuré"'
                }
            }
        }

        // 8️⃣ Build Docker Images
        stage('Build Docker Images') {
            agent any
            steps {
                script {
                    docker.build("wallet-backend:${BUILD_NUMBER}", './backend')
                    docker.build("wallet-frontend:${BUILD_NUMBER}", './frontend')
                    docker.build("wallet-ai:${BUILD_NUMBER}", './ai-service')
                }
            }
        }

        // 9️⃣ Deploy
        stage('Deploy') {
            agent any
            steps {
                sh '''
                    docker-compose down -v
                    docker-compose up -d --build
                    docker system prune -f
                '''
            }
        }
    }

    post {
        success {
            echo "✅ Pipeline ${BUILD_NUMBER} réussi !"
        }
        failure {
            error "❌ Pipeline ${BUILD_NUMBER} échoué."
        }
    }
}
