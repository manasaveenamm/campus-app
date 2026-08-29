pipeline {
    agent any

    environment {
        AWS_REGION = "ap-south-1"
        ECR_REPO   = "417780656027.dkr.ecr.ap-south-1.amazonaws.com/campus-app"
        IMAGE_TAG  = "${BUILD_NUMBER}-${GIT_COMMIT ? GIT_COMMIT.take(7) : 'local'}"
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                sh 'mvn clean test package -B'
            }

            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -f Dockerfile -t ${ECR_REPO}:${IMAGE_TAG} ."
                sh "docker tag ${ECR_REPO}:${IMAGE_TAG} ${ECR_REPO}:latest"
            }
        }

        stage('Push to ECR') {
            steps {
                sh """
                    aws ecr get-login-password --region ${AWS_REGION} | \
                    docker login --username AWS --password-stdin \
                    417780656027.dkr.ecr.${AWS_REGION}.amazonaws.com

                    docker push ${ECR_REPO}:${IMAGE_TAG}
                    docker push ${ECR_REPO}:latest
                """
            }
        }

        stage('Deploy to Dev') {
            steps {
                sh """
                    kubectl set image deployment/campus-app \
                    campus-app=${ECR_REPO}:${IMAGE_TAG} \
                    -n dev

                    kubectl rollout status deployment/campus-app \
                    -n dev --timeout=120s
                """
            }
        }

        stage('Deploy to Staging') {
            steps {
                sh """
                    kubectl set image deployment/campus-app \
                    campus-app=${ECR_REPO}:${IMAGE_TAG} \
                    -n staging

                    kubectl rollout status deployment/campus-app \
                    -n staging --timeout=120s
                """
            }
        }

        stage('Smoke Test - Staging') {
            steps {
                sh '''
                    STAGING_URL=$(kubectl get svc campus-app-svc \
                        -n staging \
                        -o jsonpath='{.spec.clusterIP}')

                    echo "Staging Service IP: ${STAGING_URL}"

                    curl -f http://${STAGING_URL}/actuator/health || \
                    (echo "Smoke test failed" && exit 1)
                '''
            }
        }

        stage('Manual Approval for Production') {
            steps {
                input message: "Deploy build ${IMAGE_TAG} to PRODUCTION?",
                      ok: "Deploy"
            }
        }

        stage('Deploy to Production') {
            steps {
                sh """
                    kubectl set image deployment/campus-app \
                    campus-app=${ECR_REPO}:${IMAGE_TAG} \
                    -n production

                    kubectl rollout status deployment/campus-app \
                    -n production --timeout=180s
                """
            }
        }

        stage('Post-Deploy Health Check') {
            steps {
                sh '''
                    PROD_URL=$(kubectl get svc campus-app-svc \
                        -n production \
                        -o jsonpath='{.spec.clusterIP}')

                    echo "Production Service IP: ${PROD_URL}"

                    sleep 5

                    curl -f http://${PROD_URL}/actuator/health || \
                    (echo "Production health check failed" && exit 1)
                '''
            }
        }
    }

    post {

        failure {
            echo "Pipeline failed - attempting production rollback"

            sh '''
                kubectl rollout undo deployment/campus-app \
                -n production || true
            '''
        }

        success {
            echo "Release ${IMAGE_TAG} deployed successfully to production."
        }

        always {
            sh "docker rmi ${ECR_REPO}:${IMAGE_TAG} || true"
            sh "docker rmi ${ECR_REPO}:latest || true"
        }
    }
}
