pipeline {
agent any

environment {
    AWS_REGION = 'ap-south-1'
    ECR_REGISTRY = '417780656027.dkr.ecr.ap-south-1.amazonaws.com'
    ECR_REPO = 'spring-app'
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
                echo "===== CURRENT DIRECTORY ====="
                pwd

                echo "===== FILES ====="
                ls -la

                echo "===== POM FILE ====="
                find . -name pom.xml -type f

                echo "===== DOCKERFILE ====="
                find . -name Dockerfile -type f

                echo "===== YAML FILES ====="
                find . -name "*.yaml" -type f
            '''
        }
    }

    stage('Unit Tests') {
        steps {
            script {
                def pomFile = sh(
                    script: 'find . -name pom.xml -type f | head -1',
                    returnStdout: true
                ).trim()

                if (pomFile == '') {
                    error('ERROR: pom.xml not found')
                }

                def projectDir = sh(
                    script: "dirname '${pomFile}'",
                    returnStdout: true
                ).trim()

                echo "Maven directory: ${projectDir}"

                dir(projectDir) {
                    sh 'mvn clean test'
                }
            }
        }
    }

    stage('Set Image Tag') {
        steps {
            script {
                def commitId = sh(
                    script: 'git rev-parse --short HEAD',
                    returnStdout: true
                ).trim()

                env.IMAGE_TAG = "${BUILD_NUMBER}-${commitId}"

                echo "IMAGE TAG: ${env.IMAGE_TAG}"
            }
        }
    }

    stage('Login to ECR') {
        steps {
            sh '''
                aws ecr get-login-password \
                --region ${AWS_REGION} | \
                docker login \
                --username AWS \
                --password-stdin ${ECR_REGISTRY}
            '''
        }
    }

    stage('Build Docker Image') {
        steps {
            sh '''
                docker build \
                -t ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG} .
            '''
        }
    }

    stage('Push Docker Image') {
        steps {
            sh '''
                docker push \
                ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}
            '''

            sh '''
                docker tag \
                ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG} \
                ${ECR_REGISTRY}/${ECR_REPO}:latest
            '''

            sh '''
                docker push \
                ${ECR_REGISTRY}/${ECR_REPO}:latest
            '''
        }
    }

    stage('Deploy to Staging') {
        steps {
            sh '''
                kubectl apply \
                -f k8s/namespaces.yaml
            '''

            sh '''
                sed -i "s|DOCKER_IMAGE_PLACEHOLDER|${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}|g" \
                k8s/deployment.yaml
            '''

            sh '''
                kubectl apply \
                -f k8s/deployment.yaml \
                -n staging
            '''

            sh '''
                kubectl rollout status \
                deployment/campus-app \
                -n staging \
                --timeout=60s
            '''
        }
    }

    stage('Manual Approval') {
        steps {
            input(
                message: 'Approve release to Production?',
                ok: 'Deploy'
            )
        }
    }

    stage('Deploy to Production') {
        steps {
            script {
                try {
                    sh '''
                        kubectl apply \
                        -f k8s/deployment.yaml \
                        -n production
                    '''

                    sh '''
                        kubectl rollout status \
                        deployment/campus-app \
                        -n production \
                        --timeout=60s
                    '''

                } catch (Exception e) {

                    echo 'Production deployment failed.'

                    sh '''
                        kubectl rollout undo \
                        deployment/campus-app \
                        -n production
                    '''

                    error('Rollback completed.')
                }
            }
        }
    }
}

}
