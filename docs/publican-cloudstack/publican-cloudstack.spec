%define brand cloudstack

Name:		publican-cloudstack
Summary:	Common documentation files for %{brand}
Version:	0.2
Release:	1%{?dist}
License:	ASLv2
Group:		Applications/Text
Buildroot:	%{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)
Buildarch:	noarch
Source:		%{name}-%{version}.tgz
Requires:	publican >= 1.99
BuildRequires:	publican >= 1.99
URL:		http://cloudstack.org

%description
This package provides common files and templates needed to build documentation
for %{brand} with publican.

%prep
%setup -qn %{name} 

%build
publican build --formats=xml --langs=en-US --publish

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p -m755 $RPM_BUILD_ROOT%{_datadir}/publican/Common_Content
publican install_brand --path=$RPM_BUILD_ROOT%{_datadir}/publican/Common_Content

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%doc README
%doc LICENSE
%doc NOTICE
%{_datadir}/publican/Common_Content/%{brand}

%changelog
* Tue Jun 26 2012  David Nalley <david@gnsa.us> 0.2-1
- updated for ASF move
* Sat Aug 11 2011  David Nalley <david@gnsa.us> 0.1-1
- Created Brand

