#!/usr/bin/python

import sys
import base64

def vm_data(args):

    router_ip = args.pop('routerIP')
    vm_ip = args.pop('vmIP')

    util.SMlog("    adding vmdata for VM with IP: " + vm_ip + " to router with IP: " + router_ip)

    for pair in args:
        pairList = pair.split(',')
        vmDataFolder = pairList[0]
        vmDataFile = pairList[1]
        vmDataValue = args[pair]
        cmd = ["/bin/bash", "/root/userdata.sh", "-v", vm_ip, "-F", vmDataFolder, "-f", vmDataFile]
        
        fd = None
        tmp_path = None
        if (vmDataValue != "none"):
            try:
                fd,tmp_path = tempfile.mkstemp()
                tmpfile = open(tmp_path, 'w')

                if (vmDataFolder == "userdata"):
                    vmDataValue = base64.urlsafe_b64decode(vmDataValue)
                    
                tmpfile.write(vmDataValue)
                tmpfile.close()
                cmd.append("-d")
                cmd.append(tmp_path)
            except:
                util.SMlog("  vmdata failed to write tempfile "  )
                os.close(fd)
                os.remove(tmp_path)
                return ''

        try:
            txt = util.pread2(cmd)
            txt = 'success'
        except:
            util.SMlog("    vmdata failed with folder: " + vmDataFolder + " and file: " + vmDataFile)
            txt = ''

        if (fd != None):
            os.close(fd)
            os.remove(tmp_path)

    return txt

def parseFileData(fileName):
	args = []
	fd = open(fileName)
	
	line = fd.readline()
	while (line != ""):
		args.append(line)
		line = fd.readline()
	
	return args
	
vmdata(parseFileData("/tmp/" + sys.argv[1]))
