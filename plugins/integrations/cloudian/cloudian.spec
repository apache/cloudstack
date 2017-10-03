%define name            cloudian-cloudstack
%define release         1
%define version         4.9

%define summary         Integrates Cloudian into CloudStack

%define dataroot_dir  %{_datarootdir}/cloudstack-management
%define client_dir    %{dataroot_dir}/webapps/client
%define bin_dir       %{_sbindir}
%define docs_dir      %{_defaultdocdir}/cloudian-cloudstack
%define rpm_state_dir %{_localstatedir}/lib/rpm-state/cloudian-cloudstack

Summary:        %{summary}
Vendor:         Cloudian Inc.
URL:            http://www.cloudian.com
License:        ASL 2.0
Name:           %{name}
Version:        %{version}
Release:        %{release}
Source:         %{name}-%{version}.tgz
BuildArch:      noarch
Group:          System Environment/Libraries

Requires:       cloudstack-management >= 4.9
Requires:       jre >= 1.7
Requires:       gzip

%description
Connects the Cloudian Management Console to Cloudstack. When enabled,
CloudStack users are also provisioned in Cloudian and users can use
Cloudian embedded in the CloudStack UI using single-sign-on.

%prep

# Unpack the source tar file
%setup -q -c -n %{name}

%build
mvn clean install -o
cp -r ../../../../../../ui/plugins/cloudian .

%install
%__rm -rf %{buildroot}

%define appbase               %{_builddir}/%{buildsubdir}
%define client                %{appbase}/client
%define buildroot_docs        %{buildroot}/%{docs_dir}
%define buildroot_rpm_state   %{buildroot}/%{rpm_state_dir}
%define buildroot_client      %{buildroot}/%{client_dir}
%define buildroot_webinf      %{buildroot_client}/WEB-INF
%define buildroot_plugins     %{buildroot_client}/plugins

%__install -d %{buildroot_docs}
%__install -d %{buildroot_rpm_state}
%__install -d %{buildroot_client}
%__install -d %{buildroot_webinf}/lib
%__install -d %{buildroot_plugins}/cloudian

# Install our server side jar files
%__install -m 0644 %{appbase}/target/cloud-plugin-integrations-cloudian-connector-*.jar %{buildroot_webinf}/lib/

# Install our ui plugin
%__install -m 0644 %{appbase}/cloudian/* %{buildroot_plugins}/cloudian/

# Install the script to enable/disable our connector
%__install -m 0644 %{appbase}/docs/*.html               %{buildroot_docs}
%__install -m 0644 %{appbase}/docs/*.txt                %{buildroot_docs}

%files
# List of files in the RPM
%defattr(-,root,root,-)
%{client_dir}/WEB-INF/lib/*
%{client_dir}/plugins/cloudian/*

# our enable/disable script
%{rpm_state_dir}

%doc %{docs_dir}/*

%clean
# clean the buildroot
%__rm -rf %{buildroot}

%pre
# RPM pre-install script
if [ $1 -ge 2 ] ; then
    # Upgrade is in progress.
    # Only want to trigger if only the other package was updated.
    touch %{rpm_state_dir}/notrigger
    /usr/sbin/cloudian-cloudstack.sh status 1>/dev/null
    if [ $? -eq 3 ] ; then
        # Existing connector is enabled. Disable it now using the
        # old patches and old script. Remember also to re-enable
        # it in %post which will have our new script and new patches.
        /usr/sbin/cloudian-cloudstack.sh disable norestart
        touch %{rpm_state_dir}/enable
    fi
fi
exit 0

%post
# RPM post-install script
if [ $1 -ge 2 ] ; then
    # Upgrading. Check if package needs to be re-enabled.
    if [ -e %{rpm_state_dir}/enable ] ; then
        /usr/sbin/cloudian-cloudstack.sh enable
        rm -f %{rpm_state_dir}/enable
    fi
    exit 0
fi
# Installation will require configuration.
cat 1>&2 << EOF
=====================================================================
HyperStore Connect for CloudPlatform has been successfully installed.

The next steps are:
    1. # cloudian-cloudstack.sh configure
    2. # cloudian-cloudstack.sh enable

Should you ever wish to disable the connector:
    3. # cloudian-cloudstack.sh disable
=====================================================================
EOF
exit 0

%triggerin -- cloudstack-management
# RPM trigger on (re)install of cloudstack-management package
# or an install of our own package in which case we avoid running
if [ -e %{rpm_state_dir}/notrigger ] ; then
    rm -f %{rpm_state_dir}/notrigger
    exit 0
fi
# Other than that, only trigger if enabled.
/usr/sbin/cloudian-cloudstack.sh status 1>/dev/null
if [ $? -eq 3 ] ; then
    # cloudstack-management server which owns the files that we patch has
    # been re-installed (or force erased and installed) while our connector
    # was enabled. Running disable and enable is dangerous the management
    # servers files may have changed and no longer be patchable. At this
    # point, it's best to clean up the .orig files of our patches and
    # disable the connector without reverting the patches.
    /usr/sbin/cloudian-cloudstack.sh forget
    cat 1>&2 << EOF
=====================================================================
The cloudian-cloudstack connector has detected that a package that it
relies on "cloudstack-management" server has been re-installed. The 
connector has been disabled.

You can re-enable the connector using:
    1. # cloudian-cloudstack.sh enable
=====================================================================
EOF
    fi
exit 0

%preun
# RPM pre-uninstall script
# For Upgrade, pre and post take care of things. In preun, we just
# have to disable the connector if it's enabled.
if [ $1 -eq 0 ] ; then
    # Actual Un-install (not upgrade)
    /usr/sbin/cloudian-cloudstack.sh disable
fi
exit 0

%postun
# RPM post-uninstall script
exit 0

%changelog
* Tue Oct 3 2017 Cloudian Inc <support@cloudian.com> - 4.9_6.2-1
- New re-implementation of Cloudian Connector plugin
* Tue Jul 19 2016 Cloudian Inc <support@cloudian.com> - 4.7_5.0-3
- Added SSL support for Cloudian Admin Server
* Fri Jul 15 2016 Cloudian Inc <support@cloudian.com> - 4.7_5.0-2
- Documentation updates for 4.7 RPM Release
* Wed Jul 6 2016 Cloudian Inc <support@cloudian.com> - 4.7_5.0-1
- Initial 4.7 RPM Release
* Mon Jun 15 2015 Cloudian Inc <support@cloudian.com> - 4.5_5.0-1
- Initial 4.5 RPM Release
