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

import json
import uuid
from flask import Flask
app = Flask(__name__)

tenant_networks = []
tenant_ports = []

@app.route("/ws.v1/login", methods=["POST",])
def login():
	response.content_type = "application/json"
	return ""

@app.route("/ssp.v1/tenant-networks", methods=["POST",])
def create_tenant_network():
	response.content_type = "application/json"
	response.status = 201
	obj = request.json
	obj["uuid"] = str(uuid.uuid1())
	tenant_networks.append(obj)
	return json.dumps(obj)

@app.route("/ssp.v1/tenant-networks/<tenant_net_uuid>", methods=["DELETE",])
def delete_tenant_network(tenant_net_uuid):
	for net in tenant_networks:
		if net["uuid"] == tenant_net_uuid:
			tenant_networks.remove(net)
			response.status = 204
			return ""
	response.status = 404
	return ""

@app.route("/ssp.v1/tenant-ports", methods=["POST",])
def create_tenant_port():
	response.content_type = "application/json"
	response.status = 201
	obj = request.json
	obj["uuid"] = str(uuid.uuid1())
	tenant_ports.append(obj)
	return json.dumps(obj)

@app.route("/ssp.v1/tenant-ports/<tenant_port_uuid>", methods=["DELETE",])
def delete_tenant_port(tenant_port_uuid):
	for port in tenant_ports:
		if port["uuid"] == tenant_port_uuid:
			tenant_ports.remove(port)
			response.status = 204
			return ""
	response.status = 404
	return ""

@app.route("/ssp.v1/tenant-ports/<tenant_port_uuid>", methods=["PUT",])
def update_tenant_port(tenant_port_uuid):
	response.content_type = "application/json"
	for port in tenant_ports:
		if port["uuid"] == tenant_port_uuid:
			obj = request.json
			obj["uuid"] = tenant_port_uuid
			obj["vlan_id"] = 100
			tenant_ports.remove(port)
			tenant_ports.append(obj)
			response.status = 200
			return json.dumps(obj)
	response.status = 404
	return ""

if __name__=="__main__":
    app.run(host="0.0.0.0", port=9080, debug=True)
