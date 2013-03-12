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

%define __os_install_post %{nil}
%global debug_package %{nil}

# DISABLE the post-percentinstall java repacking and line number stripping
# we need to find a way to just disable the java repacking and line number stripping, but not the autodeps

Name:      cloudstack
Summary:   CloudStack IaaS Platform
#http://fedoraproject.org/wiki/PackageNamingGuidelines#Pre-Release_packages
%if "%{?_prerelease}" != ""
%define _maventag %{_ver}-SNAPSHOT
Release:   %{_rel}%{dist}
%else
%define _maventag %{_ver}
Release:   %{_rel}%{dist}
%endif
Version:   %{_ver}
License:   ASL 2.0
Vendor:    Apache CloudStack <cloudstack-dev@incubator.apache.org>
Packager:  Apache CloudStack <cloudstack-dev@incubator.apache.org>
Group:     System Environment/Libraries
# FIXME do groups for every single one of the subpackages
Source0:   %{name}-%{_maventag}.tgz
BuildRoot: %{_tmppath}/%{name}-%{_maventag}-%{release}-build

BuildRequires: java-1.6.0-openjdk-devel
BuildRequires: tomcat6
BuildRequires: ws-commons-util
BuildRequires: jpackage-utils
BuildRequires: gcc
BuildRequires: glibc-devel
BuildRequires: /usr/bin/mkisofs
BuildRequires: MySQL-python
#BuildRequires: maven => 3.0.0

%description
CloudStack is a highly-scalable elastic, open source,
intelligent IaaS cloud implementation.

%package management
Summary:   CloudStack management server UI
Requires: tomcat6
Requires: java >= 1.6.0
Requires: python
Requires: bash
Requires: bzip2
Requires: gzip
Requires: unzip
Requires: /sbin/mount.nfs
Requires: openssh-clients
Requires: nfs-utils
Requires: wget
Requires: mysql-connector-java
Requires: ws-commons-util
Requires: jpackage-utils
Requires: sudo
Requires: /sbin/service
Requires: /sbin/chkconfig
Requires: /usr/bin/ssh-keygen
Requires: mkisofs
Requires: MySQL-python
Requires: python-paramiko
Requires: ipmitool
Requires: %{name}-common = %{_ver}
Requires: %{name}-awsapi = %{_ver} 
Obsoletes: cloud-client < 4.1.0
Obsoletes: cloud-client-ui < 4.1.0
Obsoletes: cloud-daemonize < 4.1.0
Obsoletes: cloud-server < 4.1.0
Obsoletes: cloud-test < 4.1.0 
Provides:  cloud-client
Group:     System Environment/Libraries
%description management
The CloudStack management server is the central point of coordination,
management, and intelligence in CloudStack.  

%package common
Summary: Apache CloudStack common files and scripts
Requires: python
Obsoletes: cloud-test < 4.1.0 
Obsoletes: cloud-scripts < 4.1.0
Obsoletes: cloud-utils < 4.1.0
Obsoletes: cloud-core < 4.1.0
Obsoletes: cloud-deps < 4.1.0
Obsoletes: cloud-python < 4.1.0
Obsoletes: cloud-setup < 4.1.0
Obsoletes: cloud-cli < 4.1.0
Group:   System Environment/Libraries
%description common
The Apache CloudStack files shared between agent and management server

%package agent
Summary: CloudStack Agent for KVM hypervisors
Requires: java >= 1.6.0
Requires: jna >= 3.2.4
Requires: %{name}-common = %{_ver}
Requires: libvirt
Requires: bridge-utils
Requires: ebtables
Requires: jsvc
Requires: jakarta-commons-daemon
Requires: jakarta-commons-daemon-jsvc
Requires: perl
Provides: cloud-agent
Obsoletes: cloud-agent < 4.1.0
Obsoletes: cloud-test < 4.1.0
Group: System Environment/Libraries
%description agent
The CloudStack agent for KVM hypervisors

%package usage
Summary: CloudStack Usage calculation server
Requires: java >= 1.6.0
Requires: jsvc
Requires: jakarta-commons-daemon
Requires: jakarta-commons-daemon-jsvc
Obsoletes: cloud-usage < 4.1.0
Provides: cloud-usage 
%description usage
The CloudStack usage calculation service

