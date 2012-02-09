#!/bin/sh

# wec
export CATALINA_HOME=${HOME}/automated
# macos tomcat
#ORIG_TOMCAT=/usr/local/tomcat
# linux/jenkins host tomcat
ORIG_TOMCAT=/usr/share/tomcat6

mkdir_copy_files() {
    if [ -z "$1" ]; then
	return 0
    fi

    echo "Copying $1 files to $2..."

    mkdir -p $2
    cp -R $1/* $2
    return $?
}

if [ ! -d ${ORIG_TOMCAT} ]; then
    echo "Tomcat must be installed on this system"
    exit 1
fi

if [ -d ${CATALINA_HOME} ]; then
    echo "Existing test Tomcat exists!!!"
    exit 1
fi

# now let's copy over the required files...
mkdir_copy_files ${ORIG_TOMCAT}/conf ${CATALINA_HOME}/conf
mkdir_copy_files ${ORIG_TOMCAT}/bin ${CATALINA_HOME}/bin
mkdir_copy_files ${ORIG_TOMCAT}/lib ${CATALINA_HOME}/lib
mkdir_copy_files ${ORIG_TOMCAT}/logs ${CATALINA_HOME}/logs
mkdir_copy_files ${ORIG_TOMCAT}/temp ${CATALINA_HOME}/temp
mkdir_copy_files ${ORIG_TOMCAT}/webapps ${CATALINA_HOME}/webapps
mkdir_copy_files ${ORIG_TOMCAT}/work ${CATALINA_HOME}/work

ant clean-all

ant automated-test-run

# clean up our temp tomcat!
rm -rf ${CATALINA_HOME}

exit $?
