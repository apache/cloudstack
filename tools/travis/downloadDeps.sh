#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# This script should be used to install additional dependencies
# This includes: installing ubuntu packages, custom services
# or internet downloads.

# Authored by Rafael da Fonseca <rsafonseca@gmail.com>

#Get all dependency blocks from all pom.xml files in the project
for line in $(find ../../ -name pom.xml -exec sed -n '/<dependencies>/{:a;n;/<\/dependencies>/b;p;ba}' {} \; | grep -e "artifactId" -e "groupId" -e "version" -e "dependency\>" -e "exclusion\>" -e "exclusions\>"| sed -e 's/\^M//'); do

  #Tokenize values
  set -- $(echo $line | awk -v FS="(>|<)" '{print $2, $3}')

  #Start processing data
  if [[ $1 == "dependency" ]]; then
    #Create new artifact dep
    unset ARTIFACT
    unset VERSION
    unset GROUP
  elif [[ $1 == "/dependency" ]]; then
    #Filter out project modules interdependencies
    if [[ $GROUP != *org.apache.cloudstack* ]] && [[ $GROUP != *com.cloud* ]] && [[ $ARTIFACT != cloudstack-service-console-proxy-rdpclient ]]; then
            if [[ -z $VERSION ]] ; then
               VERSION=LATEST
               #These dependencies don't support the LATEST keywork for some reason, and would cause mvn runs to file on dummy poms
               if [[ $GROUP == jstl ]] || [[ $ARTIFACT == mysql-connector-java ]] || [[ $GROUP == org.apache.axis ]]; then
                 continue
               fi
            fi
            #Output resolved dependency to a file, to be picked up later
            echo "$GROUP $ARTIFACT $VERSION" >> deps.out
    fi
  elif [[ $1 == "version" ]]; then
    #If version is a maven var, get the value from parent pom
    if [[ $2 == \$\{* ]]; then
      VERSION=$(grep \<$(echo $2 | awk -v FS="(}|{)" '{print $2 }') ../../pom.xml | awk -v FS="(>|<)" '{print $3}')
    #If version tag is empty, add LATEST to avoid maven errors
    elif [[ "$2" == "" ]]; then
      VERSION="LATEST"
    else
      VERSION=$2
    fi
  elif [[ $1 == "artifactId" ]]; then
    #This avoids exclusions inside dependency block to overwrite original dependency
    if [[ -z $ARTIFACT ]]; then
      ARTIFACT=$2
    fi
  elif [[ $1 == "groupId" ]]; then
    #This avoids exclusions inside dependency block to overwrite original dependency
     if [[ -z $GROUP ]]; then
       GROUP=$2
     fi
  fi
done

#Add the resolved plugins to properly download their dependencies
while read line ; do
    NAME=$(echo $line | sed -e 's/.jar$//')
    VERSION=${NAME##*-}
    ARTIFACT=${NAME%-*}
    GROUP=$(find ~/.m2/repository -name ${NAME}.pom -exec sed -n "1,/${ARTIFACT}/p" {} \; | tac | grep -m 1 -e "<groupId>"  | sed -e 's/^[[:space:]]*//' -e 's/\^M//' | tr -d '\r' | awk -v FS="(>|<)" '{print $3}')
    DATA="${GROUP} ${ARTIFACT} ${VERSION}"
    echo $DATA >> deps.out
done < /tmp/resolvedPlugins

#Remove duplicates and sort them, LANG export is needed to fix some sorting issue, sorting is needed for later function that relies on sorted input
cat deps.out | LANG=C sort -u > cleandeps.out

#Define index of pomfiles, to avoid duplicate deps with different versions in pom.xml, several poms are created in case of more than one version of same artifact
LASTPOM=0
#Create first pom
echo '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"><modelVersion>4.0.0</modelVersion><groupId>org.apache.cloudstack</groupId><artifactId>travis-build-deps</artifactId><name>Download Deps for Travis CI</name><version>1</version><dependencies>' > pom${LASTPOM}.xml
#Create pom for dependencies not on central repo, this is done separately to not adversely impact performance on downloading the majority of deps
echo '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"><modelVersion>4.0.0</modelVersion><groupId>org.apache.cloudstack</groupId><artifactId>travis-build-deps</artifactId><name>Download Deps for Travis CI</name><version>1</version><repositories><repository><id>mido-maven-public-releases</id><name>mido-maven-public-releases</name><url>http://cs-maven.midokura.com/releases</url></repository><repository><id>juniper-contrail</id><url>http://juniper.github.io/contrail-maven/snapshots</url></repository></repositories><dependencies>' > pomX.xml
while read line ; do
  set -- $line
  #This relies on correct sorting, and distributes different versions of same dependency througout different pom files
  if [[ $2 == $LASTARTIFACT ]]; then
    POMID=$(($POMID+1))
    #If value is greater than current number of poms, create a new one
    if [[ $POMID -gt $LASTPOM ]]; then
       LASTPOM=$POMID
       #This outputs the necessary structure to start a pom and also defines the extra repositories
       echo '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"><modelVersion>4.0.0</modelVersion><groupId>org.apache.cloudstack</groupId><artifactId>travis-build-deps</artifactId><name>Download Deps for Travis CI</name><version>1</version><dependencies>' > pom${LASTPOM}.xml
    fi
  else
    POMID=0
  fi
  LASTARTIFACT=$2
  if [[ $1 == org.midonet ]] || [[ $1 == net.juniper* ]]; then
    echo "<dependency><groupId>$1</groupId><artifactId>$2</artifactId><version>$3</version></dependency>" >> pomX.xml
  else
    echo "<dependency><groupId>$1</groupId><artifactId>$2</artifactId><version>$3</version></dependency>" >> pom${POMID}.xml
  fi
done < cleandeps.out

RETURN_CODE=0
#Close and resolve all pom files
for ((i=0;i<=$LASTPOM;i++))
do
  echo "</dependencies></project>" >> pom${i}.xml
  mvn org.apache.maven.plugins:maven-dependency-plugin:resolve -f pom${i}.xml
  if [[ $? -ne 0 ]]; then
    RETURN_CODE=1
  fi
done
#Close and resolve external deps pom file
echo "</dependencies></project>" >> pomX.xml
mvn org.apache.maven.plugins:maven-dependency-plugin:resolve -f pomX.xml

#Run a few plugin goals to download some more deps

#Hack to run maven-jaxb2-plugin generate, can be removed when we stop using such an old version...
mkdir -p src/main/resources
echo '<?xml version="1.0" encoding="utf-8" ?><xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"></xs:schema>' > src/main/resources/test.xsd

#Declare plugin tests to run
declare -a arr=("maven-surefire-plugin test" "maven-pmd-plugin pmd" "maven-compiler-plugin compile" "maven-resources-plugin resources" "maven-checkstyle-plugin check" "maven-site-plugin attach-descriptor" "maven-surefire-plugin test" "maven-jar-plugin jar" "license-maven-plugin check" "maven-jgit-buildnumber-plugin extract-buildnumber" "maven-jaxb2-plugin generate" "maven-war-plugin war -DfailOnMissingWebXml=false" "gmaven-plugin compile")
for i in "${arr[@]}"
do
    set -- $i
    PLUGIN=$1
    MOJO=$2
    OPTION=$3
    #Get every listed version of the plugin and make a run for each version
    while read line ; do
       set -- $line
       JOBS="${JOBS} ${1}:${2}:${3}:${MOJO} $OPTION"
    done < <(grep $PLUGIN cleandeps.out)
done
echo "Running $JOBS"
#Call all the constructed plugin goals
mvn $JOBS -f pom0.xml
if [[ $? -ne 0 ]]; then
  RETURN_CODE=1
fi

#Cleanup some files created in the run
rm -rf deps.out src target

exit $RETURN_CODE
