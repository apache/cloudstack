%define __os_install_post %{nil}
%global debug_package %{nil}

# DISABLE the post-percentinstall java repacking and line number stripping
# we need to find a way to just disable the java repacking and line number stripping, but not the autodeps

%define _ver 2.1.4
%define _rel 1

Name:      cloud
Summary:   Cloud.com Stack
Version:   %{_ver}
#http://fedoraproject.org/wiki/PackageNamingGuidelines#Pre-Release_packages
%if "%{?_prerelease}" != ""
Release:   0.%{_build_number}%{_prerelease}
%else
Release:   %{_rel}
%endif
License:   GPLv3+ with exceptions or CSL 1.1
Vendor:    Cloud.com, Inc. <sqa@cloud.com>
Packager:  Manuel Amador (Rudd-O) <manuel@cloud.com>
Group:     System Environment/Libraries
# FIXME do groups for every single one of the subpackages
Source0:   %{name}-%{_ver}.tar.bz2
BuildRoot: %{_tmppath}/%{name}-%{_ver}-%{release}-build

BuildRequires: java-1.6.0-openjdk-devel
BuildRequires: tomcat6
BuildRequires: ws-commons-util
#BuildRequires: commons-codec
BuildRequires: commons-dbcp
BuildRequires: commons-collections
BuildRequires: commons-httpclient
BuildRequires: jpackage-utils
BuildRequires: gcc
BuildRequires: glibc-devel

%global _premium %(tar jtvmf %{SOURCE0} '*/cloudstack-proprietary/' --occurrence=1 2>/dev/null | wc -l)

%description
This is the Cloud.com Stack, a highly-scalable elastic, open source,
intelligent cloud implementation.

%package utils
Summary:   Cloud.com utility library
Requires: java >= 1.6.0
Group:     System Environment/Libraries
Obsoletes: vmops-utils < %{version}-%{release}
%description utils
The Cloud.com utility libraries provide a set of Java classes used
in the Cloud.com Stack.

%package client-ui
Summary:   Cloud.com management server UI
Requires: %{name}-client
Group:     System Environment/Libraries
Obsoletes: vmops-client-ui < %{version}-%{release}
%description client-ui
The Cloud.com management server is the central point of coordination,
management, and intelligence in the Cloud.com Stack.  This package
is a requirement of the %{name}-client package, which installs the
Cloud.com management server.

%package server
Summary:   Cloud.com server library
Requires: java >= 1.6.0
Obsoletes: vmops-server < %{version}-%{release}
Requires: %{name}-utils = %{version}-%{release}, %{name}-core = %{version}-%{release}, %{name}-deps = %{version}-%{release}, tomcat6-servlet-2.5-api
Group:     System Environment/Libraries
%description server
The Cloud.com server libraries provide a set of Java classes used
in the Cloud.com Stack.

%package vnet
Summary:   Cloud.com-specific virtual network daemon
Requires: python
Requires: %{name}-daemonize = %{version}-%{release}
Requires: %{name}-python = %{version}-%{release}
Requires: net-tools
Requires: bridge-utils
Obsoletes: vmops-vnet < %{version}-%{release}
Group:     System Environment/Daemons
%description vnet
The Cloud.com virtual network daemon manages virtual networks used in the
Cloud.com Stack.

%package agent-scripts
Summary:   Cloud.com agent scripts
# FIXME nuke the archdependency
Requires: python
Requires: bash
Requires: bzip2
Requires: gzip
Requires: unzip
Requires: /sbin/mount.nfs
Requires: openssh-clients
Requires: nfs-utils
Obsoletes: vmops-agent-scripts < %{version}-%{release}
Group:     System Environment/Libraries
%description agent-scripts
The Cloud.com agent is in charge of managing shared computing resources in
a Cloud.com Stack-powered cloud.  Install this package if this computer
will participate in your cloud -- this is a requirement for the Cloud.com
agent.

