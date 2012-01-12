#!/usr/bin/env bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
 

# cloud-build-api-doc.sh -- builds api documentation.
#set -x
set -u
TARGETJARDIR="$1"
shift
DEPSDIR="$1"
shift

PATHSEP=':'
if [[ $OSTYPE == "cygwin" ]] ; then
  PATHSEP=';'
fi

CP=$PATHSEP/

for file in $TARGETJARDIR/*.jar
do
  CP=${CP}$PATHSEP$file
done

for file in $DEPSDIR/*.jar; do
  CP=${CP}$PATHSEP$file
done

java -cp $CP com.cloud.api.doc.ApiXmlDocWriter $*

if [ $? -ne 0 ]
then
	exit 1
fi
