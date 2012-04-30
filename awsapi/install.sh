#!/bin/bash

# install.sh -- deploys cloud-bridge and the corresponding DB

#set -x
set -e

#### deploying cloud-bridge
echo "Installing cloud-bridge..."
sh deploy-cloud-bridge.sh -d "$CATALINA_HOME"
echo "Deploying database..."
cd db && sh deploy-db-bridge.sh

#change port to 8090 in server.xml
#change ec2-service.properties file

exit 0
