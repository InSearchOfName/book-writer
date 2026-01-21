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
                        ),
                        string(
                            credentialsId: 'modrinth-token',
                            variable: 'MODRINTH_TOKEN'
                        )
                    ]) {
                        sh '''
                            set -e

                            echo "Release branch detected: ${BRANCH_NAME}"

                            APP_VERSION=$(grep '^mod_version=' gradle.properties | cut -d= -f2 | tr -d ' ')
                            if [ -z "$APP_VERSION" ]; then
                            echo "mod_version not found"
                            exit 1
                            fi

                            TAG_NAME="${BRANCH_NAME}-v${APP_VERSION}"
                            echo "Tag: $TAG_NAME"

                            # ---- Locate build artifacts (SINGLE SOURCE OF TRUTH) ----
                            MAIN_JAR=$(ls build/libs/*-${APP_VERSION}.jar | grep -v sources)
                            SOURCES_JAR=$(ls build/libs/*-${APP_VERSION}-sources.jar)

                            echo "Main JAR: $MAIN_JAR"
                            echo "Sources JAR: $SOURCES_JAR"

                            # ---- Git tag ----
                            git config user.name "${GIT_USER}"
                            git tag -a "$TAG_NAME" -m "Release $TAG_NAME"
                            git push https://${GIT_USER}:${GIT_PASS}@github.com/InSearchOfName/book-writer.git "$TAG_NAME"

                            # ---- GitHub Release ----
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
                            echo "Failed to obtain GitHub upload_url"
                            echo "$RELEASE_RESPONSE"
                            exit 1
                            fi

                            # Upload BOTH assets to GitHub
                            for FILE in "$MAIN_JAR" "$SOURCES_JAR"; do
                            NAME=$(basename "$FILE")
                            curl --fail -X POST \
                                -H "Authorization: token ${GIT_PASS}" \
                                -H "Content-Type: application/java-archive" \
                                --data-binary @"$FILE" \
                                "${UPLOAD_URL}?name=${NAME}"
                            done

                            # ---- Modrinth Release (SAME FILES) ----
                            curl --fail --location 'https://api.modrinth.com/v2/version' \
                            --header "Authorization: ${MODRINTH_TOKEN}" \
                            --form 'data={
                                "name": "Version '"${APP_VERSION}"'",
                                "version_number": "'"${APP_VERSION}"'",
                                "changelog": "Automated release '"${TAG_NAME}"'",
                                "dependencies": [{"project_id":"P7dR8mSH","dependency_type":"required"}],
                                "game_versions": ["'"${BRANCH_NAME}"'"],
                                "version_type": "release",
                                "loaders": ["fabric"],
                                "featured": true,
                                "status": "listed",
                                "requested_status": null,
                                "project_id": "yn4qgdpm",
                                "file_parts": ["main", "sources"],
                                "primary_file": "main"
                            }' \
                            --form "main=@${MAIN_JAR}" \
                            --form "sources=@${SOURCES_JAR}"

                            echo "Release ${TAG_NAME} published to GitHub AND Modrinth"
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

