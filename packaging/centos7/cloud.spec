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

%{!?python_sitearch: %define python_sitearch %(%{__python} -c "from distutils.sysconfig import get_python_lib; print(get_python_lib(1))")}

Version:   %{_ver}
License:   ASL 2.0
Vendor:    Apache CloudStack <dev@cloudstack.apache.org>
Packager:  Apache CloudStack <dev@cloudstack.apache.org>
Group:     System Environment/Libraries
# FIXME do groups for every single one of the subpackages
Source0:   %{name}-%{_maventag}.tgz
BuildRoot: %{_tmppath}/%{name}-%{_maventag}-%{release}-build

BuildRequires: java-1.7.0-openjdk-devel
BuildRequires: tomcat => 7.0
BuildRequires: ws-commons-util
BuildRequires: jpackage-utils
BuildRequires: gcc
BuildRequires: glibc-devel
BuildRequires: /usr/bin/mkisofs
BuildRequires: MySQL-python
BuildRequires: maven => 3.0.0

%description
CloudStack is a highly-scalable elastic, open source,
intelligent IaaS cloud implementation.

%package management
Summary:   CloudStack management server UI
Requires: tomcat => 7.0
Requires: java => 1.7.0
Requires: python
Requires: bash
Requires: bzip2
Requires: gzip
Requires: unzip
Requires: /sbin/mount.nfs
Requires: openssh-clients
Requires: nfs-utils
Requires: wget
Requires: mysql
Requires: mysql-connector-java
Requires: ws-commons-util
Requires: jpackage-utils
Requires: sudo
Requires: /sbin/service
Requires: /sbin/chkconfig
Requires: /usr/bin/ssh-keygen
Requires: mkisofs
Requires: MySQL-python
Requires: ipmitool
Requires: %{name}-common = %{_ver}
Requires: iptables-services
Group:     System Environment/Libraries
%description management
The CloudStack management server is the central point of coordination,
management, and intelligence in CloudStack.  

%package common
Summary: Apache CloudStack common files and scripts
Requires: python
Group:   System Environment/Libraries
%description common
The Apache CloudStack files shared between agent and management server

%package agent
Summary: CloudStack Agent for KVM hypervisors
Requires: openssh-clients
Requires: java => 1.7.0
Requires: %{name}-common = %{_ver}
Requires: libvirt
Requires: bridge-utils
Requires: ebtables
Requires: iptables
Requires: ethtool
Requires: iproute
Requires: ipset
Requires: jsvc
Requires: jakarta-commons-daemon
Requires: jakarta-commons-daemon-jsvc
Requires: perl
Requires: libvirt-python
Requires: qemu-img
Requires: qemu-kvm
Provides: cloud-agent
Group: System Environment/Libraries
%description agent
The CloudStack agent for KVM hypervisors

%package baremetal-agent
Summary: CloudStack baremetal agent
Requires: tftp-server
Requires: xinetd
Requires: syslinux
Requires: chkconfig
Requires: dhcp
Requires: httpd
Group:     System Environment/Libraries
%description baremetal-agent
The CloudStack baremetal agent

%package usage
Summary: CloudStack Usage calculation server
Requires: java => 1.7.0
Requires: jsvc
Requires: jakarta-commons-daemon
Requires: jakarta-commons-daemon-jsvc
Requires: mysql-connector-java
Group: System Environment/Libraries
%description usage
The CloudStack usage calculation service

%package cli
Summary: Apache CloudStack CLI
Provides: python-cloudmonkey
Provides: python-marvin
Group: System Environment/Libraries
%description cli
Apache CloudStack command line interface

%if "%{_ossnoss}" == "noredist"
%package mysql-ha
Summary: Apache CloudStack Balancing Strategy for MySQL
Requires: mysql-connector-java
Requires: tomcat => 7.0
Group: System Environmnet/Libraries
%description mysql-ha
Apache CloudStack Balancing Strategy for MySQL

%endif

%prep
echo Doing CloudStack build

%setup -q -n %{name}-%{_maventag}

%build

cp packaging/centos7/replace.properties build/replace.properties
echo VERSION=%{_maventag} >> build/replace.properties
echo PACKAGE=%{name} >> build/replace.properties
touch build/gitrev.txt
echo $(git rev-parse HEAD) > build/gitrev.txt

