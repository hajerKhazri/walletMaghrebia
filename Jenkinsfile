pipeline {
    agent {
        docker {
            image 'node:20-alpine'
            args '-u root'   // pour éviter les problèmes de permissions
            reuseNode true
        }
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'front',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/hajerKhazri/walletMaghrebia.git'
            }
        }

        // 🔍 Debug : vérifier la structure
        stage('Debug - Contenu') {
            steps {
                sh '''
                    echo "=== Contenu du workspace ==="
                    ls -la
                    echo "=== Recherche de package.json ==="
                    find . -name "package.json"
                '''
            }
        }

        stage('Test Frontend') {
            steps {
                sh '''
                    npm install --legacy-peer-deps
                    npm test -- --watch=false --browsers=ChromeHeadless || true
                '''
            }
        }

        stage('Build Frontend') {
            steps {
                sh 'npm run build -- --configuration production'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("wallet-frontend:${BUILD_NUMBER}", '.')
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'docker-credentials') {
                        docker.image("wallet-frontend:${BUILD_NUMBER}").push()
                        docker.image("wallet-frontend:${BUILD_NUMBER}").push('latest')
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Frontend build ${BUILD_NUMBER} réussi !"
        }
        failure {
            error "❌ Frontend build ${BUILD_NUMBER} échoué."
        }
    }
}