%package python
Summary:   Cloud.com Python library
# FIXME nuke the archdependency
Requires: python
Group:     System Environment/Libraries
%description python
The Cloud.com Python library contains a few Python modules that the
CloudStack uses.

%package deps
Summary:   Cloud.com library dependencies
Requires: java >= 1.6.0
Obsoletes: vmops-deps < %{version}-%{release}
Group:     System Environment/Libraries
%description deps
This package contains a number of third-party dependencies
not shipped by distributions, required to run the Cloud.com
Stack.

%package daemonize
Summary:   Cloud.com daemonization utility
Group:     System Environment/Libraries
Obsoletes: vmops-daemonize < %{version}-%{release}
%description daemonize
This package contains a program that daemonizes the specified
process.  The Cloud.com Cloud Stack uses this to start the agent
as a service.

%package core
Summary:   Cloud.com core library
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}-%{release}, %{name}-deps = %{version}-%{release}
Group:     System Environment/Libraries
Obsoletes: vmops-core < %{version}-%{release}
%description core
The Cloud.com core libraries provide a set of Java classes used
in the Cloud.com Stack.

%package client
Summary:   Cloud.com management server
# If GCJ is present, a setPerformanceSomething method fails to load Catalina
Conflicts: java-1.5.0-gcj-devel
Obsoletes: vmops-client < %{version}-%{release}
Requires: java >= 1.6.0
Requires: %{name}-deps = %{version}-%{release}, %{name}-utils = %{version}-%{release}, %{name}-server = %{version}-%{release}
Requires: %{name}-client-ui = %{version}-%{release}
Requires: %{name}-setup = %{version}-%{release}
# reqs the agent-scripts package because of xenserver within the management server
Requires: %{name}-agent-scripts = %{version}-%{release}
Requires: %{name}-python = %{version}-%{release}
# for consoleproxy
# Requires: %{name}-agent
Requires: tomcat6
Requires: ws-commons-util
#Requires: commons-codec
Requires: commons-dbcp
Requires: commons-collections
Requires: commons-httpclient
Requires: jpackage-utils
Requires: sudo
Requires: /sbin/service
Requires: /sbin/chkconfig
Requires: /usr/bin/ssh-keygen
Requires: MySQL-python
Requires: python-paramiko
Requires: augeas >= 0.7.1
Group:     System Environment/Libraries
%description client
The Cloud.com management server is the central point of coordination,
management, and intelligence in the Cloud.com Stack.  This package
installs the management server..

%package setup
Summary:   Cloud.com setup tools
Obsoletes: vmops-setup < %{version}-%{release}
Requires: java >= 1.6.0
Requires: python
Requires: mysql
Requires: %{name}-utils = %{version}-%{release}
Requires: %{name}-server = %{version}-%{release}
Requires: %{name}-deps = %{version}-%{release}
Requires: %{name}-python = %{version}-%{release}
Requires: MySQL-python
Group:     System Environment/Libraries
%description setup
The Cloud.com setup tools let you set up your Management Server and Usage Server.

%package agent
Summary:   Cloud.com agent
Obsoletes: vmops-agent < %{version}-%{release}
Obsoletes: vmops-console < %{version}-%{release}
Obsoletes: cloud-console < %{version}-%{release}
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}-%{release}, %{name}-core = %{version}-%{release}, %{name}-deps = %{version}-%{release}
Requires: %{name}-agent-scripts = %{version}-%{release}
Requires: %{name}-vnet = %{version}-%{release}
Requires: %{name}-python = %{version}-%{release}
Requires: commons-httpclient
#Requires: commons-codec
Requires: commons-collections
Requires: commons-pool
Requires: commons-dbcp
Requires: jakarta-commons-logging
Requires: libvirt
Requires: /usr/sbin/libvirtd
Requires: jpackage-utils
Requires: %{name}-daemonize
Requires: /sbin/service
Requires: /sbin/chkconfig
Requires: kvm
Requires: libcgroup
Requires: /usr/bin/uuidgen
Requires: augeas >= 0.7.1
Requires: rsync
Group:     System Environment/Libraries
%description agent
The Cloud.com agent is in charge of managing shared computing resources in
a Cloud.com Stack-powered cloud.  Install this package if this computer
will participate in your cloud.

