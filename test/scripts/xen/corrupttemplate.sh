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



 


host=""
account=""
templateid=""
storage=""
var=""

while getopts h:s:a:i: OPTION
do
  case $OPTION in
  a)    account="$OPTARG"
        ;;
  i)    templateid="$OPTARG"
                ;;
  h)    host="$OPTARG"
                ;;
  s)    storage="$OPTARG"
  esac
done

var=`ssh root@$host "cd $storage &&  mv template/tmpl/$account/$templateid/template.properties template/tmpl/$account/$templateid/template.properties1 && touch template/tmpl/$account/$templateid/template.properties"`


if  [ "$var" != "" ]
then echo "Was unable to corrupt template $path on host $host"
exit 2
else
exit 0
fi