if [ "%{_ossnoss}" == "NOREDIST" -o "%{_ossnoss}" == "noredist" ] ; then
   echo "Adding noredist flag to the maven build"
   FLAGS="$FLAGS -Dnoredist"
fi

if [ "%{_sim}" == "SIMULATOR" -o "%{_sim}" == "simulator" ] ; then 
   echo "Adding simulator flag to the maven build"
   FLAGS="$FLAGS -Dsimulator"
fi 

mvn -Psystemvm -DskipTests $FLAGS clean package

%install
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}
# Common directories
mkdir -p ${RPM_BUILD_ROOT}%{_bindir}
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/agent
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/ipallocator
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/cache/%{name}/management/work
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/cache/%{name}/management/temp
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/%{name}/mnt
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/%{name}/management
mkdir -p ${RPM_BUILD_ROOT}%{_initrddir}
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/sysconfig
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/profile.d

# Common
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/scripts
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/vms
mkdir -p ${RPM_BUILD_ROOT}%{python_sitearch}/
mkdir -p ${RPM_BUILD_ROOT}%/usr/bin
cp -r scripts/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/scripts
install -D systemvm/dist/systemvm.iso ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/vms/systemvm.iso
install -D systemvm/dist/systemvm.zip ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/vms/systemvm.zip
install python/lib/cloud_utils.py ${RPM_BUILD_ROOT}%{python_sitearch}/cloud_utils.py
cp -r python/lib/cloudutils ${RPM_BUILD_ROOT}%{python_sitearch}/
python -m py_compile ${RPM_BUILD_ROOT}%{python_sitearch}/cloud_utils.py
python -m compileall ${RPM_BUILD_ROOT}%{python_sitearch}/cloudutils
cp build/gitrev.txt ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/scripts
cp packaging/centos7/cloudstack-sccs ${RPM_BUILD_ROOT}/usr/bin
 
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/scripts/network/cisco
cp -r plugins/network-elements/cisco-vnmc/scripts/network/cisco/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/scripts/network/cisco

# Management
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/setup
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/management
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/run

# Specific for tomcat
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management/Catalina/localhost/client
ln -sf /usr/share/tomcat/bin ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/bin
ln -sf /etc/%{name}/management ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/conf
ln -sf /usr/share/tomcat/lib ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/lib
ln -sf /var/log/%{name}/management ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/logs
ln -sf /var/cache/%{name}/management/temp ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/temp
ln -sf /var/cache/%{name}/management/work ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/work

/bin/touch ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/management/catalina.out

install -D client/target/utilities/bin/cloud-migrate-databases ${RPM_BUILD_ROOT}%{_bindir}/%{name}-migrate-databases
install -D client/target/utilities/bin/cloud-set-guest-password ${RPM_BUILD_ROOT}%{_bindir}/%{name}-set-guest-password
install -D client/target/utilities/bin/cloud-set-guest-sshkey ${RPM_BUILD_ROOT}%{_bindir}/%{name}-set-guest-sshkey
install -D client/target/utilities/bin/cloud-setup-databases ${RPM_BUILD_ROOT}%{_bindir}/%{name}-setup-databases
install -D client/target/utilities/bin/cloud-setup-encryption ${RPM_BUILD_ROOT}%{_bindir}/%{name}-setup-encryption
install -D client/target/utilities/bin/cloud-setup-management ${RPM_BUILD_ROOT}%{_bindir}/%{name}-setup-management
install -D client/target/utilities/bin/cloud-setup-baremetal ${RPM_BUILD_ROOT}%{_bindir}/%{name}-setup-baremetal
install -D client/target/utilities/bin/cloud-sysvmadm ${RPM_BUILD_ROOT}%{_bindir}/%{name}-sysvmadm
install -D client/target/utilities/bin/cloud-update-xenserver-licenses ${RPM_BUILD_ROOT}%{_bindir}/%{name}-update-xenserver-licenses

cp -r client/target/utilities/scripts/db/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/setup
cp -r client/target/cloud-client-ui-%{_maventag}/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client

# Don't package the scripts in the management webapp
rm -rf ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client/WEB-INF/classes/scripts
rm -rf ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client/WEB-INF/classes/vms

