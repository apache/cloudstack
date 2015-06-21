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
    #Filter out project modules interdependencies and noredist artifacts
    if [[ $GROUP != *org.apache.cloudstack* ]] && [[ $GROUP != *com.cloud* ]] && [[ $ARTIFACT != cloudstack-service-console-proxy-rdpclient ]]; then
            if [[ -z $VERSION ]] ; then
               VERSION=LATEST
               if [[ $GROUP == jstl ]] || [[ $ARTIFACT == mysql-connector-java ]] || [[ $GROUP == org.apache.axis ]]; then
                 continue
               fi
            fi
            echo "$GROUP $ARTIFACT $VERSION" >> deps.out
    fi
  elif [[ $1 == "version" ]]; then
    #If version is a maven var, get the value from parent pom
    if [[ $2 == \$\{* ]]; then
      VERSION=$(grep \<$(echo $2 | awk -v FS="(}|{)" '{print $2 }') ../../pom.xml | awk -v FS="(>|<)" '{print $3}')
    elif [[ "$2" == "" ]]; then
      VERSION="LATEST"
    else
      VERSION=$2
    fi
  elif [[ $1 == "artifactId" ]]; then
    if [[ -z $ARTIFACT ]]; then
      ARTIFACT=$2
    fi
  elif [[ $1 == "groupId" ]]; then
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

cat deps.out | LANG=C sort -u > cleandeps.out

LASTPOM=0
echo '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"><modelVersion>4.0.0</modelVersion><groupId>org.apache.cloudstack</groupId><artifactId>travis-build-deps</artifactId><name>Download Deps for Travis CI</name><version>1</version><repositories><repository><id>mido-maven-public-releases</id><name>mido-maven-public-releases</name><url>http://cs-maven.midokura.com/releases</url></repository><repository><id>juniper-contrail</id><url>http://juniper.github.io/contrail-maven/snapshots</url></repository></repositories><dependencies>' > pom${LASTPOM}.xml
while read line ; do
  set -- $line

  if [[ $2 == $LASTARTIFACT ]]; then
    POMID=$(($POMID+1))
    if [[ $POMID -gt $LASTPOM ]]; then
       LASTPOM=$POMID
       echo '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"><modelVersion>4.0.0</modelVersion><groupId>org.apache.cloudstack</groupId><artifactId>travis-build-deps</artifactId><name>Download Deps for Travis CI</name><version>1</version><repositories><repository><id>mido-maven-public-releases</id><name>mido-maven-public-releases</name><url>http://cs-maven.midokura.com/releases</url></repository><repository><id>juniper-contrail</id><url>http://juniper.github.io/contrail-maven/snapshots</url></repository></repositories><dependencies>' > pom${LASTPOM}.xml
    fi
  else
    POMID=0
  fi
  LASTARTIFACT=$2
  echo "<dependency><groupId>$1</groupId><artifactId>$2</artifactId><version>$3</version></dependency>" >> pom${POMID}.xml

done < cleandeps.out

RETURN_CODE=0
for ((i=0;i<=$LASTPOM;i++))
do
  echo "</dependencies></project>" >> pom${i}.xml
  mvn org.apache.maven.plugins:maven-dependency-plugin:resolve -f pom${i}.xml
  if [[ $? -ne 0 ]]; then
    RETURN_CODE=1
  fi

done

#Run a few plugin goals to download some more deps

#Hack to run maven-jaxb2-plugin generate, can be removed when we stop using such an old version...
mkdir -p src/main/resources
echo '<?xml version="1.0" encoding="utf-8" ?><xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"></xs:schema>' > src/main/resources/test.xsd


declare -a arr=("maven-surefire-plugin test" "maven-pmd-plugin pmd" "maven-compiler-plugin compile" "maven-resources-plugin resources" "maven-checkstyle-plugin check" "maven-site-plugin attach-descriptor" "maven-surefire-plugin test" "maven-jar-plugin jar" "license-maven-plugin check" "maven-jgit-buildnumber-plugin extract-buildnumber" "maven-jaxb2-plugin generate" "maven-war-plugin war -DfailOnMissingWebXml=false" "gmaven-plugin compile")
for i in "${arr[@]}"
do
    set -- $i
    PLUGIN=$1
    MOJO=$2
    OPTION=$3
    while read line ; do
       set -- $line
       JOBS="${JOBS} ${1}:${2}:${3}:${MOJO} $OPTION"
    done < <(grep $PLUGIN cleandeps.out)
done
echo "Running $JOBS"
mvn $JOBS -f pom0.xml


rm -rf deps.out src target

exit $RETURN_CODE
