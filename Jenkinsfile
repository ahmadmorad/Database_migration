pipeline {
    agent any                       // works on any node with Docker CLI

    environment {
        /* ===== Versions & DB settings ===== */
        LIQUIBASE_VERSION = '4.28.0'          // pin Liquibase image tag
        POSTGRES_VERSION  = '13'
        POSTGRES_USER     = 'postgres'
        POSTGRES_PASSWORD = 'password'
        DB_NAME           = 'liquibasedb'
        DB_PORT           = '5433'            // external port on Jenkins node

        /* ===== Changelog location in repo ===== */
        CHANGELOG_FILE    = 'app/src/main/resources/db/changelog/master.xml'
    }

    stages {
        stage('Debug workspace') {
            steps { sh 'ls -R | head -40' }      // list first 40 lines of the tree
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

                    # Wait until Postgres answers
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
                 docker run --rm --network ci-network \\
                   -v "\$PWD/${CHANGELOG_DIR}":/liquibase/changelog \\
                   liquibase/liquibase:${LIQUIBASE_VERSION} \\
                   --url=jdbc:postgresql://pg_test:5432/${DB_NAME} \\
                   --username=${POSTGRES_USER} \\
                   --password=${POSTGRES_PASSWORD} \\
                   --changeLogFile=changelog/master.xml \\
                   update
               """
           }
       }

        stage('Liquibase validate') {
            steps {
                sh """
                    docker run --rm --network ci-network -v "\$PWD":/workspace \
                      liquibase/liquibase:${LIQUIBASE_VERSION} \
                      --url=jdbc:postgresql://pg_test:5432/${DB_NAME} \
                      --username=${POSTGRES_USER} \
                      --password=${POSTGRES_PASSWORD} \
                      --changeLogFile=/workspace/${CHANGELOG_FILE} \
                      validate
                """
            }
        }

        stage('Run Maven tests') {
            steps {
                sh './mvnw -B test'
            }
        }
    }

    post {
        always {
            sh '''
              echo "Cleaning up Docker resources..."
              docker rm -f pg_test || true
              docker network rm ci-network || true
            '''
        }
    }
}
