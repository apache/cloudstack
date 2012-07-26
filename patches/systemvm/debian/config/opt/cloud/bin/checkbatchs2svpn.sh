#!/bin/bash

for i in $*
do
    info=`/opt/cloud/bin/checks2svpn.sh $i`
    ret=$?
    echo -n "$i:$ret:$info&"
done