for name in catalina.properties db.properties log4j-cloud.xml web.xml \
            server.xml commons-logging.properties environment.properties java.security.ciphers tomcat-users.xml
do
  cp packaging/centos7/tomcat7/$name \
    ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management/$name
done

#ln -s %{_sysconfdir}/%{name}/management/log4j-cloud.xml \
#    ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client/WEB-INF/classes/log4j-cloud.xml

mv ${RPM_BUILD_ROOT}%{_datadir}/%{name}-management/webapps/client/WEB-INF/classes/context.xml \
    ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/management/Catalina/localhost/client

install python/bindir/cloud-external-ipallocator.py ${RPM_BUILD_ROOT}%{_bindir}/%{name}-external-ipallocator.py
install -D client/target/pythonlibs/jasypt-1.9.2.jar ${RPM_BUILD_ROOT}%{_datadir}/%{name}-common/lib/jasypt-1.9.2.jar

install -D packaging/centos7/cloud-ipallocator.rc ${RPM_BUILD_ROOT}%{_initrddir}/%{name}-ipallocator
install -D packaging/centos7/cloud-management.sysconfig ${RPM_BUILD_ROOT}%{_sysconfdir}/sysconfig/%{name}-management
install -D packaging/centos7/cloud-management.service ${RPM_BUILD_ROOT}%{_unitdir}/%{name}-management.service
install -D packaging/centos7/cloud-management.sudoers ${RPM_BUILD_ROOT}%{_sysconfdir}/sudoers.d/%{name}-management
install -D packaging/centos7/cloud.limits ${RPM_BUILD_ROOT}%{_sysconfdir}/security/limits.d/cloud
touch ${RPM_BUILD_ROOT}%{_localstatedir}/run/%{name}-management.pid

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
install -D packaging/centos7/cloud-agent.rc ${RPM_BUILD_ROOT}%{_sysconfdir}/init.d/%{name}-agent
install -D agent/target/transformed/agent.properties ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/agent/agent.properties
install -D agent/target/transformed/environment.properties ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/agent/environment.properties
install -D agent/target/transformed/log4j-cloud.xml ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/agent/log4j-cloud.xml
install -D agent/target/transformed/cloud-setup-agent ${RPM_BUILD_ROOT}%{_bindir}/%{name}-setup-agent
install -D agent/target/transformed/cloudstack-agent-upgrade ${RPM_BUILD_ROOT}%{_bindir}/%{name}-agent-upgrade
install -D agent/target/transformed/libvirtqemuhook ${RPM_BUILD_ROOT}%{_datadir}/%{name}-agent/lib/libvirtqemuhook
install -D agent/target/transformed/cloud-ssh ${RPM_BUILD_ROOT}%{_bindir}/%{name}-ssh
install -D agent/target/transformed/cloudstack-agent-profile.sh ${RPM_BUILD_ROOT}%{_sysconfdir}/profile.d/%{name}-agent-profile.sh
install -D plugins/hypervisors/kvm/target/cloud-plugin-hypervisor-kvm-%{_maventag}.jar ${RPM_BUILD_ROOT}%{_datadir}/%name-agent/lib/cloud-plugin-hypervisor-kvm-%{_maventag}.jar
cp plugins/hypervisors/kvm/target/dependencies/*  ${RPM_BUILD_ROOT}%{_datadir}/%{name}-agent/lib

# Usage server
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/usage
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-usage/lib
install -D usage/target/cloud-usage-%{_maventag}.jar ${RPM_BUILD_ROOT}%{_datadir}/%{name}-usage/cloud-usage-%{_maventag}.jar
install -D usage/target/transformed/db.properties ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/usage/db.properties
install -D usage/target/transformed/log4j-cloud_usage.xml ${RPM_BUILD_ROOT}%{_sysconfdir}/%{name}/usage/log4j-cloud.xml
cp usage/target/dependencies/* ${RPM_BUILD_ROOT}%{_datadir}/%{name}-usage/lib/
install -D packaging/centos7/cloud-usage.service ${RPM_BUILD_ROOT}%{_unitdir}/%{name}-usage.service
install -D packaging/centos7/cloud-usage.sysconfig ${RPM_BUILD_ROOT}%{_sysconfdir}/sysconfig/%{name}-usage
install -D packaging/centos7/cloud-usage-sysd ${RPM_BUILD_ROOT}/usr/sbin/%{name}-usage-sysd
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/run
touch ${RPM_BUILD_ROOT}%{_localstatedir}/run/%{name}-usage.pid
mkdir -p ${RPM_BUILD_ROOT}%{_localstatedir}/log/%{name}/usage/

# CLI
cp -r cloud-cli/cloudtool ${RPM_BUILD_ROOT}%{python_sitearch}/
install cloud-cli/cloudapis/cloud.py ${RPM_BUILD_ROOT}%{python_sitearch}/cloudapis.py

# MYSQL HA
if [ "x%{_ossnoss}" == "xnoredist" ] ; then
  mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/%{name}-mysql-ha/lib
  cp -r plugins/database/mysql-ha/target/cloud-plugin-database-mysqlha-%{_maventag}.jar ${RPM_BUILD_ROOT}%{_datadir}/%{name}-mysql-ha/lib
fi

#License files from whisker
install -D tools/whisker/NOTICE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-management-%{version}/NOTICE
install -D tools/whisker/LICENSE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-management-%{version}/LICENSE
install -D tools/whisker/NOTICE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-common-%{version}/NOTICE
install -D tools/whisker/LICENSE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-common-%{version}/LICENSE
install -D tools/whisker/NOTICE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-agent-%{version}/NOTICE
install -D tools/whisker/LICENSE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-agent-%{version}/LICENSE
install -D tools/whisker/NOTICE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-usage-%{version}/NOTICE
install -D tools/whisker/LICENSE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-usage-%{version}/LICENSE
install -D tools/whisker/NOTICE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-cli-%{version}/NOTICE
install -D tools/whisker/LICENSE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-cli-%{version}/LICENSE
if [ "x%{_ossnoss}" == "xnoredist" ] ; then
  install -D tools/whisker/LICENSE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-mysql-ha-%{version}/LICENSE
  install -D tools/whisker/NOTICE ${RPM_BUILD_ROOT}%{_defaultdocdir}/%{name}-mysql-ha-%{version}/NOTICE
fi

%clean
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}

%preun management
/usr/bin/systemctl stop cloudstack-management || true
/usr/bin/systemctl off cloudstack-management || true

%pre management
id cloud > /dev/null 2>&1 || /usr/sbin/useradd -M -c "CloudStack unprivileged user" \
     -r -s /bin/sh -d %{_localstatedir}/cloudstack/management cloud|| true

rm -rf %{_localstatedir}/cache/cloudstack

%post management
if [ "$1" == "1" ] ; then
    /usr/bin/systemctl on cloudstack-management > /dev/null 2>&1 || true
fi

if [ ! -f %{_datadir}/cloudstack-common/scripts/vm/hypervisor/xenserver/vhd-util ] ; then
    echo Please download vhd-util from http://download.cloud.com.s3.amazonaws.com/tools/vhd-util and put it in 
    echo %{_datadir}/cloudstack-common/scripts/vm/hypervisor/xenserver/
fi

%preun agent
/sbin/service cloudstack-agent stop || true
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del cloudstack-agent > /dev/null 2>&1 || true
    /sbin/service cloudstack-agent stop > /dev/null 2>&1 || true
fi

%pre agent

# save old configs if they exist (for upgrade). Otherwise we may lose them
# when the old packages are erased. There are a lot of properties files here.
if [ -d "%{_sysconfdir}/cloud" ] ; then
    mv %{_sysconfdir}/cloud %{_sysconfdir}/cloud.rpmsave
fi

%post agent
if [ "$1" == "1" ] ; then
    echo "Running %{_bindir}/%{name}-agent-upgrade to update bridge name for upgrade from CloudStack 4.0.x (and before) to CloudStack 4.1 (and later)"
    %{_bindir}/%{name}-agent-upgrade
    if [ ! -d %{_sysconfdir}/libvirt/hooks ] ; then
        mkdir %{_sysconfdir}/libvirt/hooks
    fi
    cp -a ${RPM_BUILD_ROOT}%{_datadir}/%{name}-agent/lib/libvirtqemuhook %{_sysconfdir}/libvirt/hooks/qemu
    /sbin/service libvirtd restart
    /sbin/chkconfig --add cloudstack-agent > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 cloudstack-agent on > /dev/null 2>&1 || true
fi

# if saved configs from upgrade exist, copy them over
if [ -f "%{_sysconfdir}/cloud.rpmsave/agent/agent.properties" ]; then
    mv %{_sysconfdir}/%{name}/agent/agent.properties  %{_sysconfdir}/%{name}/agent/agent.properties.rpmnew
    cp -p %{_sysconfdir}/cloud.rpmsave/agent/agent.properties %{_sysconfdir}/%{name}/agent
    # make sure we only do this on the first install of this RPM, don't want to overwrite on a reinstall
    mv %{_sysconfdir}/cloud.rpmsave/agent/agent.properties %{_sysconfdir}/cloud.rpmsave/agent/agent.properties.rpmsave
fi

%pre usage
id cloud > /dev/null 2>&1 || /usr/sbin/useradd -M -c "CloudStack unprivileged user" \
     -r -s /bin/sh -d %{_localstatedir}/cloudstack/management cloud|| true

%preun usage
/sbin/service cloudstack-usage stop || true
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del cloudstack-usage > /dev/null 2>&1 || true
    /sbin/service cloudstack-usage stop > /dev/null 2>&1 || true
fi

%post usage
if [ -f "%{_sysconfdir}/%{name}/management/db.properties" ]; then
    echo Replacing db.properties with management server db.properties
    rm -f %{_sysconfdir}/%{name}/usage/db.properties
    ln -s %{_sysconfdir}/%{name}/management/db.properties %{_sysconfdir}/%{name}/usage/db.properties
    /sbin/chkconfig --add cloudstack-usage > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 cloudstack-usage on > /dev/null 2>&1 || true
fi

if [ -f "%{_sysconfdir}/%{name}/management/key" ]; then
    echo Replacing key with management server key
    rm -f %{_sysconfdir}/%{name}/usage/key
    ln -s %{_sysconfdir}/%{name}/management/key %{_sysconfdir}/%{name}/usage/key
fi

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
%config(noreplace) %{_sysconfdir}/sysconfig/%{name}-management
%config(noreplace) %{_sysconfdir}/sudoers.d/%{name}-management
%config(noreplace) %{_sysconfdir}/security/limits.d/cloud
%config(noreplace) %attr(0640,root,cloud) %{_sysconfdir}/%{name}/management/db.properties
%config(noreplace) %{_sysconfdir}/%{name}/management/log4j-cloud.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/Catalina/localhost/client/context.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/catalina.properties
%config(noreplace) %{_sysconfdir}/%{name}/management/tomcat-users.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/web.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/server.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/environment.properties
%config(noreplace) %{_sysconfdir}/%{name}/management/java.security.ciphers
%config(noreplace) %{_sysconfdir}/%{name}/management/commons-logging.properties
%attr(0755,root,root) %{_unitdir}/%{name}-management.service
%attr(0755,cloud,cloud) %{_localstatedir}/run/%{name}-management.pid

%attr(0755,root,root) %{_bindir}/%{name}-setup-management
%attr(0755,root,root) %{_bindir}/%{name}-update-xenserver-licenses
%{_datadir}/%{name}-management/webapps
%{_datadir}/%{name}-management/bin
%{_datadir}/%{name}-management/conf
%{_datadir}/%{name}-management/lib
%{_datadir}/%{name}-management/logs
%{_datadir}/%{name}-management/temp
%{_datadir}/%{name}-management/work
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
%attr(0755,root,root) %{_bindir}/%{name}-external-ipallocator.py
%attr(0755,root,root) %{_initrddir}/%{name}-ipallocator
%dir %attr(0770,root,root) %{_localstatedir}/log/%{name}/ipallocator
%{_defaultdocdir}/%{name}-management-%{version}/LICENSE
%{_defaultdocdir}/%{name}-management-%{version}/NOTICE
%attr(0644,cloud,cloud) %{_localstatedir}/log/%{name}/management/catalina.out

%files agent
%attr(0755,root,root) %{_bindir}/%{name}-setup-agent
%attr(0755,root,root) %{_bindir}/%{name}-agent-upgrade
%attr(0755,root,root) %{_bindir}/%{name}-ssh
%attr(0755,root,root) %{_sysconfdir}/init.d/%{name}-agent
%attr(0644,root,root) %{_sysconfdir}/profile.d/%{name}-agent-profile.sh
%attr(0755,root,root) %{_datadir}/%{name}-common/scripts/network/cisco
%config(noreplace) %{_sysconfdir}/%{name}/agent
%dir %{_localstatedir}/log/%{name}/agent
%attr(0644,root,root) %{_datadir}/%{name}-agent/lib/*.jar
%attr(0755,root,root) %{_datadir}/%{name}-agent/lib/libvirtqemuhook
%dir %{_datadir}/%{name}-agent/plugins
%{_defaultdocdir}/%{name}-agent-%{version}/LICENSE
%{_defaultdocdir}/%{name}-agent-%{version}/NOTICE

%files common
%dir %attr(0755,root,root) %{python_sitearch}/cloudutils
%dir %attr(0755,root,root) %{_datadir}/%{name}-common/vms
%attr(0755,root,root) %{_datadir}/%{name}-common/scripts
%attr(0755,root,root) /usr/bin/cloudstack-sccs
%attr(0644, root, root) %{_datadir}/%{name}-common/vms/systemvm.iso
%attr(0644, root, root) %{_datadir}/%{name}-common/vms/systemvm.zip
%attr(0644,root,root) %{python_sitearch}/cloud_utils.py
%attr(0644,root,root) %{python_sitearch}/cloud_utils.pyc
%attr(0644,root,root) %{python_sitearch}/cloudutils/*
%attr(0644, root, root) %{_datadir}/%{name}-common/lib/jasypt-1.9.2.jar
%{_defaultdocdir}/%{name}-common-%{version}/LICENSE
%{_defaultdocdir}/%{name}-common-%{version}/NOTICE

%files usage
%attr(0644,root,root) %{_sysconfdir}/sysconfig/%{name}-usage
%attr(0755,root,root) /usr/sbin/%{name}-usage-sysd
%attr(0644,root,root) %{_unitdir}/%{name}-usage.service
%attr(0644,root,root) %{_datadir}/%{name}-usage/*.jar
%attr(0644,root,root) %{_datadir}/%{name}-usage/lib/*.jar
%dir %attr(0770,root,cloud) %{_localstatedir}/log/%{name}/usage
%attr(0644,root,root) %{_sysconfdir}/%{name}/usage/db.properties
%attr(0644,root,root) %{_sysconfdir}/%{name}/usage/log4j-cloud.xml
%attr(0644,cloud,cloud) %{_localstatedir}/run/%{name}-usage.pid
%{_defaultdocdir}/%{name}-usage-%{version}/LICENSE
%{_defaultdocdir}/%{name}-usage-%{version}/NOTICE

%files cli
%attr(0644,root,root) %{python_sitearch}/cloudapis.py
%attr(0644,root,root) %{python_sitearch}/cloudtool/__init__.py
%attr(0644,root,root) %{python_sitearch}/cloudtool/utils.py
%{_defaultdocdir}/%{name}-cli-%{version}/LICENSE
%{_defaultdocdir}/%{name}-cli-%{version}/NOTICE

%if "%{_ossnoss}" == "noredist"
%files mysql-ha
%defattr(0644,cloud,cloud,0755)
%attr(0644,root,root) %{_datadir}/%{name}-mysql-ha/lib/*
%{_defaultdocdir}/%{name}-mysql-ha-%{version}/LICENSE
%{_defaultdocdir}/%{name}-mysql-ha-%{version}/NOTICE
%endif

%files baremetal-agent
%attr(0755,root,root) %{_bindir}/cloudstack-setup-baremetal

%changelog
* Thu Apr 30 2015 Rohit Yadav <bhaisaab@apache.org> 4.6.0
- Remove awsapi package

* Wed Nov 19 2014 Hugo Trippaers <hugo@apache.org> 4.6.0
- Create a specific spec for CentOS 7

* Fri Jul 4 2014 Hugo Trippaers <hugo@apache.org> 4.5.0
- Add a package for the mysql ha module

* Fri Oct 5 2012 Hugo Trippaers <hugo@apache.org> 4.1.0
- new style spec file
