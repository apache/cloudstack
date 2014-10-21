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

define devcloud::functions::httpdownload () {
  $file="${name['basedir']}/${name['basefile']}"

  exec {
    "getfileifnotexist${name}":
      command => "/usr/bin/wget ${name['url']}/${file}  -O ${name['local_dir']}/${file}",
      timeout => 0,
      unless  => "test -f ${name['local_dir']}/${file}",
      require => [ File["${name['local_dir']}/${name['base_dir']}/"],
                   Exec["get_md5sums"] ];


    "getfileifnotmatch${name}":
      command => "/usr/bin/wget ${name['url']}/${file} -O ${name['local_dir']}/${file}",
      timeout => 0,
      unless  => "/usr/local/bin/compare.sh ${file} ${name['working_dir']} ",
      require => [  Exec["getfileifnotexist${name}"], File["/usr/local/bin/compare.sh"] ]
    }

}