%package console-proxy
Summary:   Cloud.com console proxy
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}-%{release}, %{name}-core = %{version}-%{release}, %{name}-deps = %{version}-%{release}, %{name}-agent = %{version}-%{release}
Requires: commons-httpclient
#Requires: commons-codec
Requires: commons-collections
Requires: commons-pool
Requires: commons-dbcp
Requires: jakarta-commons-logging
Requires: jpackage-utils
Requires: %{name}-daemonize
Requires: /sbin/service
Requires: /sbin/chkconfig
Requires: /usr/bin/uuidgen
Requires: augeas >= 0.7.1
Group:     System Environment/Libraries
%description console-proxy
The Cloud.com console proxy is the service in charge of granting console
access into virtual machines managed by the Cloud.com CloudStack.


%if %{_premium}

%package test
Summary:   Cloud.com test suite
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}-%{release}, %{name}-deps = %{version}-%{release}, wget
Group:     System Environment/Libraries
Obsoletes: vmops-test < %{version}-%{release}
%description test
The Cloud.com test package contains a suite of automated tests
that the very much appreciated QA team at Cloud.com constantly
uses to help increase the quality of the Cloud.com Stack.

%package premium-deps
Summary:   Cloud.com premium library dependencies
Requires: java >= 1.6.0
Provides: %{name}-deps = %{version}-%{release}
Group:     System Environment/Libraries
Obsoletes: vmops-premium-deps < %{version}-%{release}
%description premium-deps
This package contains the certified software components required to run
the premium edition of the Cloud.com Stack.

%package premium
Summary:   Cloud.com premium components
Obsoletes: vmops-premium < %{version}-%{release}
Provides: %{name}-premium-plugin-zynga = %{version}-%{release}
Obsoletes: %{name}-premium-plugin-zynga < %{version}-%{release}
Provides: %{name}-premium-vendor-zynga = %{version}-%{release}
Obsoletes: %{name}-premium-vendor-zynga < %{version}-%{release}
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}-%{release}
Requires: %{name}-premium-deps
License:   CSL 1.1
Group:     System Environment/Libraries
%description premium
The Cloud.com premium components expand the range of features on your Cloud.com Stack.

%package usage
Summary:   Cloud.com usage monitor
Obsoletes: vmops-usage < %{version}-%{release}
Requires: java >= 1.6.0
Requires: %{name}-utils = %{version}-%{release}, %{name}-core = %{version}-%{release}, %{name}-deps = %{version}-%{release}, %{name}-server = %{version}-%{release}, %{name}-premium = %{version}-%{release}, %{name}-daemonize = %{version}-%{release}
Requires: %{name}-setup = %{version}-%{release}
Requires: %{name}-client = %{version}-%{release}
License:   CSL 1.1
Group:     System Environment/Libraries
%description usage
The Cloud.com usage monitor provides usage accounting across the entire cloud for
cloud operators to charge based on usage parameters.

%package cli
Summary:   Cloud.com command line tools
Requires: python
Group:     System Environment/Libraries
%description cli
The Cloud.com command line tools contain a few Python modules that can call cloudStack APIs.


%endif

%prep

%if %{_premium}
echo Doing premium build
%else
echo Doing open source build
%endif

%setup -q -n %{name}-%{_ver}

%build

# this fixes the /usr/com bug on centos5
%define _localstatedir /var
%define _sharedstatedir /var/lib
./waf configure --prefix=%{_prefix} --libdir=%{_libdir} --bindir=%{_bindir} --javadir=%{_javadir} --sharedstatedir=%{_sharedstatedir} --localstatedir=%{_localstatedir} --sysconfdir=%{_sysconfdir} --mandir=%{_mandir} --docdir=%{_docdir}/%{name}-%{version} --with-tomcat=%{_datadir}/tomcat6 --tomcat-user=%{name} --fast
./waf build --build-number=%{?_build_number}

