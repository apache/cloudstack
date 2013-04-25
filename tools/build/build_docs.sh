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

sourcedir=~/incubator-cloudstack/
common_content_dir=/usr/share/publican/Common_Content
publican_path=/usr/bin/publican
output_format="html,pdf"
config="publican-adminguide.cfg"

usage(){
    echo "usage: $0 [-s source dir] [-c publican common content] [-p path to publican]"
    echo "  -s sets the source directory (defaults to $sourcedir)"
    echo "  -c sets the public common content directory (defaults to $common_content_dir)"
    echo "  -p sets the path to the publican binary (defaults to $publican_path)"
    echo "  -f sets the output format (defaults to $output_format)"
    echo "  -g sets the publican config file (defaults to $config)"
    echo "  -h show this help"
}

while getopts v:s:c:p:f:g:h opt
do
    case "$opt" in
      v)  version="$OPTARG";;
      s)  sourcedir="$OPTARG";;
      c)  common_content_dir="$OPTARG";;
      p)  publican_path="$OPTARG";;
      f)  output_format="$OPTARG";;
      g)  config="$OPTARG";;
      h)  usage
          exit 0;;
      \?)
          usage
          exit 1;;
    esac
done

if [ ! -x "$publican_path" ]; then
    echo "$publican_path doesn't seem like an executeable?"
    exit 1
fi

if [ ! -d "$sourcedir/docs" ]; then
    echo "$sourcedir/docs doesn't seem to exist? Maybe set -s?"
    exit 1
fi

cd $sourcedir/docs
cp -R /usr/share/publican/Common_Content .
ln -s $sourcedir/docs/publican-cloudstack Common_Content/cloudstack
publican build --config=$config --formats $output_format --langs en-US --common_content=$sourcedir/docs/Common_Content
rm -r Common_Content
