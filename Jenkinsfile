pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build'
            }
        }

        stage('Release') {
            when {
                allOf {
                    expression { env.BRANCH_NAME ==~ /^\d+\.\d+\.\d+$/ }
                    expression { !env.CHANGE_ID }
                }
            }

            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'github',
                            usernameVariable: 'GIT_USER',
                            passwordVariable: 'GIT_PASS'
                        )
                    ]) {
                        sh '''
                            set -e

                            echo "Release branch detected: ${BRANCH_NAME}"

                            if [ ! -f gradle.properties ]; then
                              echo "gradle.properties not found"
                              exit 1
                            fi

                            APP_VERSION=$(grep '^mod_version=' gradle.properties | cut -d= -f2 | tr -d ' ')

                            if [ -z "$APP_VERSION" ]; then
                              echo "mod_version not found"
                              exit 1
                            fi

                            TAG_NAME="${BRANCH_NAME}-v${APP_VERSION}"
                            echo "Creating tag: $TAG_NAME"

                            git config user.name "${GIT_USER}"
                            git tag -a "$TAG_NAME" -m "Release $TAG_NAME"
                            git push https://${GIT_USER}:${GIT_PASS}@github.com/InSearchOfName/book-writer.git "$TAG_NAME"

                            mkdir -p release-assets
                            cp build/libs/*.jar release-assets/

                            ARCHIVE_NAME="book-writer-${TAG_NAME}.tar.gz"
                            tar -czf "$ARCHIVE_NAME" release-assets

                            RELEASE_RESPONSE=$(curl -s -X POST \
                              -H "Accept: application/vnd.github.v3+json" \
                              -H "Authorization: token ${GIT_PASS}" \
                              https://api.github.com/repos/InSearchOfName/book-writer/releases \
                              -d "{
                                \\"tag_name\\": \\"${TAG_NAME}\\",
                                \\"name\\": \\"Release ${TAG_NAME}\\",
                                \\"body\\": \\"Automated release for ${TAG_NAME}\\"
                              }")

                            UPLOAD_URL=$(echo "$RELEASE_RESPONSE" \
                              | sed -n 's/.*"upload_url":[ ]*"\\([^"]*\\)".*/\\1/p' \
                              | sed 's/{.*}//')

                            if [ -z "$UPLOAD_URL" ]; then
                              echo "Failed to obtain upload_url"
                              echo "$RELEASE_RESPONSE"
                              exit 1
                            fi

                            curl -X POST \
                              -H "Authorization: token ${GIT_PASS}" \
                              -H "Content-Type: application/gzip" \
                              --data-binary @"$ARCHIVE_NAME" \
                              "${UPLOAD_URL}?name=${ARCHIVE_NAME}"

                            echo "Release ${TAG_NAME} published successfully"
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}

