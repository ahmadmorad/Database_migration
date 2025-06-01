pipeline {
    agent any

    environment {
        FLYWAY_IMAGE = 'flyway/flyway:8.5.0'
        POSTGRES_IMAGE = 'postgres:13'
        DB_NAME = 'mydb'
        DB_USER = 'postgres'
        DB_PASSWORD = 'password'
        DB_PORT = '5432'
        CONTAINER_NAME = 'pg_test'
        NETWORK_NAME = 'flyway-network'
    }

    stages {
        stage('Start PostgreSQL') {
            steps {
                echo "🐘 Starting PostgreSQL container..."
                sh """
                    docker network create ${NETWORK_NAME} || true
                    docker run -d --name ${CONTAINER_NAME} --network ${NETWORK_NAME} \
                        -e POSTGRES_DB=${DB_NAME} \
                        -e POSTGRES_USER=${DB_USER} \
                        -e POSTGRES_PASSWORD=${DB_PASSWORD} \
                        -p 5433:5432 ${POSTGRES_IMAGE}

                    echo "⏳ Waiting for PostgreSQL to be ready..."
                    for i in {1..10}; do
                      docker exec ${CONTAINER_NAME} pg_isready -U ${DB_USER} && break
                      sleep 2
                    done
                """
            }
        }

        stage('Run Flyway Migration') {
            steps {
                echo "🚀 Running Flyway migration..."
                sh """
                    docker run --rm --network ${NETWORK_NAME} \
                        -v "${WORKSPACE}/app/src/main/resources/db/migration:/flyway/sql" \
                        -e FLYWAY_URL=jdbc:postgresql://${CONTAINER_NAME}:${DB_PORT}/${DB_NAME} \
                        -e FLYWAY_USER=${DB_USER} \
                        -e FLYWAY_PASSWORD=${DB_PASSWORD} \
                        ${FLYWAY_IMAGE} migrate
                """
            }
        }

        stage('Show Flyway History') {
            steps {
                echo "📜 Flyway Schema History:"
                sh """
                    docker exec -i ${CONTAINER_NAME} \
                        psql -U ${DB_USER} -d ${DB_NAME} \
                        -c "SELECT version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank;"
                """
            }
        }
    }

    post {
        always {
            echo "🧹 Cleaning up..."
            sh """
                docker rm -f ${CONTAINER_NAME}
                docker network rm ${NETWORK_NAME}
            """
        }
    }
}
