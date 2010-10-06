#!/bin/bash

sessionkey=$(curl -c admincookie.txt -sS "http://localhost:8080/client/api?command=login&username=admin&password=5f4dcc3b5aa765d61d8327deb882cf99&response=json" | awk -F "," '{print $13}' | awk -F":" '{print $2}' | tr -d '[:space:]' | tr -d '"')

echo $sessionkey
