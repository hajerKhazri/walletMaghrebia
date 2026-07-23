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

        stage('Inspect Workspace') {
            steps {
                sh '''
                    echo "📂 Contenu du workspace :"
                    ls -la
                    echo "🔍 Recherche de pom.xml :"
                    find . -name "pom.xml"
                '''
            }
        }

        stage('Build Backend') {
            steps {
                script {
                    docker.image('maven:3.9.4-eclipse-temurin-21').inside {
                        sh '''
                            echo "🚀 Exécution de Maven depuis le répertoire : $(pwd)"
                            mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository
                        '''
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("wallet-backend:${BUILD_NUMBER}", "-f Dockerfile .")
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
