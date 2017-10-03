
%define name            cloudian-cloudstack
%define release         1
%define version         4.9_6.2

%define summary         Integrates Cloudian into CloudStack

%define dataroot_dir    %{_datarootdir}/cloudstack-management
%define client_dir      %{dataroot_dir}/webapps/client
%define bin_dir         %{_sbindir}
%define docs_dir        %{_defaultdocdir}/cloudian-cloudstack

Summary:        %{summary}
Vendor:         Cloudian Inc.
URL:            http://www.cloudian.com
License:        ASL 2.0
Name:           %{name}
Version:        %{version}
Release:        %{release}
Source:         %{name}.tgz
BuildArch:      noarch
Group:          System Environment/Libraries

Requires:       cloudstack-management >= 4.9
Requires:       jre >= 1.7
Requires:       sed
Requires:       gzip

%description
Connects the Cloudian Management Console to Cloudstack. When enabled,
CloudStack users are also provisioned in Cloudian and users can use
Cloudian embedded in the CloudStack UI using single-sign-on.

%prep
# Unpack the source tar file
%setup -q -c -n %{name}

%build
# Build backend server jar
mvn clean install -o
# Copy UI plugin
cp -r ../../../../../../ui/plugins/cloudian .
# Build docs
cd docs && make && cd ..

%install
%__rm -rf %{buildroot}

%define appbase               %{_builddir}/%{buildsubdir}
%define client                %{appbase}/client
%define buildroot_client      %{buildroot}/%{client_dir}
%define buildroot_plugins     %{buildroot_client}/plugins
%define buildroot_webinf      %{buildroot_client}/WEB-INF
%define buildroot_docs        %{buildroot}/%{docs_dir}

%__install -d %{buildroot_client}
%__install -d %{buildroot_plugins}/cloudian
%__install -d %{buildroot_webinf}/lib
%__install -d %{buildroot_docs}

# Install our server side jar
%__install -m 0644 %{appbase}/target/cloud-plugin-integrations-cloudian-connector-*.jar %{buildroot_webinf}/lib/

# Install our ui plugin
%__install -m 0644 %{appbase}/cloudian/*    %{buildroot_plugins}/cloudian/

# Install the docs
%__install -m 0644 %{appbase}/docs/*.html   %{buildroot_docs}
%__install -m 0644 %{appbase}/docs/*.txt    %{buildroot_docs}

%files
# List of files in the RPM
%defattr(-,root,root,-)
%{client_dir}/WEB-INF/lib/*
%{client_dir}/plugins/cloudian/*

%doc %{docs_dir}/*

%clean
# clean the buildroot
%__rm -rf %{buildroot}

%post
# RPM post-install script
if [ -f /usr/share/cloudstack-management/webapps/client/plugins/plugins.js ]; then
    if ! grep -q cloudian /usr/share/cloudstack-management/webapps/client/plugins/plugins.js; then
        rm -f /usr/share/cloudstack-management/webapps/client/plugins/plugins.js.gz
        sed -i  "/cloudStack.plugins/a 'cloudian'," /usr/share/cloudstack-management/webapps/client/plugins/plugins.js
        gzip -c /usr/share/cloudstack-management/webapps/client/plugins/plugins.js > /usr/share/cloudstack-management/webapps/client/plugins/plugins.js.gz
    fi
fi
exit 0

%postun
# RPM post-uninstall script
if [ -f /usr/share/cloudstack-management/webapps/client/plugins/plugins.js ]; then
    if grep -q cloudian /usr/share/cloudstack-management/webapps/client/plugins/plugins.js; then
        rm -f /usr/share/cloudstack-management/webapps/client/plugins/plugins.js.gz
        sed -i  "/'cloudian'/d" /usr/share/cloudstack-management/webapps/client/plugins/plugins.js
        gzip -c /usr/share/cloudstack-management/webapps/client/plugins/plugins.js > /usr/share/cloudstack-management/webapps/client/plugins/plugins.js.gz
    fi
fi
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
