pipeline {
    agent{
        any
    }
    environment {
        COMPOSE_FILE = 'docker-compose.yaml'
    }

    stages{
        stage('Checkout') {
            steps{
                checkout scm
            }
        }
        stage('Stop Old Containers') {
            steps{
                sh "docker compose -f ${COMPOSE_FILE} down || true"
            }
        }
        stage('Build & Deploy') {
            sh "docker compose -f ${COMPOSE_FILE} up -d --build"
        }

        stage('Cleanup') {
            sh "docker image prune -f"
        }
    }
}