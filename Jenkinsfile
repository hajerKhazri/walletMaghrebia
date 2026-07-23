pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'backendd',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/hajerKhazri/walletMaghrebia.git'
            }
        }

        stage('Build Backend') {
            steps {
                script {
                    sh '''
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE} \
                          maven:3.9.4-eclipse-temurin-21 \
                          mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("wallet-backend:${BUILD_NUMBER}")
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'docker-credentials') {
                        docker.image("wallet-backend:${BUILD_NUMBER}").push()
                        docker.image("wallet-backend:${BUILD_NUMBER}").push('latest')
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Backend build ${BUILD_NUMBER} réussi !"
        }
        failure {
            error "❌ Backend build ${BUILD_NUMBER} échoué."
        }
    }
}
