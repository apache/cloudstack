#!/bin/bash

sourcedir=~/incubator-cloudstack/
common_content_dir=/usr/share/publican/Common_Content
publican_path=/usr/bin/publican

usage(){
    echo "usage: $0 [-s source dir] [-c publican common content] [-p path to publican]"
    echo "  -s sets the source directory (defaults to $sourcedir)"
    echo "  -c sets the public common content directory (defaults to $common_content_dir)"
    echo "  -p sets the path to the publican binary (defaults to $publican_path)"
    echo "  -h"
}

while getopts v:s:c:p:h opt
do
    case "$opt" in
      v)  version="$OPTARG";;
      s)  sourcedir="$OPTARG";;
      c)  common_content_dir="$OPTARG";;
      p)  publican_path="$OPTARG";;
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

cd $sourcedir/docs
cp -R /usr/share/publican/Common_Content .
ln -s $sourcedir/docs/publican-cloudstack Common_Content/cloudstack
publican build --config=publican-all.cfg --formats html,pdf --langs en-US --common_content=$sourcedir/docs/Common_Content
rm -r Common_Content