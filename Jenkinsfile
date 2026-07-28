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

        stage('Build') {
            steps {
                sh '''
                    /usr/bin/npm install --legacy-peer-deps
                    /usr/bin/npm run build -- --configuration production
                '''
            }
        }

        stage('SonarQube') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        /usr/bin/npm install -g sonarqube-scanner
                        /usr/bin/sonar-scanner \
                            -Dsonar.projectKey=wallet-frontend \
                            -Dsonar.sources=. \
                            -Dsonar.exclusions=**/node_modules/**,**/dist/** \
                            -Dsonar.host.url=http://host.docker.internal:9000
                    '''
                }
            }
        }
    }

    post {
        success { echo "✅ Build frontend réussi" }
        failure { error "❌ Build frontend échoué" }
    }
}