%install
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}
# we put the build number again here, otherwise state checking will cause an almost-full recompile
./waf install --destdir=$RPM_BUILD_ROOT --nochown --build-number=%{?_build_number}

#%clean

[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}


%preun client
/sbin/service %{name}-management stop || true
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del %{name}-management  > /dev/null 2>&1 || true
    /sbin/service %{name}-management stop > /dev/null 2>&1 || true
fi

%pre client
id %{name} > /dev/null 2>&1 || /usr/sbin/useradd -M -c "Cloud.com unprivileged user" \
     -r -s /bin/sh -d %{_sharedstatedir}/%{name}/management %{name}|| true
# user harcoded here, also hardcoded on wscript

%post client
if [ "$1" == "1" ] ; then
    /sbin/chkconfig --add %{name}-management > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 %{name}-management on > /dev/null 2>&1 || true
fi
test -f %{_sharedstatedir}/%{name}/management/.ssh/id_rsa || su - %{name} -c 'yes "" 2>/dev/null | ssh-keygen -t rsa -q -N ""' < /dev/null



%if %{_premium}

%preun usage
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del %{name}-usage  > /dev/null 2>&1 || true
    /sbin/service %{name}-usage stop > /dev/null 2>&1 || true
fi

%pre usage
id %{name} > /dev/null 2>&1 || /usr/sbin/useradd -M -c "Cloud.com unprivileged user" \
     -r -s /bin/sh -d %{_sharedstatedir}/%{name}/management %{name}|| true
# user harcoded here, also hardcoded on wscript

%post usage
if [ "$1" == "1" ] ; then
    /sbin/chkconfig --add %{name}-usage > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 %{name}-usage on > /dev/null 2>&1 || true
else
    /sbin/service %{name}-usage condrestart >/dev/null 2>&1 || true
fi

%endif

%pre agent-scripts
id %{name} > /dev/null 2>&1 || /usr/sbin/useradd -M -c "Cloud.com unprivileged user" \
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

%preun console-proxy
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del %{name}-console-proxy  > /dev/null 2>&1 || true
    /sbin/service %{name}-console-proxy stop > /dev/null 2>&1 || true
fi

%post console-proxy
if [ "$1" == "1" ] ; then
    /sbin/chkconfig --add %{name}-console-proxy > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 %{name}-console-proxy on > /dev/null 2>&1 || true
else
    /sbin/service %{name}-console-proxy condrestart >/dev/null 2>&1 || true
fi

%preun vnet
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del %{name}-vnetd > /dev/null 2>&1 || true
    /sbin/service %{name}-vnetd stop > /dev/null 2>&1 || true
fi

%post vnet
if [ "$1" == "1" ] ; then
    /sbin/chkconfig --add %{name}-vnetd > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 %{name}-vnetd on > /dev/null 2>&1 || true
else
    /sbin/service %{name}-vnetd condrestart >/dev/null 2>&1 || true
fi


%files utils
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-utils.jar
%doc %{_docdir}/%{name}-%{version}/sccs-info
%doc %{_docdir}/%{name}-%{version}/configure-info
%doc %{_docdir}/%{name}-%{version}/version-info
%doc README
%doc HACKING
%doc debian/copyright

