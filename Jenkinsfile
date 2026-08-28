pipeline {
    agent any
    
    environment {
        AWS_REGION     = 'ap-south-1'
        ECR_REGISTRY   = '417780656027.dkr.ecr.ap-south-1.amazonaws.com'
        ECR_REPO       = 'spring-app'
        IMAGE_TAG      = "${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }
        stage('Check Files') {
    steps {
        sh '''
            pwd
            ls -la
            find . -name pom.xml
        '''
    }
}
        
        stage('Unit Tests') {
            steps {
                sh 'mvn clean test'
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    sh "aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
                    sh "docker build -t ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG} ."
                    sh "docker tag ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG} ${ECR_REGISTRY}/${ECR_REPO}:latest"
                    sh "docker push ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}"
                    sh "docker push ${ECR_REGISTRY}/${ECR_REPO}:latest"
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                script {
                    sh "kubectl apply -f k8s/namespaces.yaml"
                    sh "sed -i 's|DOCKER_IMAGE_PLACEHOLDER|'${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}'|g' k8s/deployment.yaml"
                    sh "kubectl apply -f k8s/deployment.yaml -n staging"
                }
            }
        }

        stage('Manual Approval for Production') {
            steps {
                input message: 'Approve release to Production environment?', ok: 'Deploy'
            }
        }

        stage('Deploy to Production') {
            steps {
                script {
                    try {
                        sh "kubectl apply -f k8s/deployment.yaml -n production"
                        sh "kubectl rollout status deployment/campus-app -n production --timeout=60s"
                    } catch (Exception e) {
                        echo "Deployment failed! Rolling back production release..."
                        sh "kubectl rollout undo deployment/campus-app -n production"
                        error("Rollback executed: Deployment was unhealthy.")
                    }
                }
            }
        }
    }
}
