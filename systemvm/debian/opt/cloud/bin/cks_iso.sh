#!/bin/bash
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

BASE_DIR=/var/www/html
CKS_ISO_DIR=$BASE_DIR/cks-iso
if [ "$1" == "true" ]
then
  mkdir -p $CKS_ISO_DIR
  echo "Options +Indexes" > $BASE_DIR/.htaccess
  echo "Mounting CKS ISO into $CKS_ISO_DIR"
  mount /dev/cdrom $CKS_ISO_DIR
else
  echo "Unmounting CKS ISO from $CKS_ISO_DIR"
  umount $CKS_ISO_DIR
  echo "Options -Indexes" > $BASE_DIR/.htaccess
  rm -rf $CKS_ISO_DIR
fi
echo "Restarting apache2 service"
service apache2 restart
