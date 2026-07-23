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
        // NOUVEAU : Analyse SonarQube
        // ==========================================
        stage('SonarQube Analysis') {
            steps {
                script {
                    // 'SonarQube' est le nom du serveur configuré dans Jenkins
                    withSonarQubeEnv('SonarQube') {
                        sh 'mvn sonar:sonar -Dsonar.projectKey=wallet-backend -Dsonar.host.url=http://sonarqube:9000'
                    }
                }
            }
        }

        // ==========================================
        // (Optionnel) Attente de la qualité SonarQube
        // ==========================================
        stage('Quality Gate') {
            steps {
                script {
                    // Attend que le Quality Gate soit passé (optionnel)
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

        // Étape Push à réactiver plus tard
        /*
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
        */
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
