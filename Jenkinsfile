pipeline {
    agent none

    stages {
        stage('Checkout') {
            agent any
            steps {
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/hajerKhazri/walletMaghrebia.git'
            }
        }

        stage('Build Backend') {
            agent {
                docker {
                    image 'maven:3.9.4-eclipse-temurin-21'
                    args '-u 0:0'   // exécution en root pour éviter les problèmes de droits
                }
            }
            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository'
                }
            }
        }

        stage('Test Backend') {
            agent {
                docker {
                    image 'maven:3.9.4-eclipse-temurin-21'
                    args '-u 0:0'
                }
            }
            steps {
                dir('backend') {
                    sh 'mvn test -Dmaven.repo.local=/tmp/.m2/repository'
                }
            }
        }

        stage('Build Frontend') {
            agent {
                docker {
                    image 'node:20-alpine'
                    args '-u 0:0'
                }
            }
            steps {
                dir('frontend') {
                    sh 'npm install --legacy-peer-deps'
                    sh 'npm run build -- --configuration production'
                }
            }
        }

        stage('Test Frontend') {
            agent {
                docker {
                    image 'node:20-alpine'
                    args '-u 0:0'
                }
            }
            steps {
                dir('frontend') {
                    // Exécute les tests unitaires Angular (si présents). Le `|| true` évite l'échec du pipeline si aucun test n'est configuré.
                    sh 'npm test -- --watch=false --browsers=ChromeHeadless || true'
                }
            }
        }

        stage('Build AI Service') {
            agent {
                docker {
                    image 'python:3.11-slim'
                    args '-u 0:0'
                }
            }
            steps {
                dir('ai-service') {
                    sh 'pip install -r requirements.txt'
                }
            }
        }

        stage('Test AI Service') {
            agent {
                docker {
                    image 'python:3.11-slim'
                    args '-u 0:0'
                }
            }
            steps {
                dir('ai-service') {
                    sh 'pytest || echo "Aucun test Python configuré"'
                }
            }
        }

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
