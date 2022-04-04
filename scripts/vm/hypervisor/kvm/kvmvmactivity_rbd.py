#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import os
import sys
import rbd
import rados

def createArgumentParser():
	parser=argparse.ArgumentParser(description='KVM RBD HA를 위한 인자')
	parser.add_argument('-i', help='Source Host ip')
	parser.add_argument('-p', help='rbd pool name')
	parser.add_argument('-n', help='pool auth username')
	parser.add_argument('-s', help='pool auth secret')
	parser.add_argument('-v', help='host')
	parser.add_argument('-u', help='volume uuid list')

	return parser

confFile='/etc/ceph/ceph.conf'
keyringFolder='/run/cloudstack/agent'
keyringFile='keyring.bin'

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

def watcher_list(i_name):
	# HB RBD Image List
	cluster = rados.Rados(conffile=confFile)
	try:
		cluster.connect()
		ioctx = cluster.open_ioctx(args.p)
		try:
			image = rbd.Image(ioctx, i_name)
			try:
				watchers = list(image.watchers_list())
				return len(watchers)
			finally:
				image.close()
		except rbd.ImageNotFound:
			return 0
		finally:
			ioctx.close()
	finally:
		cluster.shutdown()

def check_diskActivity():
	for UUID in args.u.split(','):
		ac_rs = watcher_list(UUID)
		if ac_rs == 1:
			return ac_rs
			break
	return 2


if __name__ == '__main__':
	# parser 생성
	parser = createArgumentParser()
	# 인자값 파싱
	args = parser.parse_args()

	create_cephKeyring()

	# First check: heartbeat watcher
	rs = watcher_list('hb-'+args.v)
	if rs == 2:
		print('=====> ALIVE <=====')
		sys.exit(0)

	# Second check: disk activity check
	ac_rs = check_diskActivity()
	if ac_rs == 2:
		print('=====> ALIVE <=====')
	else:
		print('=====> Considering host as DEAD due to [RBD '+args.p+' pool] Image Watcher does not exists <======')

	delete_cephKeyring()
