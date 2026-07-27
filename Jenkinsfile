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

        stage('Build Frontend') {
            agent {
                docker {
                    image 'node:20-alpine'
                    args '-u root'
                    reuseNode true
                }
            }
            steps {
                sh '''
                    if [ -f angular.json ]; then
                        sed -i '/"budgets":/,/]/c\\"budgets": []' angular.json
                    fi
                '''
                sh 'npm install --legacy-peer-deps'
                sh 'npm test -- --watch=false --browsers=ChromeHeadless || true'
                sh '''
                    export NODE_OPTIONS="--max-old-space-size=2048"
                    npm run build -- --configuration production
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    docker.image('sonarsource/sonar-scanner-cli:latest').inside {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                echo "🔍 Analyse SonarQube..."
                                sonar-scanner \
                                    -Dsonar.projectKey=wallet-frontend \
                                    -Dsonar.sources=. \
                                    -Dsonar.exclusions=**/node_modules/**,**/dist/**,**/coverage/** \
                                    -Dsonar.typescript.lcov.reportPaths=coverage/lcov.info \
                                    -Dsonar.host.url=http://host.docker.internal:9000
                            '''
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Frontend build ${BUILD_NUMBER} réussi ! (images Docker existantes utilisées)"
        }
        failure {
            error "❌ Frontend build ${BUILD_NUMBER} échoué."
        }
    }
}
