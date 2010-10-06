#!/bin/bash

MGMT_SERVER=localhost
PASSWORD=password
USERNAME=root
sessionkey=$1

zoneId=5
podId=5
ip=192.168.140.11
cluster=CV
err=$(curl -sS "http://${MGMT_SERVER}:8096/?command=addHost&zoneId=$zoneId&cluster=$clustername&podId=$podId&url=http://$ip&username=$USERNAME&password=$PASSWORD&sessionkey=$sessionkey&response=json")

