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

import argparse
import os
import rbd
import rados
import time

def createArgumentParser():
	parser=argparse.ArgumentParser(description='Argument for KVM RBD HA')
	parser.add_argument('-i', type=str, help='Source Host ip')
	parser.add_argument('-p', type=str, help='rbd pool name')
	parser.add_argument('-n', type=str, help='pool auth username')
	parser.add_argument('-s', type=str, help='pool auth secret')
	parser.add_argument('-v', type=str, help='host')
	parser.add_argument('-r', type=str, help='create/read hb watcher')

	return parser

keyringFolder='/run/cloudstack/agent'
keyringFile='keyring'

def create_cephKeyring():
	# Create Ceph keyring for executing rbd commands
	if not os.path.exists(keyringFolder):
		os.makedirs(keyringFolder)

	if not os.path.isfile(keyringFolder+'/'+keyringFile):
		f=open(keyringFolder+'/'+keyringFile,'w')
		f.write('[client.'+args.n+']\n key='+args.s+'\n')
		f.close()

def delete_cephKeyring():
	# Delete Ceph keyring for executing rbd commands
	if os.path.isfile(keyringFolder+'/'+keyringFile):
		os.remove(keyringFolder+'/'+keyringFile)

def watcher_list():
	# HB RBD Image List
	cluster = rados.Rados(conf={'mon_host': args.i, 'keyring': keyringFolder+"/"+keyringFile})
	try:
		cluster.connect()
		ioctx = cluster.open_ioctx(args.p)
		try:
			image = rbd.Image(ioctx, 'hb-'+args.v)
			try:
				watchers = list(image.watchers_list())
				return len(watchers)
			finally:
				image.close()
		except rbd.ImageNotFound:
			# Create HB RBD Image
			create_rbdImage()
			return 1
		finally:
			ioctx.close()
	finally:
		cluster.shutdown()

def create_rbdImage():
	# Create HB RBD Image
	with rados.Rados(conf={'mon_host': args.i, 'keyring': keyringFolder+"/"+keyringFile}) as cluster:
		with cluster.open_ioctx(args.p) as ioctx:
			rbd_inst = rbd.RBD()
			size = 1 * 1024**3  # 1 GiB
			rbd_inst.create(ioctx, 'hb-'+args.v, size)
			with rbd.Image(ioctx, 'hb-'+args.v) as image:
				data = b'foo' * 200
				image.write(data, 0)

def create_hbWatcher():
	# Watch HB RBD Image
	os.popen('setsid sh -c \"exec rbd watch hb-'+args.v+' --pool '+args.p+' -m '+args.i+' -k '+keyringFolder+'/'+keyringFile+' <> /dev/tty20 >&0 2>&1\"')


if __name__ == '__main__':
	# create parser
	parser = createArgumentParser()
	# argument parsing
	args = parser.parse_args()

	create_cephKeyring()

	# Monitoring
	if(args.r is None):
		rs = watcher_list()
		if rs == 1:
			create_hbWatcher()
		# Wait for watcher creation time
		time.sleep(1)

	# Checker
	else:
		rs = watcher_list()
		if rs == 2:
			os.system('echo =====> ALIVE <=====')
		else:
			os.system('echo =====> Considering host as DEAD due to [hb-$HostIP] watcher that the host is seeing is not running. <======')

	delete_cephKeyring()
