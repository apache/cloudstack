# Apache CloudStack Installation basics

This document describes how to develop, build, package and install Apache
CloudStack. For more information please refer to the official [documentation](http://docs.cloudstack.apache.org)
or the developer [wiki](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Home).

Apache CloudStack developers use various platforms for development, this guide
was tested against a CentOS 6.5 x86_64 setup.

* [Setting up development environment](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Setting+up+CloudStack+Development+Environment) for Apache CloudStack.
* [Building](https://cwiki.apache.org/confluence/display/CLOUDSTACK/How+to+build+CloudStack) Apache CloudStack.

## Setting up Development Environment

Install tools and dependencies used for development:

    $ yum install git ant ant-devel java-1.6.0-openjdk java-1.6.0-openjdk-devel
    mysql mysql-server tomcat6 mkisofs gcc python MySQL-python openssh-clients wget

    # yum -y update
    # yum -y install java-1.7.0-openjdk
    # yum -y install java-1.7.0-openjdk-devel
    # yum -y install mysql-server
    # yum -y install git
    # yum -y install genisoimage

Set up Maven (3.0.5):

    # wget http://www.us.apache.org/dist/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
    # tar -zxvf apache-maven-3.0.5-bin.tar.gz -C /usr/local
    # cd /usr/local
    # ln -s apache-maven-3.0.5 maven
    # echo export M2_HOME=/usr/local/maven >> ~/.bashrc # or .zshrc or .profile
    # echo export PATH=/usr/local/maven/bin:${PATH} >> ~/.bashrc # or .zshrc or .profile
    # source ~/.bashrc

Start the MySQL service:

    $ service mysqld start

## Getting the Source Code

You may get the source code from the repository hosted on Apache:

    $ git clone https://git-wip-us.apache.org/repos/asf/cloudstack.git

Or, you may fork the repository from the official Apache CloudStack mirror on [Github](https://github.com/apache/cloudstack)

To checkout a specific branch, for example 4.4, do:

    $ git fetch origin
    $ git checkout -b 4.4 origin/4.4

## Building

Clean and build:

    $ mvn clean install -P systemvm,developer

Clear old database (if any) and deploy the database schema:

    $ mvn -P developer -pl developer -Ddeploydb

Export the following variable if you need to run and debug the management server:

    $ export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=500m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"

Start the management server:

    $ mvn -pl :cloud-client-ui jetty:run

If this works, you've successfully setup a single server Apache CloudStack installation.

Open the following URL on your browser to access the Management Server UI:

    http://localhost:8080/client/

The default credentials are; user: admin, password: password and the domain
field should be left blank which is defaulted to the ROOT domain.

## Building with non-redistributable plugins

CloudStack supports several plugins that depend on libraries with distribution restrictions. 
Because of this they are not included in the default build. Enable these additional plugins 
activate their respective profiles. For convenience adding -Dnoredist will enable all plugins
that depend on libraries with distribution restrictions. The build procedure expects that the 
required libraries are present in the maven repository. 

The following procedure can be used to add the libraries to the local maven repository. Details 
on obtaining the required libraries can be found in this file. Note that this will vary between
releases of CloudStack

    $ cd deps
    $ ./install-non-oss.sh

To build all non redistributable components, add the noredist flag to the build command:

    $ mvn clean install -P systemvm,developer -Dnoredist

## Packaging and Installation

Before packaging, please make sure you go through the "Building" section above. This section describes packaging and installation.

### Debian/Ubuntu

To create debs install the following extra packages:

    # apt-get -y install python-mysqldb
    # apt-get -y install debhelper
    # apt-get -y install tomcat6

Then:

    $ mvn -P deps # -D noredist, for noredist as described in the "Building" section above
    $ dpkg-buildpackage -uc -us

All the deb packages will be located one level down.

### RHEL/CentOS

To create rpms, install the following extra packages:

    # yum -y install rpm-build
    # yum -y install tomcat6
    # yum -y install ws-commons-util
    # yum -y instal gcc
    # yum -y install glibc-devel
    # yum -y install MySQL-python

Then:

    $ cd packaging/centos63
    $ package.sh

To create packages for noredist add the `-p noredist` option to the package script.
All the rpm packages will be created in `dist/rpmbuild/RPMS/x86_64` directory.

## Notes

If you will be using Xen as your hypervisor, please download [vhd-util](http://download.cloud.com.s3.amazonaws.com/tools/vhd-util)

If management server is installed on RHEL/CentOS, then copy vhd-util into:

    /usr/lib64/cloud/common/scripts/vm/hypervisor/xenserver/

If management server is installed on Ubuntu, then put vhd-util into:

    /usr/lib/cloud/common/scripts/vm/hypervisor/xenserver/vhd-util
