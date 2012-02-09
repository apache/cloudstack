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
DISTDIR="$1"
shift

thisdir=$(readlink -f $(dirname "$0"))


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

java -cp $CP com.cloud.api.doc.ApiXmlDocWriter -d "$DISTDIR" $*

if [ $? -ne 0 ]
then
	exit 1
fi

set -e
(cd "$DISTDIR/xmldoc"
 cp "$thisdir"/*.java .
 cp "$thisdir"/*.xsl .
 sed -e 's,%API_HEADER%,User API,g' "$thisdir/generatetoc_header.xsl" >generatetocforuser.xsl
 sed -e 's,%API_HEADER%,Root Admin API,g' "$thisdir/generatetoc_header.xsl" >generatetocforadmin.xsl
 sed -e 's,%API_HEADER%,Domain Admin API,g' "$thisdir/generatetoc_header.xsl" >generatetocfordomainadmin.xsl

 python "$thisdir/gen_toc.py" $(find -type f)

 cat generatetocforuser_include.xsl >>generatetocforuser.xsl
 cat generatetocforadmin_include.xsl >>generatetocforadmin.xsl
 cat generatetocfordomainadmin_include.xsl >>generatetocfordomainadmin.xsl

 cat "$thisdir/generatetoc_footer.xsl" >>generatetocforuser.xsl
 cat "$thisdir/generatetoc_footer.xsl" >>generatetocforadmin.xsl
 cat "$thisdir/generatetoc_footer.xsl" >>generatetocfordomainadmin.xsl

 mkdir -p html/user html/domain_admin html/root_admin
 cp -r "$thisdir/includes" html
 cp -r "$thisdir/images" html

 javac -cp . *.java
 java -cp . XmlToHtmlConverter
)
