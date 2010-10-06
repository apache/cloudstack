#!/bin/bash

sessionkey=$($(dirname $0)/createlogin.sh)
$(dirname $0)/setupzone.sh $sessionkey
$(dirname $0)/addhosts.sh $sessionkey
