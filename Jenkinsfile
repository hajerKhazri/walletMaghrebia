pipeline {
    agent any

    environment {

        BACKEND_IMAGE = 'wallet-backend'
        FRONTEND_IMAGE = 'wallet-frontend'
        AI_IMAGE = 'wallet-ai'
    }

    stages {
        // 1️⃣ Récupération du code
        stage('Checkout') {
            steps {
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/hajerKhazri/walletMaghrebia.git'
            }
        }

        // 2️⃣ Build & Test Backend
        stage('Build Backend') {
            steps {
                dir('backend') {
                    sh 'mvn clean test'
                    sh 'mvn package -DskipTests'
                }
            }
        }

        // 3️⃣ Build & Test Frontend
        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run build -- --configuration production'
                }
            }
        }

        // 4️⃣ Build & Test IA
        stage('Build AI Service') {
            steps {
                dir('ai-service') {
                    sh 'pip install -r requirements.txt || true'
                    // Tu peux ajouter des tests ici si tu en as
                }
            }
        }

        // 5️⃣ Build Docker Images
        stage('Build Docker Images') {
            steps {
                script {
                    docker.build("${BACKEND_IMAGE}:${BUILD_NUMBER}", './backend')
                    docker.build("${FRONTEND_IMAGE}:${BUILD_NUMBER}", './frontend')
                    docker.build("${AI_IMAGE}:${BUILD_NUMBER}", './ai-service')
                }
            }
        }

        // 6️⃣ (Optionnel) Push vers Docker Hub
        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'docker-credentials') {
                         docker.image("${BACKEND_IMAGE}:${BUILD_NUMBER}").push()
                        docker.image("${BACKEND_IMAGE}:${BUILD_NUMBER}").push('latest')
                       docker.image("${FRONTEND_IMAGE}:${BUILD_NUMBER}").push()
                         docker.image("${FRONTEND_IMAGE}:${BUILD_NUMBER}").push('latest')
                        docker.image("${AI_IMAGE}:${BUILD_NUMBER}").push()
                         docker.image("${AI_IMAGE}:${BUILD_NUMBER}").push('latest')
                     }
                 }
            }
         }

        // 7️⃣ Déploiement avec Docker Compose
        stage('Deploy') {
            steps {
                sh '''
                    docker-compose down
                    docker-compose up -d --build
                    docker system prune -f
                '''
            }
        }
    }

    post {
        success {
            echo "✅ Build ${BUILD_NUMBER} réussi !"
        }
        failure {
            error "❌ Build ${BUILD_NUMBER} échoué."
        }
    }
}
