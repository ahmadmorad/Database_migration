pipeline {
    agent any

    environment {
        FLYWAY_VERSION = "v1.0.0"
        POSTGRES_USER = "postgres"
        POSTGRES_PASSWORD = "password"
        POSTGRES_DB = "mydb"
        POSTGRES_PORT = "5433"
    }

    stages {

        stage('Start PostgreSQL') {
            steps {
                echo "🐘 Starting PostgreSQL container..."
                sh '''
                    docker network create flyway-network || true
                    docker run -d --name pg_test --network flyway-network \
                        -e POSTGRES_DB=${POSTGRES_DB} \
                        -e POSTGRES_USER=${POSTGRES_USER} \
                        -e POSTGRES_PASSWORD=${POSTGRES_PASSWORD} \
                        -p ${POSTGRES_PORT}:5432 postgres:13

                    echo "⏳ Waiting for PostgreSQL to be ready..."
                    sleep 5
                    docker exec pg_test pg_isready -U ${POSTGRES_USER}
                '''
            }
        }

        stage('Drop flyway_schema_history (for test)') {
            steps {
                echo "🧨 Dropping flyway_schema_history table (for testing clean migration)..."
                sh '''
                    docker exec -i pg_test psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} \
                        -c "DROP TABLE IF EXISTS flyway_schema_history CASCADE;"
                '''
            }
        }

        stage('Build Flyway Image and Run Migration') {
            steps {
                echo "🐳 Building Flyway image and running migration..."
                sh '''
                    docker build -t flyway-migrations:${FLYWAY_VERSION} -f infrastructure/flyway/Dockerfile .

                    docker run --rm \
                        --network flyway-network \
                        flyway-migrations:${FLYWAY_VERSION} \
                        -url=jdbc:postgresql://pg_test:5432/${POSTGRES_DB} \
                        -user=${POSTGRES_USER} \
                        -password=${POSTGRES_PASSWORD} \
                        migrate
                '''
            }
        }

        stage('Show Flyway History') {
            steps {
                echo "📜 Showing Flyway migration history..."
                sh '''
                    docker run --rm \
                        --network flyway-network \
                        flyway-migrations:${FLYWAY_VERSION} \
                        -url=jdbc:postgresql://pg_test:5432/${POSTGRES_DB} \
                        -user=${POSTGRES_USER} \
                        -password=${POSTGRES_PASSWORD} \
                        info
                '''
            }
        }
    }

    post {
        always {
            echo "🧹 Cleaning up..."
            sh '''
                docker rm -f pg_test || true
                docker network rm flyway-network || true
            '''
        }
    }
}
