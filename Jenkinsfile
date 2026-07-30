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

        stage('Build') {
            steps {
                sh '/usr/bin/mvn clean package -DskipTests'
            }
        }

        stage('SonarQube') {
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    sh """
                        /usr/bin/mvn org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
                          -Dsonar.projectKey=wallet-backend \
                          -Dsonar.host.url=http://host.docker.internal:9000 \
                          -Dsonar.login=${SONAR_TOKEN} \
                          -Dsonar.userHome=/tmp/sonar-cache \
                          -Dmaven.repo.local=/tmp/.m2/repository
                    """
                }
            }
        }
    }

    post {
        success { echo "✅ Build backend réussi" }
        failure { error "❌ Build backend échoué" }
    }
}
