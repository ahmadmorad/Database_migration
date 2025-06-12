pipeline {
    agent any

    environment {
        LIQUIBASE_VERSION = '4.28.0'
        POSTGRES_VERSION  = '13'
        POSTGRES_USER     = 'postgres'
        POSTGRES_PASSWORD = 'password'
        DB_NAME           = 'liquibasedb'
        DB_PORT           = '5433'

        // Path adjustments
        CHANGELOG_DIR = 'app/src/main/resources/db/changelog'
        CHANGELOG_MOUNT_PATH  = '/liquibase/changelog'
    }

    stages {
        stage('Verify Files') {
            steps {
                sh """
                    echo "Changelog files:"
                    ls -la ${CHANGELOG_DIR}/
                """
            }
        }

        stage('Prepare network') {
            steps {
                sh 'docker network create ci-network || true'
            }
        }

        stage('Start PostgreSQL') {
            steps {
                sh """
                  docker run -d --name pg_test --network ci-network \
                    -e POSTGRES_DB=${DB_NAME} \
                    -e POSTGRES_USER=${POSTGRES_USER} \
                    -e POSTGRES_PASSWORD=${POSTGRES_PASSWORD} \
                    -p ${DB_PORT}:5432 postgres:${POSTGRES_VERSION}

                  for i in {1..30}; do
                    docker exec pg_test pg_isready -U ${POSTGRES_USER} && break
                    sleep 1
                  done
                """
            }
        }

        stage('Liquibase update') {
            steps {
                sh """
                  docker run --rm --network ci-network \
                    -v "${WORKSPACE}/${CHANGELOG_DIR}:${CHANGELOG_MOUNT_PATH}" \
                    liquibase/liquibase:${LIQUIBASE_VERSION} \
                    --url=jdbc:postgresql://pg_test:5432/${DB_NAME} \
                    --username=${POSTGRES_USER} \
                    --password=${POSTGRES_PASSWORD} \
                    --changeLogFile=${CHANGELOG_MOUNT_PATH}/master.xml \
                    --searchPath=${CHANGELOG_MOUNT_PATH} \
                    update
                """
            }
        }

        stage('Liquibase validate') {
            steps {
                sh """
                  docker run --rm --network ci-network \
                    -v "${WORKSPACE}/${CHANGELOG_DIR}:${CHANGELOG_MOUNT_PATH}" \
                    liquibase/liquibase:${LIQUIBASE_VERSION} \
                    --url=jdbc:postgresql://pg_test:5432/${DB_NAME} \
                    --username=${POSTGRES_USER} \
                    --password=${POSTGRES_PASSWORD} \
                    --changeLogFile=${CHANGELOG_MOUNT_PATH}/master.xml \
                    --searchPath=${CHANGELOG_MOUNT_PATH} \
                    validate
                """
            }
        }

        post {
            always {
                sh '''
                  docker rm -f pg_test || true
                  docker network rm ci-network || true
                '''
            }
        }
    }
}