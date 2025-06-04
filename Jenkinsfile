pipeline {
    agent any

    environment {
        FLYWAY_VERSION = "v1.0.0"
        LIQUIBASE_VERSION = "v1.0.0"
        POSTGRES_USER = "postgres"
        POSTGRES_PASSWORD = "password"
        FLYWAY_DB = "mydb"
        LIQUIBASE_DB = "liquibasedb"
        POSTGRES_PORT = "5433"

        LOCAL_DB_HOST = "pg_test"
        DOCKER_DB_HOST = "db"
        ACTIVE_DB_HOST = "db"
    }

    stages {

        stage('Start PostgreSQL') {
            steps {
                echo "Starting PostgreSQL container..."
                sh '''
                    docker network create ci-network || true

                    docker run -d --name pg_test --network ci-network \
                        -e POSTGRES_DB=${FLYWAY_DB} \
                        -e POSTGRES_USER=${POSTGRES_USER} \
                        -e POSTGRES_PASSWORD=${POSTGRES_PASSWORD} \
                        -p ${POSTGRES_PORT}:5432 postgres:13

                    sleep 5

                    docker exec pg_test psql -U ${POSTGRES_USER} -c "CREATE DATABASE ${LIQUIBASE_DB};"
                    docker exec pg_test pg_isready -U ${POSTGRES_USER}
                '''
            }
        }

        stage('Drop flyway_schema_history (for test)') {
            steps {
                echo "Dropping flyway_schema_history..."
                sh '''
                    docker exec -i pg_test psql -U ${POSTGRES_USER} -d ${FLYWAY_DB} \
                        -c "DROP TABLE IF EXISTS flyway_schema_history CASCADE;"
                '''
            }
        }

        stage('Build Flyway Image and Run Migration') {
            steps {
                echo "Running Flyway migrations..."
                sh '''
                    docker build -t flyway-migrations:${FLYWAY_VERSION} -f infrastructure/flyway/Dockerfile .

                    docker run --rm \
                        --network ci-network \
                        flyway-migrations:${FLYWAY_VERSION} \
                        -url=jdbc:postgresql://${ACTIVE_DB_HOST}:5432/${FLYWAY_DB} \
                        -user=${POSTGRES_USER} \
                        -password=${POSTGRES_PASSWORD} \
                        migrate
                '''
            }
        }

        stage('Show Flyway History') {
            steps {
                sh '''
                    docker run --rm \
                        --network ci-network \
                        flyway-migrations:${FLYWAY_VERSION} \
                        -url=jdbc:postgresql://${ACTIVE_DB_HOST}:5432/${FLYWAY_DB} \
                        -user=${POSTGRES_USER} \
                        -password=${POSTGRES_PASSWORD} \
                        info
                '''
            }
        }

        stage('Build Liquibase Image and Run Migration') {
            steps {
                echo "Running Liquibase migrations..."
                sh '''
                    docker build -t liquibase-runner:${LIQUIBASE_VERSION} -f infrastructure/liquibase/Dockerfile .

                    docker run --rm \
                        --network ci-network \
                        liquibase-runner:${LIQUIBASE_VERSION} \
                        --url=jdbc:postgresql://${ACTIVE_DB_HOST}:5432/${LIQUIBASE_DB} \
                        --username=${POSTGRES_USER} \
                        --password=${POSTGRES_PASSWORD} \
                        --changeLogFile=changelog/master.xml \
                        update
                '''
            }
        }
    }

    post {
        always {
            echo "Cleaning up..."
            sh '''
                docker rm -f pg_test || true
                docker network rm ci-network || true
            '''
        }
    }
}
