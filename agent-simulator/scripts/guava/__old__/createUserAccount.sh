#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.


name=$1

account_query="GET	http://127.0.0.1/client/?command=createAccount&accounttype=0&email=simulator%40simulator.com&username=$name&firstname=first$name&lastname=last$name&password=5f4dcc3b5aa765d61d8327deb882cf99&account=$name&domainid=1	HTTP/1.1\n\n"

echo -e $account_query | nc -v -q 120 127.0.0.1 8096
