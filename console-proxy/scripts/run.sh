#!/bin/bash
#_run.sh runs the agent client.

# set -x
 
while true
do
  ./_run.sh "$@"
  ex=$?
  if [ $ex -eq 0 ] || [ $ex -eq 1 ] || [ $ex -eq 66 ] || [ $ex -gt 128 ]; then
      # permanent errors
      sleep 160
  elif [ $ex -eq 143 ]; then
      # service cloud stop causes exit with 143
      exit $ex
  fi
  sleep 20
done
