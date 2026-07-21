pipeline {
    agent any

    stages {
        stage('Checkout avec sous-modules') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: 'main']],
                    userRemoteConfigs: [[
                        credentialsId: 'github-credentials',
                        url: 'https://github.com/hajerKhazri/walletMaghrebia.git'
                    ]],
                    extensions: [[
                        $class: 'SubmoduleOption',
                        disableSubmodules: false,
                        parentCredentials: true,
                        recursiveSubmodules: true,
                        reference: '',
                        trackingSubmodules: false,
                        timeout: 10
                    ]]
                ])
            }
        }

        stage('Debug - Structure') {
            steps {
                sh '''
                    echo "=== Contenu de la racine ==="
                    ls -la
                    echo "=== Contenu de wallet/ ==="
                    ls -la wallet/ || echo "wallet/ n'existe pas"
                    echo "=== Contenu de wallet-frontend/ ==="
                    ls -la wallet-frontend/ || echo "wallet-frontend/ n'existe pas"
                    echo "=== Recherche de pom.xml ==="
                    find . -name "pom.xml"
                    echo "=== Recherche de package.json ==="
                    find . -name "package.json"
                '''
            }
        }

        stage('Build Backend') {
            steps {
                script {
                    sh '''
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE}/wallet \
                          maven:3.9.4-eclipse-temurin-21 \
                          mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository
                    '''
                }
            }
        }

        stage('Build Frontend') {
            steps {
                script {
                    sh '''
                        docker run --rm \
                          -v ${WORKSPACE}:${WORKSPACE} \
                          -w ${WORKSPACE}/wallet-frontend \
                          node:20-alpine \
                          sh -c "npm install --legacy-peer-deps && npm run build -- --configuration production"
                    '''
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    docker.build("wallet-backend:${BUILD_NUMBER}", './wallet')
                    docker.build("wallet-frontend:${BUILD_NUMBER}", './wallet-frontend')
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    docker-compose down -v
                    docker-compose up -d --build
                    docker system prune -f
                '''
            }
        }
    }

    post {
        success {
            echo "✅ Pipeline ${BUILD_NUMBER} réussi !"
        }
        failure {
            error "❌ Pipeline ${BUILD_NUMBER} échoué."
        }
    }
}
