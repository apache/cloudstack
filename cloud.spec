%define __os_install_post %{nil}
%global debug_package %{nil}

# DISABLE the post-percentinstall java repacking and line number stripping
# we need to find a way to just disable the java repacking and line number stripping, but not the autodeps

Name:      cloud
Summary:   CloudStack IaaS Platform
Version:   %{_ver}
#http://fedoraproject.org/wiki/PackageNamingGuidelines#Pre-Release_packages
%if "%{?_prerelease}" != ""
Release:   0.%{_build_number}%{dist}.%{_prerelease}
%else
Release:   %{_rel}%{dist}
%endif
License:   GPLv3+ with exceptions or CSL 1.1
Vendor:    Cloud.com, Inc. <sqa@cloud.com>
Packager:  Cloud.com, Inc. <engineering@cloud.com>
Group:     System Environment/Libraries
# FIXME do groups for every single one of the subpackages
Source0:   %{name}-%{_ver}.tar.bz2
BuildRoot: %{_tmppath}/%{name}-%{_ver}-%{release}-build

BuildRequires: java-1.6.0-openjdk-devel
BuildRequires: tomcat6
BuildRequires: ws-commons-util
BuildRequires: jpackage-utils
BuildRequires: gcc
BuildRequires: glibc-devel
BuildRequires: /usr/bin/mkisofs
BuildRequires: MySQL-python

%description
CloudStack is a highly-scalable elastic, open source,
intelligent IaaS cloud implementation.

%package utils
Summary:   CloudStack utility library
Requires: java >= 1.6.0
Requires: python
Group:     System Environment/Libraries
Obsoletes: vmops-utils < %{version}-%{release}
%description utils
Utility libraries and set of Java classes used
by CloudStack.

%package client-ui
Summary:   CloudStack management server UI
Requires: %{name}-client
Group:     System Environment/Libraries
Obsoletes: vmops-client-ui < %{version}-%{release}
%description client-ui
The CloudStack management server is the central point of coordination,
management, and intelligence in CloudStack.  This package
is a requirement of the %{name}-client package, which installs the
CloudStack management server.

%package server
Summary:   CloudStack server library
Requires: java >= 1.6.0
Obsoletes: vmops-server < %{version}-%{release}
Requires: %{name}-utils = %{version}, %{name}-core = %{version}, %{name}-deps = %{version}, tomcat6-servlet-2.5-api
Group:     System Environment/Libraries
%description server
The CloudStack server libraries provide a set of Java classes for CloudStack.

%package agent-scripts
Summary:   CloudStack agent scripts
# FIXME nuke the archdependency
Requires: python
Requires: bash
Requires: bzip2
Requires: gzip
Requires: unzip
Requires: /sbin/mount.nfs
Requires: openssh-clients
Requires: nfs-utils
Requires: wget
# there is a fsimage.so in the source code, which adds xen-libs as a dependence, needs to supress it, as rhel doesn't have this pacakge
AutoReqProv: no
Obsoletes: vmops-agent-scripts < %{version}-%{release}
Group:     System Environment/Libraries
%description agent-scripts
The CloudStack agent is in charge of managing shared computing resources in
a KVM-powered cloud.  Install this package if this computer 
will participate in your cloud -- this is a requirement for the CloudStack KVM
agent.

%package python
Summary:   CloudStack Python library
# FIXME nuke the archdependency
Requires: python
Group:     System Environment/Libraries
%description python
The CloudStack Python library contains a few Python modules that the
CloudStack uses.

%package deps
Summary:   CloudStack library dependencies
Requires: java >= 1.6.0
Obsoletes: vmops-deps < %{version}-%{release}
Group:     System Environment/Libraries
%description deps
This package contains a number of third-party dependencies
not shipped by distributions, required to run CloudStack


%package core
Summary:   CloudStack core library
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}, %{name}-deps = %{version}
Group:     System Environment/Libraries
Obsoletes: vmops-core < %{version}-%{release}
%description core
The CloudStack core libraries provide a set of Java classes used
in CloudStack.

