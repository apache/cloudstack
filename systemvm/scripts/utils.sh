#!/bin/bash

CLOUDSTACK_HOME="/usr/local/cloud"

get_pids() {
  local i
  for i in $(ps -ef| grep java | grep -v grep | awk '{print $2}');
  do
    echo $(pwdx $i) | grep "$CLOUDSTACK_HOME"  | awk -F: '{print $1}';
  done
}

lock()
{
  lockfile=$1
  lockfd=$2
  eval "exec $lockfd>$lockfile"
  flock -n $lockfd\
        && return 0 \
        || return 1
}
