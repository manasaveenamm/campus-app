```groovy
pipeline {
    agent any

    environment {
        AWS_REGION   = 'ap-south-1'
        ECR_REGISTRY = '417780656027.dkr.ecr.ap-south-1.amazonaws.com'
        ECR_REPO     = 'spring-app'
    }

    stages {

        stage('Checkout Code') {
            steps {
                checkout scm

                script {
                    // Create image tag after checkout
                    env.IMAGE_TAG = "${BUILD_NUMBER}-${sh(
                        script: 'git rev-parse --short=7 HEAD',
                        returnStdout: true
                    ).trim()}"

                    echo "Image Tag: ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Check Files') {
            steps {
                sh '''
                    echo "Current directory:"
                    pwd

                    echo "Files in workspace:"
                    ls -la

                    echo "Searching for pom.xml:"
                    find . -name pom.xml -type f
                '''
            }
        }

        stage('Unit Tests') {
            steps {
                script {
                    def pomPath = sh(
                        script: 'find . -name pom.xml -type f | head -1',
                        returnStdout: true
                    ).trim()

                    if (!pomPath) {
                        error("pom.xml not found in Jenkins workspace!")
                    }

                    def projectDir = sh(
                        script: "dirname '${pomPath}'",
                        returnStdout: true
                    ).trim()

                    echo "Maven project directory: ${projectDir}"

                    dir(projectDir) {
                        sh 'mvn clean test'
                    }
                }
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    sh """
                        aws ecr get-login-password --region ${AWS_REGION} | \
                        docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    """

                    sh """
                        docker build \
                        -t ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG} .
                    """

                    sh """
                        docker tag \
                        ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG} \
                        ${ECR_REGISTRY}/${ECR_REPO}:latest
                    """

                    sh """
                        docker push \
                        ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}
                    """

                    sh """
                        docker push \
                        ${ECR_REGISTRY}/${ECR_REPO}:latest
                    """
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                script {
                    sh 'kubectl apply -f k8s/namespaces.yaml'

                    sh """
                        sed -i 's|DOCKER_IMAGE_PLACEHOLDER|${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}|g' \
                        k8s/deployment.yaml
                    """

                    sh 'kubectl apply -f k8s/deployment.yaml -n staging'

                    sh """
                        kubectl rollout status \
                        deployment/campus-app \
                        -n staging \
                        --timeout=60s
                    """
                }
            }
        }

        stage('Manual Approval for Production') {
            steps {
                input(
                    message: 'Approve release to Production environment?',
                    ok: 'Deploy'
                )
            }
        }

        stage('Deploy to Production') {
            steps {
                script {
                    try {
                        sh 'kubectl apply -f k8s/deployment.yaml -n production'

                        sh """
                            kubectl rollout status \
                            deployment/campus-app \
                            -n production \
                            --timeout=60s
                        """

                    } catch (Exception e) {

                        echo "Deployment failed! Rolling back production release..."

                        sh """
                            kubectl rollout undo \
                            deployment/campus-app \
                            -n production
                        """

                        error(
                            "Rollback executed: Deployment was unhealthy."
                        )
                    }
                }
            }
        }
    }
}
```
