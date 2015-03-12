#!/usr/bin/env bash
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
 

# cloud-build-api-doc.sh -- builds api documentation.
#set -x
#set -u
TARGETJARDIR="$1"
shift
DEPSDIR="$1"
shift
DISTDIR="$1"
shift

canonical_readlink ()
{
    cd `dirname $1`;
    __filename=`basename $1`;
    if [ -h "$__filename" ]; then
        canonical_readlink `readlink $__filename`;
    else
        echo "`pwd -P`";
    fi
}

thisdir=$(canonical_readlink $0)

PATHSEP=':'
if [[ $OSTYPE == "cygwin" ]] ; then
  PATHSEP=';'
fi

CP=$PATHSEP/

java -cp $CP$PATHSEP$TARGETJARDIR/*$PATHSEP$DEPSDIR/* com.cloud.api.doc.ApiXmlDocWriter -d "$DISTDIR" $*

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

 PLATFORM=`uname -s`
 if [[ "$PLATFORM" =~ .*WIN.* ]]
 then
     gen_toc_file="`cygpath -w $thisdir`\\gen_toc.py"
     for file in `find . -type f`; do
         echo "Parse file $file";
         python $gen_toc_file $file;
     done
 else
     python "$thisdir/gen_toc.py" $(find . -type f)
 fi

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
