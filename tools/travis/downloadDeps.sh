#Create dummy pom
echo '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"><modelVersion>4.0.0</modelVersion><groupId>org.apache.cloudstack</groupId><artifactId>travis-build-deps</artifactId><name>Download Deps for Travis CI</name><version>1</version><dependencies>' > pom.xml

#Get all dependency blocks
for line in $(find ../../ -name pom.xml -exec sed -n '/<dependencies>/{:a;n;/<\/dependencies>/b;p;ba}' {} \; | grep -e "artifactId" -e "groupId" -e "version" -e "dependency\>" -e "exclusion\>" -e "exclusions\>"); do

  #Tokenize values
  set -- $(echo $line | awk -v FS="(>|<)" '{print $2, $3}')

  #Start processing data

  if [ $1 == "dependency" ]; then
    #Create new artifact dep
    ARTIFACT=$line
  elif [ $1 == "/dependency" ]; then
    #Check if version is empty to fix maven 3.2.5 run
    if [[ $ARTIFACT != *version* ]]; then
      ARTIFACT="$ARTIFACT<version>LATEST</version>"
    fi
    #Filter out project modules interdependencies and noredist artifacts
    if [[ $ARTIFACT != *org.apache.cloudstack* ]] && [[ $ARTIFACT != *com.cloud* ]] && [[ $ARTIFACT != *org.midonet* ]] && [[ $ARTIFACT != *net.juniper* ]] ; then
	echo $ARTIFACT$line >> pom.xml
    fi
  elif [ $1 == "version" ]; then
    #If version is a maven var, get the value from parent pom
    if [[ $2 == \$\{* ]]; then

      VER=$(grep \<$(echo $2 | awk -v FS="(}|{)" '{print $2 }') ../../pom.xml | awk -v FS="(>|<)" '{print $3}')
      if [[ "$VER" == "" ]]; then
        ARTIFACT=org.apache.cloudstack
      else
        ARTIFACT="$ARTIFACT<version>$VER</version>"
      fi
    elif [[ "$2" == "" ]]; then
      ARTIFACT="$ARTIFACT<version>LATEST</version>"
    else
      ARTIFACT=$ARTIFACT$line
    fi
  else
    ARTIFACT=$ARTIFACT$line
  fi

done


#Finish dummy pom
echo "</dependencies></project>" >> pom.xml
