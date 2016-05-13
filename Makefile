test:
	docker run --rm -vi ${CURDIR}:/repo qstack-cloudstack-build /bin/bash -c "cd /repo && MAVEN_OPTS='-Dmaven.repo.local=/repo/.m2' mvn -T16 install cobertura:cobertura"

bash:
	docker run --rm -vit ${CURDIR}:/repo qstack-cloudstack-build /bin/bash

build_container:
	docker build -t qstack-cloudstack-build buildcontainer

findbugs:
	mvn -T16 -DforkCount=4 findbugs:findbugs