%package cli
Summary: Apache CloudStack CLI
Provides: python-cloudmonkey
Provides: python-marvin
%description cli
Apache CloudStack command line interface

%package awsapi
Summary: Apache CloudStack AWS API compatibility wrapper
Requires: %{name}-management = %{_ver}
Obsoletes: cloud-aws-api < 4.1.0
Provides: cloud-aws-api
%description awsapi
Apache Cloudstack AWS API compatibility wrapper

#%package docs
#Summary: Apache CloudStack documentation
#%description docs
#Apache CloudStack documentations

%prep
echo Doing CloudStack build
%setup -q -n %{name}-%{_maventag}

%build

cp packaging/centos63/replace.properties build/replace.properties
echo VERSION=%{_maventag} >> build/replace.properties
echo PACKAGE=%{name} >> build/replace.properties
mvn -P awsapi package -Dsystemvm

%install
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}
# Common directories
mkdir -p ${RPM_BUILD_ROOT}%{_bindir}
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/agent
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/awsapi
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/ipallocator
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/cache/%{name}/management/work
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/cache/%{name}/management/temp
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/%{name}/mnt
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/%{name}/management
mkdir -p ${RPM_BUILD_ROOT}%{_initrddir}
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/sysconfig

# Common
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/scripts
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/vms
mkdir -p ${RPM_BUILD_ROOT}%{_libdir}/python2.6/site-packages/
cp -r scripts/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/scripts
install -D services/console-proxy/server/dist/systemvm.iso ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/vms/systemvm.iso
install -D services/console-proxy/server/dist/systemvm.zip ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/vms/systemvm.zip
install python/lib/cloud_utils.py ${RPM_BUILD_ROOT}%{_libdir}/python2.6/site-packages/cloud_utils.py
cp -r python/lib/cloudutils ${RPM_BUILD_ROOT}%{_libdir}/python2.6/site-packages/
python -m py_compile ${RPM_BUILD_ROOT}%{_libdir}/python2.6/site-packages/cloud_utils.py
python -m compileall ${RPM_BUILD_ROOT}%{_libdir}/python2.6/site-packages/cloudutils

# Management
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/setup
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/management
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/awsapi
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management

# Specific for tomcat
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management/Catalina/localhost/client
ln -sf /usr/share/tomcat6/bin ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/bin
ln -sf /etc/%{name}/management ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/conf
ln -sf /usr/share/tomcat6/lib ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/lib
ln -sf /var/log/%{name}/management ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/logs
ln -sf /var/cache/%{name}/management/temp ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/temp
ln -sf /var/cache/%{name}/management/work ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/work

install -D client/target/utilities/bin/cloud-migrate-databases ${RPM_BUILD_ROOT}%{_bindir}/%{name}-migrate-databases
install -D client/target/utilities/bin/cloud-set-guest-password ${RPM_BUILD_ROOT}%{_bindir}/%{name}-set-guest-password
install -D client/target/utilities/bin/cloud-set-guest-sshkey ${RPM_BUILD_ROOT}%{_bindir}/%{name}-set-guest-sshkey
install -D client/target/utilities/bin/cloud-setup-databases ${RPM_BUILD_ROOT}%{_bindir}/%{name}-setup-databases
install -D client/target/utilities/bin/cloud-setup-encryption ${RPM_BUILD_ROOT}%{_bindir}/%{name}-setup-encryption
install -D client/target/utilities/bin/cloud-setup-management ${RPM_BUILD_ROOT}%{_bindir}/%{name}-setup-management
install -D client/target/utilities/bin/cloud-sysvmadm ${RPM_BUILD_ROOT}%{_bindir}/%{name}-sysvmadm
install -D client/target/utilities/bin/cloud-update-xenserver-licenses ${RPM_BUILD_ROOT}%{_bindir}/%{name}-update-xenserver-licenses

cp -r client/target/utilities/scripts/db/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/setup
cp -r client/target/cloud-client-ui-%{_maventag}/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client

# Don't package the scripts in the management webapp
rm -rf ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client/WEB-INF/classes/scripts
rm -rf ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client/WEB-INF/classes/vms

for name in db.properties log4j-cloud.xml tomcat6-nonssl.conf tomcat6-ssl.conf server-ssl.xml server-nonssl.xml \
            catalina.policy catalina.properties db-enc.properties classpath.conf tomcat-users.xml web.xml environment.properties ; do
  mv ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client/WEB-INF/classes/$name \
    ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management/$name