%package client
Summary:   CloudStack management server
# If GCJ is present, a setPerformanceSomething method fails to load Catalina
Conflicts: java-1.5.0-gcj-devel
Obsoletes: vmops-client < %{version}-%{release}
Obsoletes: cloud-premium < %{version}-%{release}
Requires: java >= 1.6.0
Requires: %{name}-deps = %{version}, %{name}-utils = %{version}, %{name}-server = %{version}
Requires: %{name}-client-ui = %{version}
Requires: %{name}-setup = %{version}
# reqs the agent-scripts package because of xenserver within the management server
Requires: %{name}-agent-scripts = %{version}
Requires: %{name}-python = %{version}
# for consoleproxy
# Requires: %{name}-agent
Requires: tomcat6
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
Requires: %{name}-utils = %{version}
%if 0%{?fedora} > 14 
Requires: apache-commons-dbcp
Requires: apache-commons-collections
Requires: jakarta-commons-httpclient
%endif

Group:     System Environment/Libraries
%description client
The CloudStack management server is the central point of coordination,
management, and intelligence in CloudStack and installs the management server. 

%package setup
Summary:   CloudStack setup tools
Obsoletes: vmops-setup < %{version}-%{release}
Requires: java >= 1.6.0
Requires: python
Requires: MySQL-python
Requires: %{name}-utils = %{version}
Requires: %{name}-server = %{version}
Requires: %{name}-deps = %{version}
Requires: %{name}-python = %{version}
Group:     System Environment/Libraries
%description setup
The CloudStack setup tools let you set up your Management Server and Usage Server.

%package agent-libs
Summary:   CloudStack agent libraries
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}, %{name}-core = %{version}, %{name}-deps = %{version}
Requires: commons-httpclient
Requires: jakarta-commons-logging
Requires: jpackage-utils
Group:     System Environment/Libraries
%description agent-libs
The CloudStack agent libraries are used by the KVM Agent 

%package agent
Summary:   CloudStack agent
Obsoletes: vmops-agent < %{version}-%{release}
Obsoletes: vmops-console < %{version}-%{release}
Obsoletes: cloud-console < %{version}-%{release}
Obsoletes: cloud-vnet < %{version}-%{release}
Obsoletes: cloud-premium-agent < %{version}-%{release}
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}, %{name}-core = %{version}, %{name}-deps = %{version}
Requires: %{name}-agent-libs = %{version}
Requires: %{name}-agent-scripts = %{version}
Requires: python
Requires: %{name}-python = %{version}
Requires: commons-httpclient
Requires: jakarta-commons-logging
Requires: libvirt
Requires: /usr/sbin/libvirtd
Requires: jpackage-utils
Requires: /sbin/service
Requires: /sbin/chkconfig
Requires: jna
Requires: ebtables
Group:     System Environment/Libraries

Requires: kvm

%if 0%{?fedora} >= 14 && 0%{?fedora} != 16
Requires: cloud-qemu-kvm
Requires: cloud-qemu-img
%endif

%if 0%{?rhel} >= 5
Requires: qemu-img
%endif

Requires: libcgroup
%if 0%{?fedora} >= 16
Requires: libcgroup-tools
%endif
Requires: /usr/bin/uuidgen
Requires: rsync
Requires: /bin/egrep
Requires: /sbin/ip
Requires: vconfig
Group:     System Environment/Libraries
%description agent
The CloudStack agent is in charge of managing KVM shared computing resources in
a CloudStack-powered cloud.  Install this package if this computer
will participate in your cloud.

%package baremetal-agent
Summary: CloudStack baremetal agent
Requires: PING
Requires: tftp-server
Requires: xinetd
Requires: syslinux
Requires: chkconfig
Requires: dhcp
Group:     System Environment/Libraries
%description baremetal-agent
The CloudStack baremetal agent

%package cli
Summary:   CloudStack command line tools
Requires: python
Group:     System Environment/Libraries
%description cli
The CloudStack command line tools contain a few Python modules that can call cloudStack APIs.

%package test
Summary:   CloudStack test suite
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}, %{name}-deps = %{version}, wget
Group:     System Environment/Libraries
Obsoletes: vmops-test < %{version}-%{release}
%description test
The CloudStack test package contains a suite of automated tests
that the very much appreciated CloudStack QA team constantly
uses to help increase the quality of the CloudStack.

%package usage
Summary:   CloudStack usage monitor
Obsoletes: vmops-usage < %{version}-%{release}
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}, %{name}-core = %{version}, %{name}-deps = %{version}, %{name}-server = %{version} 
Requires: %{name}-setup = %{version}
Requires: %{name}-client = %{version}
License:   GPLv3+
Group:     System Environment/Libraries
%description usage
The CloudStack usage monitor provides usage accounting across the entire cloud for
cloud operators to charge based on usage parameters.


%prep

