This document describes how to set up and configure a single server CloudStack
development environment. If you aren't looking for a development environment,
the easiest way to deploy CloudStack is by using RPM or DEB packages from:

  - http://cloudstack.org/download.html
  - http://jenkins.cloudstack.org (CI/Build server)
  - http://cloudstack.apt-get.eu (Debian repository)

CloudStack developers use various platforms for development, this guide will
focus on CentOS and was tested against a CentOS 6.2 x86_64 setup.

Refer to the [wiki](http://cwiki.apache.org/confluence/display/CLOUDSTACK/Index)
for the latest information, especially:

  - [Setting up development environment](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Setting+up+CloudStack+Development+Environment) for CloudStack.
  - [Building](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Building+with+Maven) CloudStack.

## Setting up Development Environment

### Installing Tools and Dependencies

Install tools and dependencies used for development:

    $ yum install git ant ant-devel java-1.6.0-openjdk java-1.6.0-openjdk-devel
    mysql mysql-server tomcat6 mkisofs gcc python MySQL-python openssh-clients wget

Set up Maven (3.0.4):

    $ wget http://www.us.apache.org/dist/maven/maven-3/3.0.4/binaries/apache-maven-3.0.4-bin.tar.gz
    $ cd /usr/local/ # or any path
    $ tar -zxvf apache-maven-3.0.4-bin.tar.gz
    $ echo export M2_HOME=/usr/local/apache-maven-3.0.4 >> ~/.bashrc # or .zshrc or .profile
    $ echo export PATH=${M2_HOME}/bin:${PATH} >> ~/.bashrc # or .zshrc or .profile

Note: Tomcat 6.0.35 has some known issue with CloudStack, please use Tomcat
6.0.33 from http://archive.apache.org/dist/tomcat/tomcat-6/v6.0.33/bin

### Configure Environment

Set CATALINA_HOME to path where you extract/install tomcat, put them in your
.bashrc or .zshrc or .profile:

    $ echo export CATALINA_HOME=/usr/share/tomcat6/ >> ~/.bashrc

Fix permissions on CATALINA_HOME:

    $ chown -R <you>:<your-group> $CATALINA_HOME

Generate you ssh keys, useful for ssh-ing to your hosts and vm etc.:

    $ ssh-keygen -t rsa -q

CloudStack uses some ports, make sure at least those used by the management
server are available and not blocked by any local firewall. Following ports are
used by CloudStack and its entities:

8787: CloudStack (Tomcat) debug socket
9090, 8250: CloudStack Management Server, User/Client API
8096: User/Client to CloudStack Management Server (unauthenticated)
3306: MySQL Server
3922, 8250, 80/443, 111/2049, 53: Secondary Storage VM
3922, 8250, 53: Console Proxy VM
3922, 8250, 53: Virtual Router
22, 80, 443: XenServer, XAPI
22: KVM
443: vCenter
DNS: 53
NFS: 111/2049

### Configuring MySQL Server

Start the MySQL service:

    $ service mysqld start

### Getting the Source Code

You may get the source code from the repository hosted on Apache:

    $ git clone https://git-wip-us.apache.org/repos/asf/incubator-cloudstack.git

Or, you may fork a repository from the official Apache CloudStack mirror by
Apache on [Github](https://github.com/apache/incubator-cloudstack)

To keep yourself updated on a branch, do:

    $ git pull <origin> <branch>

For example, for master:

    $ git pull origin master

## Building

Populate the dependencies using Maven:

    $ mvn -P deps

Clean previous build, if needed:

    $ mvn clean
    $ ant clean-all
    $ ant clean-tomcat

Build all sub-modules:

    $ ant build-all

Deploy the built project on tomcat:

    $ ant deploy-server

Clear old database (if any) and deploy the database schema:

    $ ant deploydb

Start the management server in debug mode:

    $ ant debug

If this works, you've successfully setup a single server CloudStack installation.

Open the following URL on your browser to access the Management Server UI:

    http://localhost:8080/client/

Or,

    http://management-server-ip-address:8080/client

The default credentials are; user: admin, password: password and the domain
field should be left blank which is defaulted to the ROOT domain.

## Packaging

To create rpms:

    $ mvn -P deps && ./waf rpm

To create debs:

    $ mvn -P deps && dpkg-buildpackage

