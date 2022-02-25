# Apache CloudStack Installation basics

This document describes how to develop, build, package and install Apache
CloudStack. For more information please refer to the official [documentation](http://docs.cloudstack.apache.org)
or the developer [wiki](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Home).

Apache CloudStack developers use various platforms for development, this guide
was tested against a CentOS 7 x86_64 setup.

* [Setting up development environment](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Setting+up+CloudStack+Development+Environment) for Apache CloudStack.
* [Building](https://cwiki.apache.org/confluence/display/CLOUDSTACK/How+to+build+CloudStack) Apache CloudStack.
* [Appliance based development](https://github.com/rhtyd/monkeybox)

## Setting up Development Environment

Install tools and dependencies used for development:

    # yum -y install git java-11-openjdk java-11-openjdk-devel \
      mysql mysql-server mkisofs git gcc python MySQL-python openssh-clients wget

Set up Maven (3.6.0):

    # wget http://www.us.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
    # tar -zxvf apache-maven-3.6.3-bin.tar.gz -C /usr/local
    # cd /usr/local
    # ln -s apache-maven-3.6.3 maven
    # echo export M2_HOME=/usr/local/maven >> ~/.bashrc # or .zshrc or .profile
    # echo export PATH=/usr/local/maven/bin:${PATH} >> ~/.bashrc # or .zshrc or .profile
    # source ~/.bashrc

Setup up NodeJS (LTS):

    # curl -sL https://rpm.nodesource.com/setup_12.x | sudo bash -
    # sudo yum install nodejs
    # sudo npm install -g @vue/cli npm-check-updates

Start the MySQL service:

    $ service mysqld start

### Using jenv and/or pyenv for Version Management

CloudStack is built using Java and Python.  To make selection of these tools versions more consistent and ease installation for developers, optional support for [jenv](http://www.jenv.be/) and [pyenv](https://github.com/yyuu/pyenv) with [virtualenv]|(https://github.com/yyuu/pyenv-virtualenv) is provided.  jenv installation instructions are available here and pyenv installation instructions are available here.  For users of [oh-my-zsh](http://ohmyz.sh/) there is a pyenv plugin available to trigger configuration of pyenv in a shell session.

Following installation, execute the following commands to configure jenv and pyenv for use with CloudStack development:

    # pyenv install 2.7.16                                          ## Install Python 2.7.16
    # pyenv virtualenv 2.7.16 cloudstack                            ## Create a cloudstack virtualenv using Python 2.7.16
    # pip install -r <root CloudStack source tree>/requirements.txt ## Install cloudstack Python dependencies
    # jenv add <path to JDK 1.8 installation>                       ## Add Java7 to jenv

*N.B.* If you are running Linux, you may need to install additional packages to allow pyenv to build Python.

Following these steps, jenv and pyenv will use .java-version and .python-version files in the root of the CloudStack source tree to switch to the correct Java version and the cloudstack Python virtualenv for CloudStack development.

## Getting the Source Code

You may get the source code from the repository hosted on Apache:

    $ git clone https://gitbox.apache.org/repos/asf/cloudstack.git

Or, you may fork the repository from the official Apache CloudStack mirror on [Github](https://github.com/apache/cloudstack)

To checkout a specific branch, for example 4.11, do:

    $ git fetch origin
    $ git checkout -b 4.11 origin/4.11

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

Then:

    $ mvn -P deps # -D noredist, for noredist as described in the "Building" section above
    $ dpkg-buildpackage -uc -us

All the deb packages will be located one level down.

### RHEL/CentOS

To create rpms, install the following extra packages:

    # yum -y install rpm-build
    # yum -y install ws-commons-util
    # yum -y install gcc
    # yum -y install glibc-devel
    # yum -y install MySQL-python

Then:

    $ cd packaging
    $ package.sh

To create packages for noredist add the `-p noredist` option to the package script.
All the rpm packages will be created in `dist/rpmbuild/RPMS/x86_64` directory.

## Notes

If you will be using Xen as your hypervisor, please download [vhd-util](http://download.cloudstack.org/tools/vhd-util)

If management server is installed on RHEL/CentOS, then copy vhd-util into:

    /usr/lib64/cloud/common/scripts/vm/hypervisor/xenserver/

If management server is installed on Ubuntu, then put vhd-util into:

    /usr/lib/cloud/common/scripts/vm/hypervisor/xenserver/vhd-util