%files client-ui
%defattr(0644,root,root,0755)
%{_datadir}/%{name}/management/webapps/client/*
%doc README
%doc HACKING
%doc debian/copyright

%files server
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-server.jar
%{_sysconfdir}/%{name}/server/*
%doc README
%doc HACKING
%doc debian/copyright

%if %{_premium}

%files agent-scripts
%defattr(-,root,root,-)
%{_libdir}/%{name}/agent/scripts/*
# maintain the following list in sync with files agent-scripts
%if %{_premium}
%exclude %{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/check_heartbeat.sh
%exclude %{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/find_bond.sh
%exclude %{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/launch_hb.sh
%exclude %{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/setup_heartbeat_sr.sh
%exclude %{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/vmopspremium
%exclude %{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/xenheartbeat.sh
%exclude %{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/xenserver56/patch-premium
%exclude %{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/xs_cleanup.sh
%endif
%{_libdir}/%{name}/agent/vms/systemvm.zip
%doc README
%doc HACKING
%doc debian/copyright

%else

%files agent-scripts
%defattr(-,root,root,-)
%{_libdir}/%{name}/agent/scripts/installer/*
%{_libdir}/%{name}/agent/scripts/network/domr/*.sh
%{_libdir}/%{name}/agent/scripts/storage/*.sh
%{_libdir}/%{name}/agent/scripts/storage/zfs/*
%{_libdir}/%{name}/agent/scripts/storage/qcow2/*
%{_libdir}/%{name}/agent/scripts/storage/secondary/*
%{_libdir}/%{name}/agent/scripts/util/*
%{_libdir}/%{name}/agent/scripts/vm/*.sh
%{_libdir}/%{name}/agent/scripts/vm/storage/nfs/*
%{_libdir}/%{name}/agent/scripts/vm/storage/iscsi/*
%{_libdir}/%{name}/agent/scripts/vm/network/*
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/*.sh
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/kvm/*
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/xen/*
%{_libdir}/%{name}/agent/vms/systemvm.zip
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/*
%doc README
%doc HACKING
%doc debian/copyright

%endif

%files daemonize
%defattr(-,root,root,-)
%attr(755,root,root) %{_bindir}/%{name}-daemonize
%doc README
%doc HACKING
%doc debian/copyright

%files deps
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-commons-codec-1.4.jar
%{_javadir}/%{name}-apache-log4j-extras-1.0.jar
%{_javadir}/%{name}-backport-util-concurrent-3.0.jar
%{_javadir}/%{name}-ehcache.jar
%{_javadir}/%{name}-email.jar
%{_javadir}/%{name}-gson-1.3.jar
%{_javadir}/%{name}-httpcore-4.0.jar
%{_javadir}/%{name}-jna.jar
%{_javadir}/%{name}-junit-4.8.1.jar
%{_javadir}/%{name}-libvirt-0.4.5.jar
%{_javadir}/%{name}-log4j.jar
%{_javadir}/%{name}-trilead-ssh2-build213.jar
%{_javadir}/%{name}-cglib.jar
%{_javadir}/%{name}-mysql-connector-java-5.1.7-bin.jar
%{_javadir}/%{name}-xenserver-5.5.0-1.jar
%{_javadir}/%{name}-xmlrpc-common-3.*.jar
%{_javadir}/%{name}-xmlrpc-client-3.*.jar
%{_javadir}/%{name}-manageontap.jar
%doc README
%doc HACKING
%doc debian/copyright

%files core
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-core.jar
%doc README
%doc HACKING
%doc debian/copyright

%files vnet
%defattr(0644,root,root,0755)
%attr(0755,root,root) %{_sbindir}/%{name}-vnetd
%attr(0755,root,root) %{_sbindir}/%{name}-vn
%attr(0755,root,root) %{_initrddir}/%{name}-vnetd
%doc README
%doc HACKING
%doc debian/copyright

%files python
%defattr(0644,root,root,0755)
%{_prefix}/lib*/python*/site-packages/%{name}_PrettyPrint.*
%{_prefix}/lib*/python*/site-packages/%{name}_sxp.*
%{_prefix}/lib*/python*/site-packages/%{name}_utils.*
%doc README
%doc HACKING
%doc debian/copyright

