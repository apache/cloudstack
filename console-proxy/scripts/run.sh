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
  fi
  sleep 20
done
