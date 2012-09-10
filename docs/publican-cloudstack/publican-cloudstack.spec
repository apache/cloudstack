# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information#    
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

%define brand cloudstack

Name:		publican-cloudstack
Summary:	Common documentation files for Apache %{brand}
Version:	0.4
Release:	1%{?dist}
License:	ASLv2
Group:		Applications/Text
Buildroot:	%{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)
Buildarch:	noarch
Source:		%{name}.tgz
Requires:	publican >= 1.99
BuildRequires:	publican >= 1.99
URL:		http://cloudstack.org

%description
This package provides common files and templates needed to build documentation
for Apache %{brand} with publican.

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
* Sun Sep 22 2012 David Nalley <david@gnsa.us> 0.4-1
- added 'Apache' to the description
- moved the issue tracker url to the ASF jira instance

* Tue Aug 14 2012 Joe Brockmeier <jzb@zonker.net> 0.3-1
- Removed unneeded common files distributed with Publican
* Tue Jun 26 2012  David Nalley <david@gnsa.us> 0.2-1
- updated for ASF move
* Sat Aug 11 2011  David Nalley <david@gnsa.us> 0.1-1
- Created Brand