echo Doing CloudStack build

%setup -q -n %{name}-%{_ver}

%build

# this fixes the /usr/com bug on centos5
%define _localstatedir /var
%define _sharedstatedir /var/lib
./waf configure --prefix=%{_prefix} --libdir=%{_libdir} --bindir=%{_bindir} --javadir=%{_javadir} --sharedstatedir=%{_sharedstatedir} --localstatedir=%{_localstatedir} --sysconfdir=%{_sysconfdir} --mandir=%{_mandir} --docdir=%{_docdir}/%{name}-%{version} --with-tomcat=%{_datadir}/tomcat6 --tomcat-user=%{name} --fast --build-number=%{_ver}-%{release} --package-version=%{_ver}
./waf build --build-number=%{?_build_number} --package-version=%{_ver}

%install
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}
# we put the build number again here, otherwise state checking will cause an almost-full recompile
./waf install --destdir=$RPM_BUILD_ROOT --nochown --build-number=%{?_build_number}

%clean

[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}


%preun client
/sbin/service %{name}-management stop || true
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del %{name}-management  > /dev/null 2>&1 || true
    /sbin/service %{name}-management stop > /dev/null 2>&1 || true
fi

%pre client
id %{name} > /dev/null 2>&1 || /usr/sbin/useradd -M -c "CloudStack unprivileged user" \
     -r -s /bin/sh -d %{_sharedstatedir}/%{name}/management %{name}|| true

# set max file descriptors for cloud user to 4096
sed -i /"cloud hard nofile"/d /etc/security/limits.conf
sed -i /"cloud soft nofile"/d /etc/security/limits.conf
echo "cloud hard nofile 4096" >> /etc/security/limits.conf
echo "cloud soft nofile 4096" >> /etc/security/limits.conf
rm -rf %{_localstatedir}/cache/%{name}
# user harcoded here, also hardcoded on wscript

%pre client-ui
if [ -d %{_datadir}/%{name}/management/webapps/client/ ]; then
	pushd /tmp &>/dev/null
	file=cloud-ui-backup-%(date +%%F).tar.bz2
	cp -r %{_datadir}/%{name}/management/webapps/client/ .
	tar cjf "$file" client/
	rm -rf client/
	mkdir -p /usr/share/cloud/ui-backup/
	mv "$file" /usr/share/cloud/ui-backup/
	popd &>/dev/null
fi

%post client
if [ "$1" == "1" ] ; then
    /sbin/chkconfig --add %{name}-management > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 %{name}-management on > /dev/null 2>&1 || true
fi

%preun usage
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del %{name}-usage  > /dev/null 2>&1 || true
    /sbin/service %{name}-usage stop > /dev/null 2>&1 || true
fi

%pre usage
id %{name} > /dev/null 2>&1 || /usr/sbin/useradd -M -c "CloudStack unprivileged user" \
     -r -s /bin/sh -d %{_sharedstatedir}/%{name}/management %{name}|| true
# user harcoded here, also hardcoded on wscript

%post usage
if [ "$1" == "1" ] ; then
    /sbin/chkconfig --add %{name}-usage > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 %{name}-usage on > /dev/null 2>&1 || true
else
    /sbin/service %{name}-usage condrestart >/dev/null 2>&1 || true
fi

%pre agent-scripts
id %{name} > /dev/null 2>&1 || /usr/sbin/useradd -M -c "CloudStack unprivileged user" \
     -r -s /bin/sh -d %{_sharedstatedir}/%{name}/management %{name}|| true

%preun agent
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del %{name}-agent  > /dev/null 2>&1 || true
    /sbin/service %{name}-agent stop > /dev/null 2>&1 || true
fi

%post agent
if [ "$1" == "1" ] ; then
    /sbin/chkconfig --add %{name}-agent > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 %{name}-agent on > /dev/null 2>&1 || true
else
    /sbin/service %{name}-agent condrestart >/dev/null 2>&1 || true
fi


%files utils
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-utils.jar
%{_javadir}/%{name}-api.jar
%attr(0755,root,root) %{_bindir}/cloud-sccs
%attr(0755,root,root) %{_bindir}/cloud-gitrevs
%doc %{_docdir}/%{name}-%{version}/sccs-info
%doc %{_docdir}/%{name}-%{version}/version-info
%doc %{_docdir}/%{name}-%{version}/configure-info
%doc README.html
%doc debian/copyright

