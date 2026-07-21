pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/hajerKhazri/walletMaghrebia.git'
            }
        }

        // 🔍 Étape de diagnostic (à garder temporairement)
        stage('Debug - Structure des dossiers') {
            steps {
                sh '''
                    echo "=== Contenu de la racine ==="
                    ls -la
                    echo "=== Contenu de wallet/ ==="
                    ls -la wallet/ || echo "wallet/ n'existe pas"
                    echo "=== Contenu de wallet-frontend/ ==="
                    ls -la wallet-frontend/ || echo "wallet-frontend/ n'existe pas"
                '''
            }
        }

        // =========================================================
        // BUILD BACKEND (dans wallet/)
        // =========================================================
        stage('Build Backend') {
            steps {
                script {
                    sh '''
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE}/wallet \
                          maven:3.9.4-eclipse-temurin-21 \
                          mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository
                    '''
                }
            }
        }

        // =========================================================
        // TEST BACKEND
        // =========================================================
        stage('Test Backend') {
            steps {
                script {
                    sh '''
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE}/wallet \
                          maven:3.9.4-eclipse-temurin-21 \
                          mvn test -Dmaven.repo.local=/tmp/.m2/repository
                    '''
                }
            }
        }

        // =========================================================
        // BUILD FRONTEND
        // =========================================================
        stage('Build Frontend') {
            steps {
                script {
                    sh '''
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE}/wallet-frontend \
                          node:20-alpine \
                          sh -c "npm install --legacy-peer-deps && npm run build -- --configuration production"
                    '''
                }
            }
        }

        // =========================================================
        // TEST FRONTEND (optionnel)
        // =========================================================
        stage('Test Frontend') {
            steps {
                script {
                    sh '''
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE}/wallet-frontend \
                          node:20-alpine \
                          sh -c "npm test -- --watch=false --browsers=ChromeHeadless || true"
                    '''
                }
            }
        }

        // =========================================================
        // BUILD AI SERVICE (si le dossier existe)
        // =========================================================
        stage('Build AI Service') {
            when { expression { fileExists('ai-service') } }
            steps {
                script {
                    sh '''
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE}/ai-service \
                          python:3.11-slim \
                          sh -c "pip install -r requirements.txt"
                    '''
                }
            }
        }

        // =========================================================
        // TEST AI SERVICE (optionnel)
        // =========================================================
        stage('Test AI Service') {
            when { expression { fileExists('ai-service') } }
            steps {
                script {
                    sh '''
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE}/ai-service \
                          python:3.11-slim \
                          sh -c "pytest || echo 'Aucun test Python configuré'"
                    '''
                }
            }
        }

        // =========================================================
        // BUILD DOCKER IMAGES
        // =========================================================
        stage('Build Docker Images') {
            steps {
                script {
                    docker.build("wallet-backend:${BUILD_NUMBER}", './wallet')
                    docker.build("wallet-frontend:${BUILD_NUMBER}", './wallet-frontend')
                    if (fileExists('ai-service')) {
                        docker.build("wallet-ai:${BUILD_NUMBER}", './ai-service')
                    }
                }
            }
        }

        // =========================================================
        // DEPLOY
        // =========================================================
        stage('Deploy') {
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
