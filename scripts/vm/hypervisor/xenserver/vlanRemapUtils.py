#!/usr/bin/python

# Create flows on Open vSwitch
#
# @param: bridge name, refer to a network created by xenserver
# @param: our MAC
# @param: vlan ID

import os
import sys
from os.path import exists as _exists
from time import localtime as _localtime, asctime as _asctime

vSwitchDBPidFile = "/var/run/openvswitch/ovsdb-server.pid"
vSwitchDBDaemonName = "ovsdb-server"
vSwitchPidFile = "/var/run/openvswitch/ovs-vswitchd.pid"
vsctlPath = "/usr/bin/ovs-vsctl"
vSwitchDaemonName = "ovs-vswitchd"

logFile = "/var/log/cloud/management/vlanRemapUtils.log"
fLog = None

global result

errors = \
		{"NO_DB_PID_FILE" : "NO_DB_PID_FILE", \
		 "DB_NOT_RUN" : "DB_NOT_RUN", \
		 "NO_SWITCH_PID_FILE" : "NO_SWITCH_PID_FILE", \
		 "SWITCH_NOT_RUN" : "SWITCH_NOT_RUN", \
		 "NO_VSCTL" : "NO_VSCTL", \
		 "COMMAND_FAILED" : "COMMAND_FAILED", \

		 "ERR_ARGS_NUM" : "ERR_ARGS_NUM", \
		 "ERROR_OP" : "ERROR_OP", \
		 "SUCCESS" : "SUCCESS", \
		}

def openLog ():
	global fLog

	try:
		if fLog == None:
			fLog = open (logFile, "a")
	except IOError, e:
		#print e
		pass

def log (str):
	global fLog

	if fLog != None:
		str = "[%s]:" % _asctime (_localtime()) + str + "\n"
		fLog.write (str)

def closeLog ():
	global fLog

	if fLog != None:
		fLog.close ()

def isProcessRun (pidFile, name):
	try:
		fpid = open (pidFile, "r")
		pid = fpid.readline ()
		fpid.close ()
	except IOError, e:
		return -1

	pid = pid[:-1]
	ps = os.popen ("ps -ae")
	for l in ps:
		if pid in l and name in l:
			ps.close ()
			return 0

	ps.close ()
	return -2

def isToolExist (name):
	if _exists (name):
		return 0
	return -1


def checkvSwitch ():
	global result

	ret = isProcessRun (vSwitchDBPidFile, vSwitchDBDaemonName);
	if ret < 0:
		if ret == -1: result = errors["NO_DB_PID_FILE"]
		if ret == -2: result = errors["DB_NOT_RUN"]
		return -1

	ret = isProcessRun (vSwitchPidFile, vSwitchDaemonName)
	if ret < 0:
		if ret == -1: result = errors["NO_SWITCH_PID_FILE"]
		if ret == -2: result = errors["SWITCH_NOT_RUN"]
		return -1

	if isToolExist (vsctlPath) < 0:
		result = errors["NO_VSCTL"]
		return -1

	return 0

def doCmd (cmds, lines=False):
	cmd = ""
	for i in cmds:
		cmd += " "
		cmd += i

	log ("do command '%s'" % cmd)
	f = os.popen (cmd)
	if lines == True:
		res = f.readlines ()
	else:
		res = f.readline ()
		res = res[:-1]
	f.close ()

	if lines == False:
		log ("command output '%s'" % res)
	return res

######################## GRE creation utils ##########################
# UUID's format is 8-4-4-4-12
def isUUID (uuid):
	list = uuid.split ("-")

	if len (list) != 5:
		return -1

	if len (list[0]) != 8 or len (list[1]) != 4 \
       or len (list[2]) != 4 or len (list[3]) != 4 \
	   or len (list[4]) != 12:
		   return -1

	return 0

#FIXME: better check method
def checkGREInterface (bridge, remoteIP, greKey):
	listIfaces = [vsctlPath, "list interface"]
	res = doCmd (listIfaces, True)

	start = False
	num = 0
	keyStr = "key=%s" % greKey
	uuid = ''
	for i in res:
		if "_uuid" in i:
			(x, uuid) = i.split(":")
			uuid = strip(uuid)

		if "options" in i and remoteIP in i and keyStr in i:
			log("WARNING: GRE tunnel for remote_ip=%s key=%s already here" % \
					(remoteIP, greKey))
			return -1

	return 0


def createGRE (bridge, remoteIP, greKey):
	global result

	name = "%sgre" % bridge
	if checkGREInterface(bridge, remoteIP, greKey) < 0:
		return 0

	createInterface = [vsctlPath, "create interface", "name=%s" % name, \
			'type=gre options:"remote_ip=%s key=%s"' % (remoteIP, greKey)]
	ifaceUUID = doCmd (createInterface)
	if isUUID (ifaceUUID) < 0:
		result = errors["COMMAND_FAILED"];
		return -1

	createPort = [vsctlPath, "create port", "name=%s" % name, \
			"interfaces=[%s]" % ifaceUUID]
	portUUID = doCmd (createPort)
	if isUUID (portUUID) < 0:
		result = errors["COMMAND_FAILED"];
		return -1

	addBridge = [vsctlPath, "add bridge %s" % bridge, "port %s" % portUUID]
	doCmd (addBridge)
	return 0
