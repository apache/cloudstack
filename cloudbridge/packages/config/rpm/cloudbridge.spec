%define __os_install_post %{nil}
%global debug_package %{nil}

%define _rel 1

Name:      cloud-bridge
Summary:   CloudStack CloudBridge 
Version:   %{_ver}
#http://fedoraproject.org/wiki/PackageNamingGuidelines#Pre-Release_packages
%if "%{?_prerelease}" != ""
Release:   0.%{_build_number}%{_prerelease}
%else
Release:   %{_rel}
%endif
License:   GPLv3+ with exceptions or CSL 1.1
Vendor:    Citrix Systems, Inc. <sqa@cloud.com>
Packager:  Citrix Systems, Inc. <cloud@cloud.com>
Source0:   cloud-bridge-%{_ver}.tar.bz2
Group:     System Environment/Libraries
Requires: java >= 1.6.0
Requires: tomcat6
Obsoletes: cloud-bridge < %{version}-%{release}
BuildRoot:  %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)

%description
This is the CloudStack CloudBridge

%prep

%setup -q -n %{name}-%{_ver}

%build

%define _localstatedir /var
%define _sharedstatedir /usr/share
./waf configure --prefix=%{_prefix} --libdir=%{_libdir} --bindir=%{_bindir} --javadir=%{_javadir} --sharedstatedir=%{_sharedstatedir} --localstatedir=%{_localstatedir} --sysconfdir=%{_sysconfdir} --mandir=%{_mandir} --docdir=%{_docdir}/%{name}-%{version} --fast --package-version=%{_ver}

%install
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}
ant deploy-rpm-install -Dversion=%{version}
mv ../cloud-bridge-%{_ver}-1 ${RPM_BUILD_ROOT}
mkdir $RPM_BUILD_ROOT/usr/share/cloud/bridge/logs
mkdir $RPM_BUILD_ROOT/usr/share/cloud/bridge/work
mkdir $RPM_BUILD_ROOT/usr/share/cloud/bridge/temp

%clean

#[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}


%preun 
/sbin/service cloud-bridge stop || true
if [ "$1" == "0" ] ; then
    /sbin/chkconfig --del cloud-bridge  > /dev/null 2>&1 || true
    /sbin/service cloud-bridge stop > /dev/null 2>&1 || true
fi

%pre 
id cloud > /dev/null 2>&1 || /usr/sbin/useradd -M -c "CloudStack CloudBridge unprivileged user" \
     -r -s /bin/sh -d %{_sharedstatedir}/cloud cloud|| true
# user harcoded here

%post 
if [ "$1" == "1" ] ; then
    /sbin/chkconfig --add cloud-bridge > /dev/null 2>&1 || true
    /sbin/chkconfig --level 345 cloud-bridge on > /dev/null 2>&1 || true
fi

%files 
%defattr(0644,cloud,cloud,0755)
/usr/share/cloud/bridge/conf/*
/usr/share/cloud/bridge/lib/*
/usr/share/cloud/bridge/webapps/*
%dir %attr(0775,cloud,cloud) /usr/share/cloud/bridge/logs
%dir %attr(0775,cloud,cloud) /usr/share/cloud/bridge/work
%dir %attr(0775,cloud,cloud) /usr/share/cloud/bridge/temp
%attr(0644,root,root) /usr/share/cloud/setup/bridge/db/*
%attr(0755,root,root) /etc/init.d/cloud-bridge
%attr(0755,root,root) /usr/bin/cloud-bridge-register
%attr(0755,root,root) /usr/bin/cloud-setup-bridge
%attr(0755,root,root) /usr/bin/cloud-setup-bridge-db
