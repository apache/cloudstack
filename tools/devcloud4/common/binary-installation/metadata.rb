#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#

name 'binary-installation'
maintainer 'Ian Duffy'
maintainer_email 'ian@ianduffy.ie'
license 'Apache 2'
description 'Wrapper around several different cookbooks.'
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version '0.1.0'

depends 'mysql', '= 5.6.1'
depends 'cloudstack', '>= 3.0.0'
depends 'nfs', '>= 2.0.0'

supports 'centos'
supports 'redhat'
supports 'debian'
supports 'ubuntu'
supports 'fedora'
supports 'oracle'

provides 'binary-installation::default'
provides 'binary-installation::management_server'
provides 'binary-installation::database_server'
provides 'binary-installation::nfs_server'
