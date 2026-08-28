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

    stage('Build') {
        steps {
            sh 'mvn clean package -DskipTests'
        }
    }

    stage('Test') {
        steps {
            sh 'mvn test'
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
            sh 'kubectl apply -f k8s/deployment.yaml -n staging'
        }
    }

    stage('Check Staging Deployment') {
        steps {
            sh 'kubectl rollout status deployment/campus-app -n staging --timeout=60s'
        }
    }

    stage('Production Approval') {
        steps {
            input message: 'Do you want to deploy to Production?', ok: 'Deploy'
        }
    }

    stage('Deploy to Production') {
        steps {
            sh 'kubectl apply -f k8s/deployment.yaml -n production'
        }
    }

    stage('Check Production Deployment') {
        steps {
            sh 'kubectl rollout status deployment/campus-app -n production --timeout=60s'
        }
    }
}

post {
    success {
        echo 'Pipeline completed successfully!'
    }

    failure {
        echo 'Pipeline failed. Check the Console Output.'
    }

    always {
        echo 'Pipeline execution completed.'
    }
}

}
