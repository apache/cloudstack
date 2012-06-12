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
do
	start_vm="GET  http://127.0.0.1:8096/client/?command=startVirtualMachine&id=$x	HTTP/1.0\n\n"
	echo -e $start_vm | nc -v -q 60 127.0.0.1 8096
done

sleep 60s

for x in `seq 3 1102`
do
	stop_vm="GET  http://127.0.0.1/client/?command=stopVirtualMachine&id=$x	HTTP/1.0\n\n"
	echo -e $stop_vm | nc -v -q 60 127.0.0.1 8096
done

sleep 60s