done

ln -s %{_sysconfdir}/%{name}/management/log4j-cloud.xml \
    ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client/WEB-INF/classes/log4j-cloud.xml

mv ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client/WEB-INF/classes/context.xml \
    ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management/Catalina/localhost/client

install python/bindir/cloud-external-ipallocator.py ${RPM_BUILD_ROOT}%{_bindir}/%{name}-external-ipallocator.py
install -D client/target/pythonlibs/jasypt-1.9.0.jar ${RPM_BUILD_ROOT}%{_javadir}/jasypt-1.9.0.jar
install -D client/target/pythonlibs/jasypt-1.8.jar ${RPM_BUILD_ROOT}%{_javadir}/jasypt-1.8.jar

install -D packaging/centos63/cloud-ipallocator.rc ${RPM_BUILD_ROOT}%{_initrddir}/%{name}-ipallocator
install -D packaging/centos63/cloud-management.rc ${RPM_BUILD_ROOT}%{_initrddir}/%{name}-management
install -D packaging/centos63/cloud-management.sysconfig ${RPM_BUILD_ROOT}%{_sysconfdir}/sysconfig/%{name}-management

chmod 770 ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management/Catalina
chmod 770 ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management/Catalina/localhost
chmod 770 ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management/Catalina/localhost/client
chmod 770 ${RPM_BUILD_ROOT}%{_localstatedir}/%{name}/mnt
chmod 770 ${RPM_BUILD_ROOT}%{_localstatedir}/%{name}/management
chmod 770 ${RPM_BUILD_ROOT}%{_localstatedir}/cache/%{name}/management/work
chmod 770 ${RPM_BUILD_ROOT}%{_localstatedir}/cache/%{name}/management/temp
chmod 770 ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/management
chmod 770 ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/agent

