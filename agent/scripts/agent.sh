#!/bin/bash
#run.sh runs the agent client.

# set -x
 
while true
do
  ./run.sh "$@"
  ex=$?
  if [ $ex -eq 0 ] || [ $ex -eq 1 ] || [ $ex -eq 66 ] || [ $ex -gt 128 ]; then
    exit $ex
  fi
done
