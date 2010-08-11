#!/bin/bash -x

d=`dirname "$0"`
cd "$d"
tar c --owner root --group root -vzf patch.tgz root etc