%files client-ui
%defattr(0644,root,root,0755)
%{_datadir}/%{name}/management/webapps/client/*

%files server
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-server.jar
%{_javadir}/%{name}-vmware-base.jar
%{_javadir}/%{name}-ovm.jar
%config(noreplace) %{_sysconfdir}/%{name}/server/*

%files agent-scripts
%defattr(-,root,root,-)
%{_libdir}/%{name}/agent/scripts/*
# maintain the following list in sync with files agent-scripts
%{_libdir}/%{name}/agent/vms/systemvm.zip
%{_libdir}/%{name}/agent/vms/systemvm.iso


%files deps
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-commons-codec-1.5.jar
%{_javadir}/%{name}-commons-dbcp-1.4.jar
%{_javadir}/%{name}-commons-pool-1.5.6.jar
%{_javadir}/%{name}-commons-httpclient-3.1.jar
%{_javadir}/%{name}-google-gson-1.7.1.jar
%{_javadir}/%{name}-netscaler.jar
%{_javadir}/%{name}-netscaler-sdx.jar
%{_javadir}/%{name}-log4j-extras.jar
%{_javadir}/%{name}-backport-util-concurrent-3.0.jar
%{_javadir}/%{name}-ehcache.jar
%{_javadir}/%{name}-email.jar
%{_javadir}/%{name}-httpcore-4.0.jar
%{_javadir}/%{name}-libvirt-0.4.5.jar
%{_javadir}/%{name}-log4j.jar
%{_javadir}/%{name}-trilead-ssh2-build213.jar
%{_javadir}/%{name}-cglib.jar
%{_javadir}/%{name}-mysql-connector-java-5.1.7-bin.jar
%{_javadir}/%{name}-xenserver-5.6.100-1.jar
%{_javadir}/%{name}-xmlrpc-common-3.*.jar
%{_javadir}/%{name}-xmlrpc-client-3.*.jar
%{_javadir}/%{name}-jstl-1.2.jar
%{_javadir}/jetty-6.1.26.jar
%{_javadir}/jetty-util-6.1.26.jar
%{_javadir}/%{name}-axis.jar
%{_javadir}/%{name}-commons-discovery.jar
%{_javadir}/%{name}-wsdl4j.jar
%{_javadir}/%{name}-bcprov-jdk16-1.45.jar
%{_javadir}/%{name}-jsch-0.1.42.jar
%{_javadir}/%{name}-iControl.jar
%{_javadir}/%{name}-manageontap.jar
%{_javadir}/vmware*.jar
%{_javadir}/%{name}-jnetpcap.jar
%{_javadir}/%{name}-junit.jar
%{_javadir}/%{name}-jasypt-1.8.jar
%{_javadir}/%{name}-commons-configuration-1.8.jar
%{_javadir}/%{name}-commons-lang-2.6.jar


%files core
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-core.jar

%files python
%defattr(0644,root,root,0755)
%{_prefix}/lib*/python*/site-packages/%{name}*
%attr(0755,root,root) %{_bindir}/cloud-external-ipallocator.py
%attr(0755,root,root) %{_initrddir}/cloud-ipallocator
%dir %attr(0770,root,root) %{_localstatedir}/log/%{name}/ipallocator

