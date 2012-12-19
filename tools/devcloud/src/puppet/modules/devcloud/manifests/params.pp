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
# specific language governing permission  s and limitations
# under the License.

# == Class: devcloud::params
#
# This class implements the module params pattern, but it's loaded using hiera
# as opposed to the 'default' usage of coding the parameter values in your
# manifest.
#
# == Usage
#
# Don't use this class directly; it's being used where it is needed
#
class devcloud::params {

      $cs_dir         = '/opt/cloudstack'
      $storage_dir    = '/opt/storage'
      $tomcat_version = '6.0.32'
      $tomcat_url     = "http://archive.apache.org/dist/tomcat/tomcat-6/v${tomcat_version}/bin/apache-tomcat-${tomcat_version}.zip"
      $tomcat_home    = "${cs_dir}/apache-tomcat-${tomcat_version}"
      $maven_version  = '3.0.4'
      $maven_url      = "http://apache.mirrors.pair.com/maven/maven-3/${maven_version}/binaries/apache-maven-${maven_version}-bin.tar.gz"
      $maven_home     = "${cs_dir}/apache-maven-${maven_version}"
      $devcloud_path  = 'http://download.cloud.com/templates/devcloud'
      $template_path  = "${devcloud_path}/defaulttemplates"
      $md5sum_remote  = "${template_path}/md5sum.txt"
      $md5sum_local   = "${storage_dir}/secondary/template/tmpl/1/md5sum.txt"
      $template_dir   = "${storage_dir}/secondary/template/tmpl/1"
      $gitrepo        = 'https://github.com/apache/incubator-cloudstack.git'
      $build_cloudstack = false


  $downloads =  [

    {
      'basefile'    => 'template.properties',
      'basedir'     => '1',
      'url'         => $template_path,
      'local_dir'   => $template_dir,
      'working_dir' => $template_dir
    },
    {
      'basefile'    => 'template.properties',
      'basedir'     => '5',
      'url'         => $template_path,
      'local_dir'   => $template_dir,
      'working_dir' => $template_dir
    },
    {
      'basefile'    => 'dc68eb4c-228c-4a78-84fa-b80ae178fbfd.vhd',
      'basedir'     => '1',
      'url'         => $template_path,
      'local_dir'   => $template_dir,
      'working_dir' => $template_dir
    },
    {
      'basefile'    => 'ce5b212e-215a-3461-94fb-814a635b2215.vhd',
      'basedir'     => '5',
      'url'         => $template_path,
      'local_dir'   => $template_dir,
      'working_dir' => $template_dir
    }
  ]
}
