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

        // ==========================================
        // Analyse SonarQube (dans le même conteneur Maven)
        // ==========================================
        stage('SonarQube Analysis') {
            steps {
                script {
                    docker.image('maven:3.9.4-eclipse-temurin-21').inside {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                echo "🔍 Analyse SonarQube en cours..."
                                mvn sonar:sonar \
                                  -Dsonar.projectKey=wallet-backend \
                                  -Dsonar.host.url=http://host.docker.internal:9000 \
                                  -Dmaven.repo.local=/tmp/.m2/repository
                            '''
                        }
                    }
                }
            }
        }

        // (Optionnel) Attente du Quality Gate
        stage('Quality Gate') {
            steps {
                script {
                    timeout(time: 1, unit: 'HOURS') {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "❌ Quality Gate échoué : ${qg.status}"
                        }
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

        

    post {
        success {
            echo "✅ Backend build ${BUILD_NUMBER} réussi !"
        }
        failure {
            error "❌ Backend build ${BUILD_NUMBER} échoué."
        }
    }
}
