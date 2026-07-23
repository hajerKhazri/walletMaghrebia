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
                                  -Dsonar.userHome=/tmp/sonar-cache \
                                  -Dmaven.repo.local=/tmp/.m2/repository
                            '''
                        }
                    }
                }
            }
        }

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

        // Étape Push désactivée (à réactiver plus tard)
        // stage('Push to Docker Hub') {
        //     steps {
        //         script {
        //             docker.withRegistry('https://index.docker.io/v1/', 'docker-credentials') {
        //                 docker.image("wallet-backend:${BUILD_NUMBER}").push()
        //                 docker.image("wallet-backend:${BUILD_NUMBER}").push('latest')
        //             }
        //         }
        //     }
        // }
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
