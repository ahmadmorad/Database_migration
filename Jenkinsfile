pipeline {
    agent any

    environment {
        DB_NAME = 'liquibasedb'
        DB_USER = 'postgres'
        DB_PASSWORD = 'password'
        DB_PORT = '5432'
        DB_IMAGE = 'postgres:13'
    }

    stages {

        stage('Start PostgreSQL') {
            steps {
                script {
                    sh '''
                        docker network create ci-network || true

                        docker run -d --name liquibase-db --network ci-network \
                            -e POSTGRES_DB=${DB_NAME} \
                            -e POSTGRES_USER=${DB_USER} \
                            -e POSTGRES_PASSWORD=${DB_PASSWORD} \
                            -p ${DB_PORT}:5432 ${DB_IMAGE}

                        echo "Waiting for PostgreSQL to start..."
                        sleep 10
                    '''
                }
            }
        }

        stage('Run Liquibase Migration') {
            steps {
                sh '''
                    ./mvnw liquibase:update \
                      -Dspring.datasource.url=jdbc:postgresql://localhost:${DB_PORT}/${DB_NAME} \
                      -Dspring.datasource.username=${DB_USER} \
                      -Dspring.datasource.password=${DB_PASSWORD}
                '''
            }
        }

        stage('Build Application') {
            steps {
                sh './mvnw clean package'
            }
        }

    }

    post {
        always {
            echo 'Cleaning up...'
            sh 'docker stop liquibase-db || true && docker rm liquibase-db || true'
        }
    }
}
