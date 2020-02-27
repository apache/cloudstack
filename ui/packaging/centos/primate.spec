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

Name:      cloudstack
Summary:   Modern Apache CloudStack UI - Primate
Release:   %{_rel}
Version:   %{_ver}
License:   Apache License, Version 2
Vendor:    Apache CloudStack <dev@cloudstack.apache.org>
Packager:  Apache CloudStack <dev@cloudstack.apache.org>
Group:     System Environment/Libraries
Source0:   primate-%{_ver}.tgz
BuildRoot: %{_tmppath}/%{name}-%{release}-build

%description
Modern Apache CloudStack UI - Primate

%package primate
Summary:   Modern Apache CloudStack UI - Primate
Requires:  cloudstack-management >= 4.13.0
Group:     System Environment/Libraries
%description primate
Primate - modern role-base progressive UI for Apache CloudStack

%prep
echo "Starting Primate build..."

%setup -q -n %{name}

%build

echo "Executing npm build..."
npm install
npm run build

%install
echo "Installing Primate"
ls -lahi
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}
mkdir -p ${RPM_BUILD_ROOT}%{_datadir}/cloudstack-management/webapp/primate
mkdir -p ${RPM_BUILD_ROOT}%{_sysconfdir}/cloudstack/primate
mkdir -p ${RPM_BUILD_ROOT}%{_bindir}/

ls
cp -vr dist/* ${RPM_BUILD_ROOT}%{_datadir}/cloudstack-management/webapp/primate/
# copy config to ${RPM_BUILD_ROOT}%{_sysconfdir}/cloudstack/primate

%clean
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}

%preun primate
echo "Running through the pre-uninstall cloudstack-primate"

%pre primate
echo "Running through pre-install cloudstack-primate"

%post primate
echo "Running through post-install cloudstack-primate"

%postun primate
echo "Running through the post-uninstall cloudstack-primate"

%files primate
%defattr(-,root,root,-)
%{_datadir}/cloudstack-management/webapp/primate/*
%changelog
* Thu Feb 27 2020 Rohit Yadav <rohit@apache.org> 0.1.0
- CloudStack Primate RPM
