pipeline {
    agent any

    environment {
        FLYWAY_VERSION = '8.5.0'
        DB_SERVICE = 'db'
        DB_IMAGE = 'postgres:13'
        DB_USER = 'postgres'
        DB_PASS = 'password'
        DB_NAME = 'mydb'
    }

    stages {
        stage('Start PostgreSQL') {
            steps {
                sh 'docker-compose up -d db'

                // Wait until DB is ready
                sh '''
                echo "Warte auf Datenbank..."
                for i in {1..10}; do
                  docker exec $(docker-compose ps -q db) pg_isready -U $DB_USER && break
                  sleep 2
                done
                '''
            }
        }

        stage('Run Flyway Migration') {
            steps {
                sh '''
                docker run --rm \
                  --network ${COMPOSE_PROJECT_NAME}_default \
                  -v $WORKSPACE/src/main/resources/db/migration:/flyway/sql \
                  -e FLYWAY_URL=jdbc:postgresql://db:5432/$DB_NAME \
                  -e FLYWAY_USER=$DB_USER \
                  -e FLYWAY_PASSWORD=$DB_PASS \
                  flyway/flyway:$FLYWAY_VERSION migrate
                '''
            }
        }

        stage('Validate Migrations') {
            steps {
                sh '''
                docker run --rm \
                  --network ${COMPOSE_PROJECT_NAME}_default \
                  -v $WORKSPACE/src/main/resources/db/migration:/flyway/sql \
                  -e FLYWAY_URL=jdbc:postgresql://db:5432/$DB_NAME \
                  -e FLYWAY_USER=$DB_USER \
                  -e FLYWAY_PASSWORD=$DB_PASS \
                  flyway/flyway:$FLYWAY_VERSION validate
                '''
            }
        }
    }

    post {
        always {
            echo 'Stoppe und entferne Container'
            sh 'docker-compose down'
        }
    }
}
