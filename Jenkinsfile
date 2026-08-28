pipeline {
agent any

environment {
    AWS_REGION = 'ap-south-1'
    ECR_REGISTRY = '417780656027.dkr.ecr.ap-south-1.amazonaws.com'
    ECR_REPOSITORY = 'spring-app'
    IMAGE_TAG = 'latest'
}

stages {

    stage('Checkout') {
        steps {
            checkout scm
        }
    }

    stage('Check Files') {
        steps {
            sh '''
                echo "===== WORKSPACE ====="
                pwd

                echo "===== FILES ====="
                ls -la

                echo "===== KUBERNETES FILES ====="
                find k8s -type f
            '''
        }
    }

    stage('Login to AWS ECR') {
        steps {
            sh 'aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}'
        }
    }

    stage('Build Docker Image') {
        steps {
            sh 'docker build -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG} .'
        }
    }

    stage('Push Docker Image') {
        steps {
            sh 'docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}'
        }
    }

    stage('Deploy to Staging') {
        steps {
            sh 'kubectl apply -f k8s/namespaces.yaml'
            sh 'kubectl apply -f k8s/k8s/deployment.yaml -n staging'
        }
    }

    stage('Check Staging Deployment') {
        steps {
            sh 'kubectl get pods -n staging'
            sh 'kubectl get deployment -n staging'
        }
    }

    stage('Production Approval') {
        steps {
            input message: 'Do you want to deploy to Production?', ok: 'Deploy'
        }
    }

    stage('Deploy to Production') {
        steps {
            sh 'kubectl apply -f k8s/k8s/deployment.yaml -n production'
        }
    }

    stage('Check Production Deployment') {
        steps {
            sh 'kubectl get pods -n production'
            sh 'kubectl get deployment -n production'
        }
    }
}

post {
    success {
        echo 'Pipeline completed successfully!'
    }

    failure {
        echo 'Pipeline failed. Check Console Output.'
    }

    always {
        echo 'Pipeline execution completed.'
    }
}

}
