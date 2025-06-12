pipeline {
    agent any   // any node with Docker CLI

    environment {
        /* ===== Versions & DB settings ===== */
        LIQUIBASE_VERSION = '4.28.0'
        POSTGRES_VERSION  = '13'
        POSTGRES_USER     = 'postgres'
        POSTGRES_PASSWORD = 'password'
        DB_NAME           = 'liquibasedb'
        DB_PORT           = '5433'        // mapped port on the agent

        /* ===== Changelog location in repo ===== */
        CHANGELOG_DIR              = 'app/src/main/resources/db/changelog'
        CHANGELOG_FILE_IN_CONTAINER = '/liquibase/changelog/master.xml'  // Fixed path with leading slash
    }

    stages {
        /* Verify workspace structure */
        stage('Verify Files Exist') {
            steps {
                sh """
                    echo "Checking if changelog files exist..."
                    ls -la ${CHANGELOG_DIR}/master.xml
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

                  # Wait for PostgreSQL to be ready
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
                    -v "${WORKSPACE}/${CHANGELOG_DIR}":/liquibase/changelog \
                    liquibase/liquibase:${LIQUIBASE_VERSION} \
                    --url=jdbc:postgresql://pg_test:5432/${DB_NAME} \
                    --username=${POSTGRES_USER} \
                    --password=${POSTGRES_PASSWORD} \
                    --changeLogFile=${CHANGELOG_FILE_IN_CONTAINER} \
                    update
                """
            }
        }

        stage('Liquibase validate') {
            steps {
                sh """
                  docker run --rm --network ci-network \
                    -v "${WORKSPACE}/${CHANGELOG_DIR}":/liquibase/changelog \
                    liquibase/liquibase:${LIQUIBASE_VERSION} \
                    --url=jdbc:postgresql://pg_test:5432/${DB_NAME} \
                    --username=${POSTGRES_USER} \
                    --password=${POSTGRES_PASSWORD} \
                    --changeLogFile=${CHANGELOG_FILE_IN_CONTAINER} \
                    validate
                """
            }
        }

        stage('Run Maven tests') {
            steps {
                sh './app/mvnw -B test'
            }
        }
    }

    post {
        always {
            sh '''
              docker rm -f pg_test || true
              docker network rm ci-network || true
            '''
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully'
        }
        failure {
            echo 'Pipeline failed'
        }
    }
}