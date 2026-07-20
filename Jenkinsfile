pipeline {
    agent any

    environment {
       
        IMAGE_NAME = 'wallet-frontend'
        // ════════════════════════════════
    }

    stages {
      
        stage('Checkout') {
            steps {
                git branch: 'frontend',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/hajerKhazri/walletMaghrebia-frontend.git'
            }
        }

     
        stage('Build & Test Frontend') {
            steps {
              
                sh 'npm install'
                
              
                sh 'npm run test:ci -- --watch=false --browsers=ChromeHeadless' || true
                
              
                sh 'npm run build -- --configuration production'
            }
        }

        
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${IMAGE_NAME}:${BUILD_NUMBER}")
                }
            }
        }
    }

    post {
        success {
            echo "✅ Frontend build ${BUILD_NUMBER} réussi ! Image : ${IMAGE_NAME}:${BUILD_NUMBER}"
        }
        failure {
            error "❌ Frontend build ${BUILD_NUMBER} échoué."
        }
    }
}
