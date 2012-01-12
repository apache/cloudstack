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



 

PATHSEP=':'
if [[ $OSTYPE == "cygwin" ]] ; then
  PATHSEP=';'
fi

CP=commons-httpclient-3.1.jar${PATHSEP}commons-logging-1.1.1.jar${PATHSEP}commons-codec-1.3.jar${PATHSEP}testclient.jar${PATHSEP}log4j-1.2.15.jar${PATHSEP}utils.jar${PATHSEP}./conf
java -cp $CP com.vmops.test.longrun.BuildGuestNetwork $*


