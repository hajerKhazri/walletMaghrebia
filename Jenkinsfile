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
                    # Désactiver les budgets
                    if [ -f angular.json ]; then
                        sed -i '/"budgets":/,/]/c\\"budgets": []' angular.json
                    fi
                    /usr/bin/npm install --legacy-peer-deps
                    /usr/bin/npm run build -- --configuration production
                '''
            }
        }

        stage('SonarQube') {
            steps {
                withCredentials([string(credentialsId: 'sonar-token-3', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        /usr/bin/npm install -g sonarqube-scanner
                        npx sonar-scanner \
                            -Dsonar.projectKey=wallet-frontend \
                            -Dsonar.sources=. \
                            -Dsonar.exclusions=**/node_modules/**,**/dist/** \
                            -Dsonar.host.url=http://host.docker.internal:9000 \
                            -Dsonar.login=${SONAR_TOKEN}
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
