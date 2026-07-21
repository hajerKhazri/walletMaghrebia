pipeline {
    agent none

    stages {
        // 1️⃣ Checkout du code depuis GitHub
        stage('Checkout') {
            agent any
            steps {
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/hajerKhazri/walletMaghrebia.git'
            }
        }

        // 2️⃣ Build du backend (Spring Boot / Maven)
        stage('Build Backend') {
            agent {
                docker {
                    image 'maven:3.9.4-eclipse-temurin-21'
                    args '-v /var/run/docker.sock:/var/run/docker.sock -u 0:0'   // ✅ exécution en root
                }
            }
            steps {
                dir('backend') {
                    // Utilisation d'un cache local dans /tmp pour éviter les problèmes de droits
                    sh 'mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository'
                }
            }
        }

        // 3️⃣ Build du frontend (Angular / Node.js)
        stage('Build Frontend') {
            agent {
                docker {
                    image 'node:20-alpine'
                    args '-u 0:0'   // ✅ root pour éviter les permissions
                }
            }
            steps {
                dir('frontend') {
                    sh 'npm install --legacy-peer-deps'
                    sh 'npm run build -- --configuration production'
                }
            }
        }

        // 4️⃣ Build du service IA (Python)
        stage('Build AI Service') {
            agent {
                docker {
                    image 'python:3.11-slim'
                    args '-u 0:0'   // ✅ root pour écrire dans /app
                }
            }
            steps {
                dir('ai-service') {
                    sh 'pip install -r requirements.txt'
                }
            }
        }

        // 5️⃣ Construction des images Docker (backend, frontend, IA)
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

        // 6️⃣ Déploiement avec Docker Compose (optionnel)
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
