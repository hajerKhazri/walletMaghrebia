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

                        # === Vérification dans le conteneur ===
                        echo "🔍 Vérification du contenu du répertoire dans le conteneur :"
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE}/$POM_DIR \
                          maven:3.9.4-eclipse-temurin-21 \
                          sh -c "pwd && ls -la && echo 'Recherche pom.xml:' && find . -name pom.xml"

                        # === Exécution de Maven ===
                        echo "🚀 Lancement de Maven :"
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE}/$POM_DIR \
                          maven:3.9.4-eclipse-temurin-21 \
                          mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh '''
                        # Trouver le dossier contenant Dockerfile
                        DOCKER_DIR=$(find . -name "Dockerfile" -printf "%h\n" | head -1)
                        if [ -z "$DOCKER_DIR" ]; then
                            echo "❌ Aucun Dockerfile trouvé !"
                            exit 1
                        fi
                        echo "🐳 Dockerfile trouvé dans : $DOCKER_DIR"
                        echo "DOCKER_CONTEXT=$DOCKER_DIR" > /tmp/docker_context.txt
                    '''
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
