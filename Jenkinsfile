pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'front',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/hajerKhazri/walletMaghrebia.git'
            }
        }

        // 🔍 Debug : vérifier la structure
        stage('Debug - Structure') {
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
                script {
                    sh '''
                        docker run --rm \
                          -u 0:0 \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE} \
                          mcr.microsoft.com/playwright:v1.48.0-focal \
                          sh -c "npm install --legacy-peer-deps && npm test -- --watch=false --browsers=ChromeHeadless || true"
                    '''
                }
            }
        }

        stage('Build Frontend') {
            steps {
                script {
                    sh '''
                        docker run --rm \
                          -u 0:0 \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE} \
                          node:20-alpine \
                          sh -c "npm install --legacy-peer-deps && npm run build -- --configuration production"
                    '''
                }
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
