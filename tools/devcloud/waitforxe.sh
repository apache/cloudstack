#! /bin/bash
date
interval=20
timeout=300
command="xe host-list"

count=0
maxcount=$(($timeout/$interval))


until  [ $count -gt $maxcount ]; do
    if $command > /dev/null 2>&1; then
        echo "\"$command\" executed successfully."
        date
        exit 0
    fi
    let count=count+1
    echo "Waiting for \"$command\" to run successfully."
    sleep $interval
done

echo "\"$command\" failed to complete."
date
