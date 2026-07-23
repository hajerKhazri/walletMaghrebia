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
                    sh '''
                        # Trouver le dossier contenant pom.xml
                        POM_DIR=$(find . -name "pom.xml" -printf "%h\n" | head -1)
                        if [ -z "$POM_DIR" ]; then
                            echo "❌ Aucun fichier pom.xml trouvé !"
                            exit 1
                        fi
                        echo "✅ pom.xml trouvé dans : $POM_DIR"

                        # Exécuter Maven dans ce dossier
                        docker run --rm \\
                          -v ${WORKSPACE}:${WORKSPACE} \\
                          -w ${WORKSPACE}/$POM_DIR \\
                          maven:3.9.4-eclipse-temurin-21 \\
                          mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    // Déterminer le contexte Docker (le dossier contenant Dockerfile)
                    // On suppose que le Dockerfile est dans le même dossier que pom.xml ou dans un sous-dossier 'wallet'
                    sh '''
                        DOCKER_DIR=$(find . -name "Dockerfile" -printf "%h\n" | head -1)
                        if [ -z "$DOCKER_DIR" ]; then
                            echo "❌ Aucun Dockerfile trouvé !"
                            exit 1
                        fi
                        echo "🐳 Dockerfile trouvé dans : $DOCKER_DIR"
                        # Passer le chemin relatif au script Docker
                        echo "DOCKER_CONTEXT=$DOCKER_DIR" > /tmp/docker_context.txt
                    '''
                    // Lire le contexte depuis le fichier temporaire
                    def dockerContext = readFile('/tmp/docker_context.txt').trim().split('=')[1]
                    docker.build("wallet-backend:${BUILD_NUMBER}", "-f ${dockerContext}/Dockerfile ${dockerContext}")
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
