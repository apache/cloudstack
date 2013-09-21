This document describes how to develop, build, package and install Apache
CloudStack. For more information please refer to:

    http://cloudstack.apache.org

Apache CloudStack developers use various platforms for development, this guide
was tested against a CentOS 6.2 x86_64 setup.

Refer to the [wiki](http://cwiki.apache.org/confluence/display/CLOUDSTACK/Index)
for the latest information, especially:

  - [Setting up development environment](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Setting+up+CloudStack+Development+Environment) for Apache CloudStack.
  - [Building](https://cwiki.apache.org/confluence/display/CLOUDSTACK/How+to+build+on+master+branch) Apache CloudStack.

## Setting up Development Environment

### Installing Tools and Dependencies

Install tools and dependencies used for development:

    $ yum install git ant ant-devel java-1.6.0-openjdk java-1.6.0-openjdk-devel
    mysql mysql-server tomcat6 mkisofs gcc python MySQL-python openssh-clients wget

Set up Maven (3.0.5):

    $ wget http://www.us.apache.org/dist/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
    $ cd /usr/local/ # or any path
    $ tar -zxvf apache-maven-3.0.5-bin.tar.gz
    $ echo export M2_HOME=/usr/local/apache-maven-3.0.5 >> ~/.bashrc # or .zshrc or .profile
    $ echo export PATH=${M2_HOME}/bin:${PATH} >> ~/.bashrc # or .zshrc or .profile

Note: Tomcat 6.0.35 has some known issue with Apache CloudStack, please use Tomcat
6.0.33 from http://archive.apache.org/dist/tomcat/tomcat-6/v6.0.33/bin

### Configure Environment

Set CATALINA_HOME to path where you extract/install tomcat, put them in your
.bashrc or .zshrc or .profile:

    $ echo export CATALINA_HOME=/usr/share/tomcat6/ >> ~/.bashrc

Fix permissions on CATALINA_HOME:

    $ chown -R <you>:<your-group> $CATALINA_HOME

Generate you ssh keys, useful for ssh-ing to your hosts and vm etc.:

    $ ssh-keygen -t rsa -q

Apache CloudStack uses some ports, make sure at least those used by the management
server are available and not blocked by any local firewall. Following ports are
used by Apache CloudStack and its entities:

    8080: API Server (authenticated), browser or CLI client to management server
    8096: API Server (unauthenticated), browser or CLI client to management server
    8787: Remote java debug debugging port, from IDE to management server
    9090: Management server to management server (cluster)
    7080: AWS API Server to which an AWS client can connect
    80/443: HTTP client to Secondary Storage VM (template download)
    111/2049: Secondary Storage to NFS server
    3922: Port used to ssh/scp into system vms (SSVM, CPVM, VR)
    8250: Agent (SSVM, CPVM, VR) to management server
    22, 80, 443: XenServer, XAPI
    22: KVM
    443: vCenter
    53: DNS
    111/2049: NFS
    3306: MySQL Server to which the management server connects

### Configuring MySQL Server

Start the MySQL service:

    $ service mysqld start

### Getting the Source Code

You may get the source code from the repository hosted on Apache:

    $ git clone git://git.apache.org/cloudstack.git

Or, you may fork a repository from the official Apache CloudStack mirror by
Apache on [Github](https://github.com/apache/cloudstack)

To keep yourself updated on a branch, do:

    $ git pull <origin> <branch>

For example, for master:

    $ git pull origin master

## Building

Clean and build:

    $ mvn clean install -P systemvm,developer

CloudStack supports several plugins that depend on libraries with distribution restrictions. 
Because of this they are not included in the default build. Enable these additional plugins 
activate their respective profiles. For convenience adding -Dnoredist will enable all plugins
that depend on libraries with distribution restrictions. The build procedure expects that the 
required libraries are present in the maven repository. 

The following procedure can be used to add the libraries to the local maven repository. Details 
on obtaining the required libraries can be found in this file. Note that this will vary between
releases of cloudstack

    $ cd deps
    $ ./install-non-oss.sh

To build all non redistributable components, add the noredist flag to the build command:

    $ mvn clean install -P systemvm,developer -Dnoredist

Clear old database (if any) and deploy the database schema:

    $ mvn -P developer -pl developer -Ddeploydb

Export the following variable if you need to run and debug the management server:

    $ export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=500m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"

Start the management server:

    $ mvn -pl :cloud-client-ui jetty:run

If this works, you've successfully setup a single server Apache CloudStack installation.

Open the following URL on your browser to access the Management Server UI:

    http://localhost:8080/client/

Or,

    http://management-server-ip-address:8080/client

The default credentials are; user: admin, password: password and the domain
field should be left blank which is defaulted to the ROOT domain.

If you want to contribute your changes, send your [git formatted patch](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Git) to:
https://reviews.apache.org/groups/cloudstack or contact on the developer mailing list.

## Packaging and Installation

Before packaging, please make sure you go through the "Building" section above.
This section describes packaging and installation.

### Debian/Ubuntu

To create debs:

    $ mvn -P deps # -D noredist, for noredist as described in the "Building" section above
    $ dpkg-buildpackage

All the deb packages will be created in ../$PWD

To create an apt repo: (assuming appropriate user privileges)

    $ path=/path/to/your/webserver/cloudstack
    $ mv ../*.deb $path
    $ dpkg-scanpackages $path /dev/null | gzip -9c > $path/Packages.gz

Configure your system to use your new apt repo:

    $ echo "deb $path ./" >> /etc/apt/sources.list.d/cloudstack.list

Installation:

Install needed packages, apt-get upgrade for upgrading:

    $ apt-get update
    $ apt-get install cloud-client                   # management server
    $ apt-get install mysql-server                   # mysql server
    $ apt-get install cloud-agent cloud-system-iso   # agent (kvm)
    $ apt-get install cloud-awsapi                   # awsapi server
    $ apt-get install cloud-usage                    # usage server

### RHEL/CentOS

To create rpms:

    $ cd packaging/centos63
    $ bash packaging.sh [ -p NOREDIST ]

All the rpm packages will be create in dist/rpmbuild/RPMS/x86_64

To create a yum repo: (assuming appropriate user privileges)

    $ path=/path/to/your/webserver/cloudstack
    $ cd dist/rpmbuild/RPMS/x86_64
    $ mv *.rpm $path
    $ createrepo $path

Configure your system to use your new yum repo, add the following to /etc/yum.repos.d/cloudstack.repo:

    [apache-cloudstack]
    name=Apache CloudStack
    baseurl=http://webserver.tld/path/to/repo
    enabled=1
    gpgcheck=0

Installation:

Install needed packages:

    $ yum update
    $ yum install cloudstack-management                       # management server
    $ yum install mysql-server                       # mysql server
    $ yum install cloudstack-agent                        # agent (kvm)
    $ yum install cloudstack-usage                        # usage server

## Installing CloudMonkey CLI

CloudMonkey is a CLI for Apache CloudStack. It was earlier in `tools/cli` within
the source code but now it has its own repository:

    https://git-wip-us.apache.org/repos/asf?p=cloudstack-cloudmonkey.git

## Notes

If you will be using Xen as your hypervisor, please download [vhd-util](http://download.cloud.com.s3.amazonaws.com/tools/vhd-util)

If management server is installed on RHEL/CentOS, then copy vhd-util into:
/usr/lib64/cloud/common/scripts/vm/hypervisor/xenserver/

If management server is installed on Ubuntu, then put vhd-util into:
/usr/lib/cloud/common/scripts/vm/hypervisor/xenserver/vhd-util

Once, you've successfully installed Apache CloudStack you may read the user manuals
and guides which contains technical documentation for Apache CloudStack.
