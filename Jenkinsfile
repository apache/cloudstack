#!groovy
def now = new Date()
def date = now.format('yyyyMMdd', TimeZone.getTimeZone('UTC'))
def time = now.format('HHmmss', TimeZone.getTimeZone('UTC'))

def buildCommand() {
    return '''mkdir /tmp/build && \
        cp -r . /tmp/build && cd /tmp/build && \
        export ACS_BUILD_OPTS="-DskipTests=true -T 4 --batch-mode" && \
        git reset --hard && \
        git clean -f && \
        cd packaging && \
        sudo -E ./build-deb.sh -o ~/packages/$BRANCH_NAME -T'''
}

def publishCommand(String distro) {
    return '''cd /tmp/build && \
          export COMPONENT_NAME=$(echo $BRANCH_NAME | sed 's/[^0-9a-z]/-/gi') && \
          ./ci/publish.sh \
          -d ~/packages/$BRANCH_NAME \
          -a https://artifactory.devleaseweb.com/artifactory/cloudstack-apt/cloudstack \
          -u $USER -p $PASS -c $COMPONENT_NAME -i ''' + distro
}

parallel(
    jammy: {
        lswci([node: 'docker', mattermost: 'cloudstack-jenkins-build', notifyStagingOnly: false]) {
            withEnv(['DOCKER_BUILDKIT=1']) {
                lswWithDockerContainer(image: 'artifactory.devleaseweb.com/lswci/cloudstack:jammy') {
                    stage('Build package for jammy') {
                        sh(buildCommand())
                    }
                    stage('Publish package for jammy') {
                        withCredentials([usernamePassword(credentialsId: 'svc_jenkins', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                            sh(publishCommand('jammy'))
                        }
                    }
                }
            }
        }
    },
    focal: {
        lswci([node: 'docker', mattermost: 'cloudstack-jenkins-build', notifyStagingOnly: false]) {
            withEnv(['DOCKER_BUILDKIT=1']) {
                lswWithDockerContainer(image: 'artifactory.devleaseweb.com/lswci/cloudstack:focal') {
                    stage('Build package for focal') {
                        sh(buildCommand())
                    }
                    stage('Publish package for focal') {
                        withCredentials([usernamePassword(credentialsId: 'svc_jenkins', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                            sh(publishCommand('focal'))
                        }
                    }
                }
            }
        }
    },
    xenial: {
        lswci([node: 'docker', mattermost: 'cloudstack-jenkins-build', notifyStagingOnly: false]) {
            withEnv(['DOCKER_BUILDKIT=1']) {
                lswWithDockerContainer(image: 'artifactory.devleaseweb.com/lswci/cloudstack:xenial') {
                    stage('Build package for xenial') {
                        sh(buildCommand())
                    }
                    stage('Publish package for xenial') {
                        withCredentials([usernamePassword(credentialsId: 'svc_jenkins', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                            sh(publishCommand('xenial'))
                        }
                    }
                }
            }
        }
    }
)