######################## End GRE creation utils ##########################

######################## Flow creation utils ##########################
def getPortsOnBridge(bridge):
	listBr = [vsctlPath, "list br", bridge]
	res = doCmd(listBr, True)

	for i in res:
		if "ports" in i:
			(x, str) =  i.split(":")
			str = str.lstrip().rstrip()
			str = str[1:]
			str = str[:-1]
			return str.split(",")
	return None

def getFieldOfPort(nameOruuid, field):
	listport = [vsctlPath, "list port", nameOruuid]
	res = doCmd(listport, True)

	for i in res:
		if field in i:
			(x, r) = i.split(":")
			return r.lstrip().rstrip()
	return None

def getFieldOfInterface(nameOruuid, field):
	listIface = [vsctlPath, "list interface", nameOruuid]
	res = doCmd(listIface, True)

	for i in res:
		if field in i:
			(x, r) = i.split(":")
			return r.lstrip().rstrip()
	return None

def strip(str, direction="default"):
	str = str.lstrip().rstrip()
	if direction == "left":
		return str[1:]
	if direction == "right":
		return str[:-1]
	if direction == "both":
		str = str[1:]
		str = str[:-1]
		return str
	return str

def getVifPort(bridge, vifName):
	portUuids = getPortsOnBridge(bridge)
	if portUuids == None:
		log("No ports on bridge %s" % bridge)
		return -1

	for i in portUuids:
		name = getFieldOfPort(i, "name")
		if name == None:
			log("WARNING: no name found for %s" % name)
			continue

		name = strip(name, "both")
		if name == vifName:
			return getFieldOfInterface(vifName, "ofport")
	return None

def getInterfacesOnPort(nameOruuid):
	listPort = [vsctlPath, "list port", nameOruuid]
	res = doCmd(listPort, True)

	for i in res:
		if "interfaces" in i:
			(x, str) = i.split(":")
			str = strip(str, "both")
			return str.split(",")
	return None

def getOfPortsByType(bridge, askGre):
	portUuids = getPortsOnBridge(bridge)
	if portUuids == None:
		log("WARNING:No ports on bridge %s" % bridge)
		return -1

	OfPorts = []
	for i in portUuids:
		ifaces = getInterfacesOnPort(i)
		if ifaces == None:
			log("WARNING:No interfaces on port %s" % i)
			continue

		for j in ifaces:
			if j == '[]':
				log("WARNING:invalid interface [] on port %s" % i)
				continue

			type = getFieldOfInterface(j, "type")
			type = strip(type)
			if askGre == True and type == "gre":
				ofport = getFieldOfInterface(j, "ofport")
				if ofport != '[]':OfPorts.append(strip(ofport))
			elif askGre == False and type != "gre":
				ofport = getFieldOfInterface(j, "ofport")
				if ofport != '[]' and ofport != "65534":OfPorts.append(strip(ofport))
	return OfPorts

def getNoneGreOfPort(bridge):
	return getOfPortsByType(bridge, False)

def getGreOfPorts(bridge):
	return getOfPortsByType(bridge, True)

def findInPort():
	listIface = [vsctlPath, "list interface"]
	res = doCmd (listIface, True)

	inport = []
	port = ""
	for i in res:
		if "ofport" in i:
			(x, port) = i.split(":")
			port = port.lstrip().rstrip()

		if "type" in i:
			(x, type) = i.split(":")
			type = type.lstrip().rstrip()
			if type == "gre":
				inport.append (port)
	return inport


def formatFlow(inPort, vlan, mac, outPut):
	flow = "in_port=%s dl_vlan=%s dl_dst=%s idle_timeout=0 hard_timeout=0 \
			actions=strip_vlan,output:%s" % (inPort, vlan, mac, outPut)
	return flow

def delFlow(mac):
	param = "dl_dst=%s" % mac
	delFlow = ["ovs-ofctl del-flows %s" % bridge, '"%s"' % param]
	doCmd(delFlow)

def delARPFlow(vlan):
	param = "dl_type=0x0806 dl_vlan=%s" % vlan
	delFlow = ["ovs-ofctl del-flows %s" % bridge, '"%s"' % param]
	doCmd(delFlow)

def formatARPFlow(bridge, inPort, vlan, ports):
	outputs = ''
	for i in ports:
		str = "output:%s," % i
		outputs += str

	outputs = outputs[:-1]
	flow = "in_port=%s dl_vlan=%s dl_type=0x0806 idle_timeout=0 hard_timeout=0 \
			actions=strip_vlan,%s" % (inPort, vlan, outputs)
	return flow