%files setup
%attr(0755,root,root) %{_bindir}/%{name}-setup-databases
%attr(0755,root,root) %{_bindir}/%{name}-migrate-databases
%attr(0755,root,root) %{_bindir}/%{name}-migrate-snapshot
%attr(0755,root,root) %{_bindir}/%{name}-check-snapshot
%dir %{_datadir}/%{name}/setup
%{_datadir}/%{name}/setup/create-database.sql
%{_datadir}/%{name}/setup/create-index-fk.sql
%{_datadir}/%{name}/setup/create-schema.sql
%{_datadir}/%{name}/setup/server-setup.sql
%{_datadir}/%{name}/setup/templates.kvm.sql
%{_datadir}/%{name}/setup/templates.xenserver.sql
%{_datadir}/%{name}/setup/deploy-db-dev.sh
%{_datadir}/%{name}/setup/server-setup.xml
%{_datadir}/%{name}/setup/data-20to21.sql
%{_datadir}/%{name}/setup/index-20to21.sql
%{_datadir}/%{name}/setup/index-212to213.sql
%{_datadir}/%{name}/setup/postprocess-20to21.sql
%{_datadir}/%{name}/setup/schema-20to21.sql
%{_datadir}/%{name}/setup/schema-213to214.sql
%{_datadir}/%{name}/setup/schema-214to215.sql
%{_datadir}/%{name}/setup/schema-level.sql
%{_datadir}/%{name}/setup/data-214to215.sql
%doc README
%doc HACKING
%doc debian/copyright

%files client
%defattr(0644,root,root,0755)
%{_sysconfdir}/%{name}/management/catalina.policy
%{_sysconfdir}/%{name}/management/catalina.properties
%{_sysconfdir}/%{name}/management/commands.properties
%{_sysconfdir}/%{name}/management/components.xml
%{_sysconfdir}/%{name}/management/context.xml
%config(noreplace) %attr(640,root,%{name}) %{_sysconfdir}/%{name}/management/db.properties
%{_sysconfdir}/%{name}/management/environment.properties
%{_sysconfdir}/%{name}/management/ehcache.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/log4j-%{name}.xml
%{_sysconfdir}/%{name}/management/logging.properties
%{_sysconfdir}/%{name}/management/server.xml
%config(noreplace) %{_sysconfdir}/%{name}/management/tomcat6.conf
%{_sysconfdir}/%{name}/management/classpath.conf
%{_sysconfdir}/%{name}/management/tomcat-users.xml
%{_sysconfdir}/%{name}/management/web.xml
%dir %attr(770,root,%{name}) %{_sysconfdir}/%{name}/management/Catalina
%dir %attr(770,root,%{name}) %{_sysconfdir}/%{name}/management/Catalina/localhost
%dir %attr(770,root,%{name}) %{_sysconfdir}/%{name}/management/Catalina/localhost/client
%config %{_sysconfdir}/sysconfig/%{name}-management
%attr(0755,root,root) %{_initrddir}/%{name}-management
%dir %{_datadir}/%{name}/management
%{_datadir}/%{name}/management/bin
%{_datadir}/%{name}/management/conf
%{_datadir}/%{name}/management/lib
%{_datadir}/%{name}/management/logs
%{_datadir}/%{name}/management/temp
%{_datadir}/%{name}/management/work
%attr(755,root,root) %{_bindir}/%{name}-setup-management
%attr(755,root,root) %{_bindir}/%{name}-update-xenserver-licenses
%dir %attr(770,root,%{name}) %{_sharedstatedir}/%{name}/mnt
%dir %attr(770,%{name},%{name}) %{_sharedstatedir}/%{name}/management
%dir %attr(770,root,%{name}) %{_localstatedir}/cache/%{name}/management
%dir %attr(770,root,%{name}) %{_localstatedir}/cache/%{name}/management/work
%dir %attr(770,root,%{name}) %{_localstatedir}/cache/%{name}/management/temp
%dir %attr(770,root,%{name}) %{_localstatedir}/log/%{name}/management
%dir %attr(770,root,%{name}) %{_localstatedir}/log/%{name}/agent
%doc README
%doc HACKING
%doc debian/copyright

