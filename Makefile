test:
	mvn -T16 clean install cobertura:cobertura

bash:
	docker run --rm -vit ${CURDIR}:/repo qstack-cloudstack-build /bin/bash

build_container:
	docker build -t qstack-cloudstack-build buildcontainer

findbugs:
	mvn -T16 -DforkCount=4 findbugs:findbugs