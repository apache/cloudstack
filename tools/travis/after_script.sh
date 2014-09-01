#!/bin/bash
#
# This script should run any tear down commands required.
#

mvn -Dsimulator -pl client jetty:stop 2>&1