%files agent
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-agent.jar
%config(noreplace) %{_sysconfdir}/%{name}/agent/agent.properties
%config %{_sysconfdir}/%{name}/agent/developer.properties.template
%config %{_sysconfdir}/%{name}/agent/environment.properties
%config(noreplace) %{_sysconfdir}/%{name}/agent/log4j-%{name}.xml
%attr(0755,root,root) %{_initrddir}/%{name}-agent
%attr(0755,root,root) %{_libexecdir}/agent-runner
%{_libdir}/%{name}/agent/css
%{_libdir}/%{name}/agent/ui
%{_libdir}/%{name}/agent/js
%{_libdir}/%{name}/agent/images
%attr(0755,root,root) %{_bindir}/%{name}-setup-agent
%dir %attr(770,root,root) %{_localstatedir}/log/%{name}/agent
%doc README
%doc HACKING
%doc debian/copyright

%files console-proxy
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-console*.jar
%config(noreplace) %{_sysconfdir}/%{name}/console-proxy/agent.properties
%config(noreplace) %{_sysconfdir}/%{name}/console-proxy/consoleproxy.properties
%config(noreplace) %{_sysconfdir}/%{name}/console-proxy/log4j-%{name}.xml
%attr(0755,root,root) %{_initrddir}/%{name}-console-proxy
%attr(0755,root,root) %{_libexecdir}/console-proxy-runner
%{_libdir}/%{name}/console-proxy/*
%attr(0755,root,root) %{_bindir}/%{name}-setup-console-proxy
%dir %attr(770,root,root) %{_localstatedir}/log/%{name}/console-proxy
%doc README
%doc HACKING
%doc debian/copyright

%if %{_premium}

%files test
%defattr(0644,root,root,0755)
%attr(755,root,root) %{_bindir}/%{name}-run-test
%{_javadir}/%{name}-test.jar
%{_sharedstatedir}/%{name}/test/*
%{_libdir}/%{name}/test/*
%{_sysconfdir}/%{name}/test/*
%doc README
%doc HACKING
%doc debian/copyright

%files premium-deps
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-premium/*.jar
%doc README
%doc HACKING
%doc debian/copyright

%files premium
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-core-extras.jar
%{_javadir}/%{name}-server-extras.jar
%{_sysconfdir}/%{name}/management/commands-ext.properties
%{_sysconfdir}/%{name}/management/components-premium.xml
%{_libdir}/%{name}/agent/vms/systemvm-premium.zip
%{_datadir}/%{name}/setup/create-database-premium.sql
%{_datadir}/%{name}/setup/create-schema-premium.sql
# maintain the following list in sync with files agent-scripts
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/check_heartbeat.sh
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/find_bond.sh
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/launch_hb.sh
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/setup_heartbeat_sr.sh
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/vmopspremium
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/xenheartbeat.sh
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/xenserver56/patch-premium
%{_libdir}/%{name}/agent/scripts/vm/hypervisor/xenserver/xs_cleanup.sh
%doc README
%doc HACKING
%doc debian/copyright

%files usage
%defattr(0644,root,root,0755)
%{_javadir}/%{name}-usage.jar
%attr(0755,root,root) %{_initrddir}/%{name}-usage
%attr(0755,root,root) %{_libexecdir}/usage-runner
%dir %attr(770,root,%{name}) %{_localstatedir}/log/%{name}/usage
%{_sysconfdir}/%{name}/usage/usage-components.xml
%config(noreplace) %{_sysconfdir}/%{name}/usage/log4j-%{name}_usage.xml
%config(noreplace) %attr(640,root,%{name}) %{_sysconfdir}/%{name}/usage/db.properties
%doc README
%doc HACKING
%doc debian/copyright

%files cli
%{_bindir}/%{name}-tool
%{_bindir}/%{name}voladm
%{_sysconfdir}/%{name}/cli/commands.xml
%dir %{_prefix}/lib*/python*/site-packages/%{name}tool
%{_prefix}/lib*/python*/site-packages/%{name}tool/*
%{_prefix}/lib*/python*/site-packages/%{name}apis.py
%doc README
%doc HACKING
%doc debian/copyright

%endif

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