# KVM Agent
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/agent
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/agent
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-agent/lib
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-agent/plugins
install -D packaging/centos63/cloud-agent.rc ${RPM_BUILD_ROOT}%{_sysconfdir}/init.d/%{name}-agent
install -D agent/target/transformed/agent.properties ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/agent/agent.properties
install -D agent/target/transformed/environment.properties ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/agent/environment.properties
install -D agent/target/transformed/log4j-cloud.xml ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/agent/log4j-cloud.xml
install -D agent/target/transformed/cloud-setup-agent ${RPM_BUILD_ROOT}%{_bindir}/%{name}-setup-agent
install -D agent/target/transformed/cloud-ssh ${RPM_BUILD_ROOT}%{_bindir}/%{name}-ssh
install -D plugins/hypervisors/kvm/target/cloud-plugin-hypervisor-kvm-%{_maventag}.jar ${RPM_BUILD_ROOT}%{_datadir}/%name-agent/lib/cloud-plugin-hypervisor-kvm-%{_maventag}.jar
cp plugins/hypervisors/kvm/target/dependencies/*  ${RPM_BUILD_ROOT}%{_datadir}/%{name}-agent/lib

# Usage server
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/usage
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-usage/lib
install -D usage/target/cloud-usage-%{_maventag}.jar ${RPM_BUILD_ROOT}%{_datadir}/%{name}-usage/cloud-usage-%{_maventag}.jar
cp usage/target/dependencies/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-usage/lib/
install -D packaging/centos63/cloud-usage.rc ${RPM_BUILD_ROOT}/%{_sysconfdir}/init.d/%{name}-usage
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/usage/

# CLI
cp -r cloud-cli/cloudtool ${RPM_BUILD_ROOT}%{_libdir}/python2.6/site-packages/
install cloud-cli/cloudapis/cloud.py ${RPM_BUILD_ROOT}%{_libdir}/python2.6/site-packages/cloudapis.py

# AWS API
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-bridge/webapps/bridge
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-bridge/setup
cp -r awsapi/target/cloud-awsapi-%{_maventag}/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-bridge/webapps/bridge
install -D awsapi-setup/setup/cloud-setup-bridge ${RPM_BUILD_ROOT}%{_bindir}/cloudstack-setup-bridge
install -D awsapi-setup/setup/cloudstack-aws-api-register ${RPM_BUILD_ROOT}%{_bindir}/cloudstack-aws-api-register
cp -r awsapi-setup/db/mysql/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-bridge/setup

%clean
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}


%preun management
/sbin/service cloud-management stop || true
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del cloud-management  > /dev/null 2>&1 || true
    /sbin/service cloud-management stop > /dev/null 2>&1 || true
fi

%pre management
id cloud > /dev/null 2>&1 || /usr/sbin/useradd -M -c "CloudStack unprivileged user" \
     -r -s /bin/sh -d %{_localstatedir}/cloudstack/management cloud|| true

# set max file descriptors for cloud user to 4096
sed -i /"cloud hard nofile"/d /etc/security/limits.conf
sed -i /"cloud soft nofile"/d /etc/security/limits.conf
echo "cloud hard nofile 4096" >> /etc/security/limits.conf
echo "cloud soft nofile 4096" >> /etc/security/limits.conf
rm -rf %{_localstatedir}/cache/cloud
# user harcoded here, also hardcoded on wscript

%post management
if [ "$1" == "1" ] ; then
    /sbin/chkconfig --add cloud-management > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 cloud-management on > /dev/null 2>&1 || true
fi

if [ -d "%{_datadir}/%{name}-management" ] ; then
   ln -s %{_datadir}/%{name}-bridge/webapps %{_datadir}/%{name}-management/webapps7080
fi

if [ ! -f %{_datadir}/cloudstack-common/scripts/vm/hypervisor/xenserver/vhd-util ] ; then
    echo Please download vhd-util from http://download.cloud.com.s3.amazonaws.com/tools/vhd-util and put it in 
    echo %{_datadir}/cloudstack-common/scripts/vm/hypervisor/xenserver/
fi

# change cloud user's home to 4.1+ version if needed. Would do this via 'usermod', but it
# requires that cloud user not be in use, so RPM could not be installed while management is running
if getent passwd cloud | grep -q /var/lib/cloud; then 
    sed -i 's/\/var\/lib\/cloud\/management/\/var\/cloudstack\/management/g' /etc/passwd
fi


#%post awsapi
#if [ -d "%{_datadir}/%{name}-management" ] ; then
#   ln -s %{_datadir}/%{name}-bridge/webapps %{_datadir}/%{name}-management/webapps7080
#fi

#No default permission as the permission setup is complex
%files management
%defattr(-,root,root,-)
%dir %attr(0770,root,cloud) %{_sysconfdir}/%{name}/management/Catalina
%dir %attr(0770,root,cloud) %{_sysconfdir}/%{name}/management/Catalina/localhost
%dir %attr(0770,root,cloud) %{_sysconfdir}/%{name}/management/Catalina/localhost/client
%dir %{_datadir}/%{name}-management
%dir %attr(0770,root,cloud) %{_localstatedir}/%{name}/mnt
%dir %attr(0770,cloud,cloud) %{_localstatedir}/%{name}/management
%dir %attr(0770,root,cloud) %{_localstatedir}/cache/%{name}/management
%dir %attr(0770,root,cloud) %{_localstatedir}/cache/%{name}/management/work
%dir %attr(0770,root,cloud) %{_localstatedir}/cache/%{name}/management/temp
%dir %attr(0770,root,cloud) %{_localstatedir}/log/%{name}/management
%dir %attr(0770,root,cloud) %{_localstatedir}/log/%{name}/agent
%dir %attr(0770,root,cloud) %{_localstatedir}/log/%{name}/awsapi
%config(noreplace) %{_sysconfdir}/sysconfig/%{name}-management
%config(noreplace) %attr(0640,root,cloud) %{_sysconfdir}/%{name}/management/db.properties
%config(noreplace) %{_sysconfdir}/%{name}/management/log4j-cloud.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/tomcat6-nonssl.conf
%config(noreplace) %{_sysconfdir}/%{name}/management/tomcat6-ssl.conf
%config(noreplace) %{_sysconfdir}/%{name}/management/Catalina/localhost/client/context.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/catalina.policy
%config(noreplace) %{_sysconfdir}/%{name}/management/catalina.properties
%config(noreplace) %{_sysconfdir}/%{name}/management/classpath.conf
%config(noreplace) %{_sysconfdir}/%{name}/management/db-enc.properties
%config(noreplace) %{_sysconfdir}/%{name}/management/server-nonssl.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/server-ssl.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/tomcat-users.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/web.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/environment.properties
%attr(0755,root,root) %{_initrddir}/%{name}-management
%attr(0755,root,root) %{_bindir}/%{name}-setup-management
%attr(0755,root,root) %{_bindir}/%{name}-update-xenserver-licenses
%{_datadir}/%{name}-management/webapps
%dir %{_datadir}/%{name}-management/bin
%dir %{_datadir}/%{name}-management/conf
%dir %{_datadir}/%{name}-management/lib
%dir %{_datadir}/%{name}-management/logs
%dir %{_datadir}/%{name}-management/temp
%dir %{_datadir}/%{name}-management/work
%attr(0755,root,root) %{_bindir}/%{name}-setup-databases
%attr(0755,root,root) %{_bindir}/%{name}-migrate-databases
%attr(0755,root,root) %{_bindir}/%{name}-set-guest-password
%attr(0755,root,root) %{_bindir}/%{name}-set-guest-sshkey
%attr(0755,root,root) %{_bindir}/%{name}-sysvmadm
%attr(0755,root,root) %{_bindir}/%{name}-setup-encryption
%{_datadir}/%{name}-management/setup/*.sql
%{_datadir}/%{name}-management/setup/db/*.sql
%{_datadir}/%{name}-management/setup/*.sh
%{_datadir}/%{name}-management/setup/server-setup.xml
%{_javadir}/jasypt-1.9.0.jar
%{_javadir}/jasypt-1.8.jar
%attr(0755,root,root) %{_bindir}/%{name}-external-ipallocator.py
%attr(0755,root,root) %{_initrddir}/%{name}-ipallocator
%dir %attr(0770,root,root) %{_localstatedir}/log/%{name}/ipallocator
%doc LICENSE
%doc NOTICE

%files agent
%attr(0755,root,root) %{_bindir}/%{name}-setup-agent
%attr(0755,root,root) %{_bindir}/%{name}-ssh
%attr(0755,root,root) %{_sysconfdir}/init.d/%{name}-agent
%config(noreplace) %{_sysconfdir}/%{name}/agent
%dir %{_localstatedir}/log/%{name}/agent
%attr(0644,root,root) %{_datadir}/%{name}-agent/lib/*.jar
%dir %{_datadir}/%{name}-agent/plugins
%doc LICENSE
%doc NOTICE

%files common
%dir %attr(0755,root,root) %{_libdir}/python2.6/site-packages/cloudutils
%dir %attr(0755,root,root) %{_datadir}/%{name}-common/vms
%attr(0755,root,root) %{_datadir}/%{name}-common/scripts
%attr(0644, root, root) %{_datadir}/%{name}-common/vms/systemvm.iso
%attr(0644, root, root) %{_datadir}/%{name}-common/vms/systemvm.zip
%attr(0644,root,root) %{_libdir}/python2.6/site-packages/cloud_utils.py
%attr(0644,root,root) %{_libdir}/python2.6/site-packages/cloud_utils.pyc
%attr(0644,root,root) %{_libdir}/python2.6/site-packages/cloudutils/*
%doc LICENSE
%doc NOTICE

%files usage
%attr(0755,root,root) %{_sysconfdir}/init.d/%{name}-usage
%attr(0644,root,root) %{_datadir}/%{name}-usage/*.jar
%attr(0644,root,root) %{_datadir}/%{name}-usage/lib/*.jar
%dir /var/log/%{name}/usage
%dir %{_sysconfdir}/%{name}/usage
%doc LICENSE
%doc NOTICE

%files cli
%attr(0644,root,root) %{_libdir}/python2.6/site-packages/cloudapis.py
%attr(0644,root,root) %{_libdir}/python2.6/site-packages/cloudtool/__init__.py
%attr(0644,root,root) %{_libdir}/python2.6/site-packages/cloudtool/utils.py
%doc LICENSE
%doc NOTICE

#%files docs
#%doc LICENSE
#%doc NOTICE

%files awsapi
%defattr(0644,cloud,cloud,0755)
%{_datadir}/%{name}-bridge/webapps/bridge
%attr(0644,root,root) %{_datadir}/%{name}-bridge/setup/*
%attr(0755,root,root) %{_bindir}/cloudstack-aws-api-register
%attr(0755,root,root) %{_bindir}/cloudstack-setup-bridge
%doc LICENSE
%doc NOTICE


%changelog
* Fri Oct 03 2012 Hugo Trippaers <hugo@apache.org> 4.1.0
- new style spec file

