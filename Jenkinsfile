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
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        /usr/bin/npm install -g sonarqube-scanner
                        npx sonar-scanner \
                            -Dsonar.projectKey=wallet-frontend \
                            -Dsonar.sources=. \
                            -Dsonar.exclusions=**/node_modules/**,**/dist/**
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