%files setup
%attr(0755,root,root) %{_bindir}/%{name}-setup-databases
%attr(0755,root,root) %{_bindir}/%{name}-migrate-databases
%attr(0755,root,root) %{_bindir}/%{name}-set-guest-password
%attr(0755,root,root) %{_bindir}/%{name}-set-guest-sshkey
%attr(0755,root,root) %{_bindir}/%{name}-sysvmadm
%attr(0755,root,root) %{_bindir}/%{name}-setup-encryption
%dir %{_datadir}/%{name}/setup
%{_datadir}/%{name}/setup/*.sql
%{_datadir}/%{name}/setup/db/*.sql
%{_datadir}/%{name}/setup/*.sh
%{_datadir}/%{name}/setup/server-setup.xml

%files client
%defattr(0644,root,root,0775)
%config(noreplace) %{_sysconfdir}/%{name}/management/*
%config(noreplace) %attr(0640,root,%{name}) %{_sysconfdir}/%{name}/management/db.properties
%config(noreplace) %{_sysconfdir}/%{name}/management/log4j-%{name}.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/tomcat6.conf
%dir %attr(0770,root,%{name}) %{_sysconfdir}/%{name}/management/Catalina
%dir %attr(0770,root,%{name}) %{_sysconfdir}/%{name}/management/Catalina/localhost
%dir %attr(0770,root,%{name}) %{_sysconfdir}/%{name}/management/Catalina/localhost/client
%config(noreplace) %{_sysconfdir}/sysconfig/%{name}-management
%attr(0755,root,root) %{_initrddir}/%{name}-management
%dir %{_datadir}/%{name}/management
%{_datadir}/%{name}/management/*
%attr(0755,root,root) %{_bindir}/%{name}-setup-management
%attr(0755,root,root) %{_bindir}/%{name}-update-xenserver-licenses
%dir %attr(0770,root,%{name}) %{_sharedstatedir}/%{name}/mnt
%dir %attr(0770,%{name},%{name}) %{_sharedstatedir}/%{name}/management
%dir %attr(0770,root,%{name}) %{_localstatedir}/cache/%{name}/management
%dir %attr(0770,root,%{name}) %{_localstatedir}/cache/%{name}/management/work
%dir %attr(0770,root,%{name}) %{_localstatedir}/cache/%{name}/management/temp
%dir %attr(0770,root,%{name}) %{_localstatedir}/log/%{name}/management
%dir %attr(0770,root,%{name}) %{_localstatedir}/log/%{name}/agent

%files agent-libs
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-agent.jar

%files agent
%defattr(0644,root,root,0755)
%config(noreplace) %{_sysconfdir}/%{name}/agent/agent.properties
%config(noreplace) %{_sysconfdir}/%{name}/agent/dummy.agent.properties
%config(noreplace) %{_sysconfdir}/%{name}/agent/developer.properties.template
%config(noreplace)  %{_sysconfdir}/%{name}/agent/environment.properties
%config(noreplace) %{_sysconfdir}/%{name}/agent/log4j-%{name}.xml
%attr(0755,root,root) %{_initrddir}/%{name}-agent
%attr(0755,root,root) %{_libexecdir}/agent-runner
%attr(0755,root,root) %{_bindir}/%{name}-setup-agent
%dir %attr(0770,root,root) %{_localstatedir}/log/%{name}/agent
%attr(0755,root,root) %{_bindir}/mycloud-setup-agent

%files cli
%{_bindir}/%{name}-tool
%{_bindir}/cloudvoladm
%{_bindir}/cloud-grab-dependent-library-versions
%config(noreplace) %{_sysconfdir}/%{name}/cli/commands.xml
%dir %{_prefix}/lib*/python*/site-packages/%{name}tool
%{_prefix}/lib*/python*/site-packages/%{name}tool/*
%{_prefix}/lib*/python*/site-packages/%{name}apis.py

%files baremetal-agent
%attr(0755,root,root) %{_bindir}/cloud-setup-baremetal

%files test
%defattr(0644,root,root,0755)
%attr(0755,root,root) %{_bindir}/%{name}-run-test
%{_javadir}/%{name}-test.jar
%{_sharedstatedir}/%{name}/test/*
%{_libdir}/%{name}/test/*
%config(noreplace) %{_sysconfdir}/%{name}/test/*

%files usage
%defattr(0644,root,root,0775)
%{_javadir}/%{name}-usage.jar
%attr(0755,root,root) %{_initrddir}/%{name}-usage
%attr(0755,root,root) %{_libexecdir}/usage-runner
%dir %attr(0770,root,%{name}) %{_localstatedir}/log/%{name}/usage
%config(noreplace) %{_sysconfdir}/%{name}/usage/usage-components.xml
%config(noreplace) %{_sysconfdir}/%{name}/usage/log4j-%{name}_usage.xml
%config(noreplace) %attr(0640,root,%{name}) %{_sysconfdir}/%{name}/usage/db.properties

%changelog
* Mon May 3 2010 Manuel Amador (Rudd-O) <manuel@vmops.com> 1.9.12
- Bump version for RC4 release

%changelog
* Fri Apr 30 2010 Manuel Amador (Rudd-O) <manuel@vmops.com> 1.9.11
- Rename to Cloud.com everywhere

* Wed Apr 28 2010 Manuel Amador (Rudd-O) <manuel@vmops.com> 1.9.10
- FOSS release

%changelog
* Mon Apr 05 2010 Manuel Amador (Rudd-O) <manuel@vmops.com> 1.9.8
- RC3 branched

* Wed Feb 17 2010 Manuel Amador (Rudd-O) <manuel@vmops.com> 1.9.7
- First initial broken-up release

