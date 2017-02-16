#!/usr/bin/env groovy

if (env.BRANCH_NAME == 'master') {
  keepDays = 60
}
else {
  keepDays = 7
}

timeout(time: 30, unit: 'MINUTES'){
 node() {
  wrap([$class: 'TimestamperBuildWrapper']) {
    string currentBuild.result;
    //string mailerlist = 'rsood@firstfuel.com';

    stage('Code Checkout') {
      checkout([
        $class: 'GitSCM',
        branches: scm.branches,
        extensions: scm.extensions + [[$class: 'CleanCheckout']] +
          [[$class: 'LocalBranch', localBranch: "${env.BRANCH_NAME}" ]],
        userRemoteConfigs: scm.userRemoteConfigs
      ])
    }

    stage('Build Maven External Plugin') {
      try {
        sh 'mvn clean deploy -DskipTests -Dquality.check.skip=true'
        currentBuild.result = 'SUCCESS'
      }
      catch(e) {
        echo 'Build has failed'
        currentBuild.result = 'FAILURE'
        Notify()
        throw(e)
      }
    }
   
    stage ('Publish Console Output') {
      step([
        $class: 'WarningsPublisher',
        canComputeNew: false,
        canResolveRelativePaths: false,
        consoleParsers: [[
          parserName: 'Java Compiler (javac)'
        ]],
        defaultEncoding: '',
        excludePattern: '',
        healthy: '',
        includePattern: '',
        messagesPattern: '',
        unHealthy: '',
        canRunOnFailed: true
      ])
    }

    stage('Notify') {
      Notify()
    }

    stage('Clean up workspace') {
      // Clean up the WORKSPACE
      step([$class: 'WsCleanup'])
    }
  }
} 
}

def Notify() {
    try {
        println currentBuild.result
        if (currentBuild.result == 'SUCCESS'){
          colorcode = '#008000'
        }
        else {
          colorcode = '#FF0000'
        }
    }

    catch(e){
        println currentBuild.result
        throw e
      }

    withCredentials([[$class: 'StringBinding',
    credentialsId: 'SlackIntegrationToken',
    variable: 'SlackIntegrationToken']])
    {
    slackSend (channel: '#jenkins',
    color: "$colorcode",
    message: "Job '${currentBuild.result}' '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")

    step([$class: 'Mailer',
    notifyEveryBuild: true,
    //recipients: "${mailerlist}",
    //sendToIndividuals: true])
    recipients: emailextrecipients([[$class: 'CulpritsRecipientProvider'],
                   [$class: 'RequesterRecipientProvider']])])
  }
}

def reports() {
  step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true])
}

void withMavenEnv(List envVars = [], def body) {

    // Set MAVEN_HOME and special PATH variables for the tools we're
    // using.
    List mvnEnv = ["MAVEN_HOME=${M2}"]

    // Add any additional environment variables.
    mvnEnv.addAll(envVars)

    // Invoke the body closure we're passed within the environment we've created.
    withEnv(mvnEnv) {
        body.call()
    }
}
