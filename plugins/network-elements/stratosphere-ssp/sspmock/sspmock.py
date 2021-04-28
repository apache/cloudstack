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
from flask import Flask,request,make_response
from beaker.middleware import SessionMiddleware
app = Flask(__name__)

tenant_networks = []
tenant_ports = []

@app.route("/ws.v1/login", methods=["POST",])
def login():
	assert "username" in request.form
	assert "password" in request.form
        request.environ["beaker.session"]["login"] = True
	res = make_response("", 200)
	res.headers["Content-type"] = "application/json"
	return res

@app.route("/ssp.v1/tenant-networks", methods=["POST",])
def create_tenant_network():
        if "login" not in request.environ["beaker.session"]:
                return make_response("", 401)
	obj = request.json
	obj["uuid"] = str(uuid.uuid1())
	tenant_networks.append(obj)
	res = make_response(json.dumps(obj), 201)
	res.headers["Content-type"] = "application/json"
	return res

@app.route("/ssp.v1/tenant-networks/<tenant_net_uuid>", methods=["DELETE",])
def delete_tenant_network(tenant_net_uuid):
        if "login" not in request.environ["beaker.session"]:
                return make_response("", 401)
	for net in tenant_networks:
		if net["uuid"] == tenant_net_uuid:
			tenant_networks.remove(net)
			return make_response("", 204)
	return make_response("", 404)

@app.route("/ssp.v1/tenant-ports", methods=["POST",])
def create_tenant_port():
        if "login" not in request.environ["beaker.session"]:
                return make_response("", 401)
	obj = request.json
	obj["uuid"] = str(uuid.uuid1())
	tenant_ports.append(obj)
	res = make_response(json.dumps(obj), 201)
	res.headers["Content-type"] = "application/json"
	return res

@app.route("/ssp.v1/tenant-ports/<tenant_port_uuid>", methods=["DELETE",])
def delete_tenant_port(tenant_port_uuid):
        if "login" not in request.environ["beaker.session"]:
                return make_response("", 401)
	for port in tenant_ports:
		if port["uuid"] == tenant_port_uuid:
			tenant_ports.remove(port)
			return make_response("", 204)
	return make_response("", 404)

@app.route("/ssp.v1/tenant-ports/<tenant_port_uuid>", methods=["PUT",])
def update_tenant_port(tenant_port_uuid):
        if "login" not in request.environ["beaker.session"]:
                return make_response("", 401)
	for port in tenant_ports:
		if port["uuid"] == tenant_port_uuid:
			obj = request.json
			obj["uuid"] = tenant_port_uuid
			obj["vlan_id"] = 100
			tenant_ports.remove(port)
			tenant_ports.append(obj)
			res = make_response(json.dumps(obj), 200)
			res.headers["Content-type"] = "application/json"
			return res
	return make_response("", 404)

if __name__=="__main__":
    app.wsgi_app = SessionMiddleware(app.wsgi_app, {
       "session.auto":True,
       "session.type":"cookie",
       "session.validate_key":"hoge"})
    app.run(host="0.0.0.0", port=9080, debug=True)
