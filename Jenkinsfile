node("docker"){
    
    sh 'sudo chown -R jenkins:jenkins .'
	stage "SCM Checkout"
	checkout scm
	sh 'git clean -fdxe *.m2* && git reset HEAD --hard'
    
    stage: "Building Required Container"
	sh 'docker run --rm -i qstack-cloudstack-build:latest echo "exists" || make build_container'

	stage "Validation"
	sh 'make -B test'
	step([$class: 'CheckStylePublisher', pattern: '**/target/checkstyle-result.xml', usePreviousBuildAsReference: true])
    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
    step([$class: 'ArtifactArchiver', artifacts: '**/target/site/cobertura/coverage.xml']) 
	
}