def createFlow (bridge, vifName, mac, remap):
	inport = getGreOfPorts(bridge)
	if len(inport) == 0:
		log("WARNING: no inport found")
		return -1

	output = getVifPort(bridge, vifName)
	if output == None:
		log("WARNING: cannot find ofport for %s" % vifName)
		return -1
	if output == '[]':
		log("WARNING: ofport is [] for %s" % vifName)
		return -1

	#del old flow here, if any, but in normal there should be no old flow
	#maybe we need add check here
	delFlow(mac)

	#set remap here, remap has format e.g. [1,22,200,13,16]
	remap = strip(remap)
	log("")
	log("Create flow for remap")
	noneGreOfPorts = getNoneGreOfPort(bridge)
	isARP = True
	if len(noneGreOfPorts) == 0:
		log("WARNING: no none GRE ofports found, no ARP flow will be created")
		isARP = False

	for j in remap.split("/"):
		delARPFlow(j)
		for i in inport:
			flow = formatFlow(i, j, mac, output)
			param = bridge + ' "%s"' % flow
			addflow = ["ovs-ofctl add-flow", param]
			doCmd (addflow)

			if isARP == True:
				flow = formatARPFlow(bridge, i, j, noneGreOfPorts)
				param = bridge + ' "%s"' % flow
				addflow = ["ovs-ofctl add-flow", param]
				doCmd (addflow)

	return 0
######################## End Flow creation utils ##########################

def setTag(bridge, vifName, vlan):
	# The startVM command is slow, we may wait for a while for it creates vif on
	# open vswitch
	log("Waiting for %s ..." % vifName)
	waitPortCmd = [vsctlPath, "--timeout=10 wait-until port %s -- get port %s name" % \
			(vifName, vifName)]
	doCmd (waitPortCmd)
	log("%s is here" % vifName)

	if getVifPort(bridge, vifName) == None:
		log("WARNING: %s is not on bridge %s" % (vifName, bridge))
		return 0

	log("Set tag")
	setTagCmd = [vsctlPath, "set port", vifName, "tag=%s"%vlan]
	doCmd (setTagCmd)
	return 0

def doCreateGRE(bridge, remoteIP, key):
	if createGRE(bridge, remoteIP, key) < 0:
		log("WARNING: create GRE tunnel on %s for %s failed" % (bridge, \
			remoteIP))
	else:
		log("WARNING: create GRE tunnel on %s for %s success" % (bridge, \
			remoteIP))

def doCreateFlow (bridge, vifName, mac, remap):
	if createFlow(bridge, vifName, mac, remap) < 0:
		log ("Create flow failed(bridge=%s, vifName=%s, mac=%s,\
remap=%s" % (bridge, vifName, mac, remap))
	else:
		log ("Create flow success(bridge=%s, vifName=%s, mac=%s,\
remap=%s" % (bridge, vifName, mac, remap))

def doSetTag (bridge, vifName, tag):
	setTag(bridge, vifName, tag)

def doDeleteFlow(bridge, vifName, mac, remap):
	delFlow(mac)
	log("Delete flows for %s" % mac)

	remap = strip(remap)

	# remove our port from arp flow
	inport = getGreOfPorts(bridge)
	if len(inport) == 0:
		return

	mine = getVifPort(bridge, vifName)
	noneGreOfPorts = getNoneGreOfPort(bridge)
	noneGreOfPorts.remove(mine)
	log("Delete ARP flows for(vifname=%s, ofport=%s)" % (vifName, mine))

	for j in remap.split("/"):
		delARPFlow(j)
		for i in inport:
			flow = formatARPFlow(bridge, i, j, noneGreOfPorts)
			param = bridge + ' "%s"' % flow
			addflow = ["ovs-ofctl add-flow", param]
			doCmd (addflow)

def checkArgNum(num):
	if len (sys.argv) < num:
		result = errors["ERR_ARGS_NUM"]
		print result
		log ("Error number of arguments")
		sys.exit (-1)

if __name__ == "__main__":
	global result

	openLog ()


	if checkvSwitch () < 0:
		print result
		log ("Check switch failed, reason = '%s'" % result)
		sys.exit (-1)

	op = sys.argv[1]
	if op == "createGRE":
		checkArgNum(5)
		bridge = sys.argv[2]
		remoteIP = sys.argv[3]
		key = sys.argv[4]
		doCreateGRE(bridge, remoteIP, key)
	elif op == "createFlow":
		checkArgNum(6)
		bridge = sys.argv[2]
		vifName = sys.argv[3]
		mac = sys.argv[4]
		remap = sys.argv[5]
		doCreateFlow(bridge, vifName, mac, remap)
	elif op == "deleteFlow":
		checkArgNum(6)
		bridge = sys.argv[2]
		vifName = sys.argv[3]
		mac = sys.argv[4]
		remap = sys.argv[5]
		doDeleteFlow(bridge, vifName, mac, remap)
	elif op == "setTag":
		checkArgNum(5)
		bridge = sys.argv[2]
		vifName = sys.argv[3]
		tag = sys.argv[4]
		doSetTag(bridge, vifName, tag)
	else:
		log("WARNING: get an unkown op %s" % op)
		result=errors["ERROR_OP"]
		print result
		sys.exit(-1)

	result = errors["SUCCESS"]
	closeLog ()
	print result
