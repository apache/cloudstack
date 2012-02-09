#!/usr/bin/python
import logging
from signal import alarm, signal, SIGALRM, SIGKILL
from subprocess import PIPE, Popen
import os
from optparse import OptionParser
import paramiko 
import MySQLdb
from urlparse import urlparse

class bash:
    def __init__(self, args, timeout=600):
        self.args = args
        logging.debug("execute:%s"%args)
        self.timeout = timeout
        self.process = None
        self.success = False
        self.run()

    def run(self):
        class Alarm(Exception):
            pass
        def alarm_handler(signum, frame):
            raise Alarm

        try:
            self.process = Popen(self.args, shell=True, stdout=PIPE, stderr=PIPE)
            if self.timeout != -1:
                signal(SIGALRM, alarm_handler)
                alarm(self.timeout)

            try:
                self.stdout, self.stderr = self.process.communicate()
                if self.timeout != -1:
                    alarm(0)
            except Alarm:
                os.kill(self.process.pid, SIGKILL)
                

            self.success = self.process.returncode == 0
        except:
            pass

        if not self.success: 
            logging.debug("Failed to execute:" + self.getErrMsg())

    def isSuccess(self):
        return self.success
    
    def getStdout(self):
        return self.stdout.strip("\n")
    
    def getLines(self):
        return self.stdout.split("\n")

    def getStderr(self):
        return self.stderr.strip("\n")
    
    def getErrMsg(self):
        if self.isSuccess():
            return ""
        
        if self.getStderr() is None or self.getStderr() == "":
            return self.getStdout()
        else:
            return self.getStderr()
        
def initLoging(logFile=None):
    try:
        if logFile is None:
            logging.basicConfig(level=logging.DEBUG) 
        else: 
            logging.basicConfig(filename=logFile, level=logging.DEBUG) 
    except:
        logging.basicConfig(level=logging.DEBUG) 
        

def upgradeVmware(dcId, systemisoPath, db):
    db.execute("select version from mshost")
    version=db.fetchone()[0]
    logging.debug("mgt server version number is %s"%version)
    db.execute("""select url from host where type = "SecondaryStorage" and data_center_id=%s and removed is null """, (dcId))
    secondarystorages=db.fetchall()
    for ss in secondarystorages:
        uri=ss[0]
        nfshost=uri.split("//")[1].split("/", 1)[0]
        nfspath="/" + uri.split("//")[1].split("/", 1)[1]
        logging.debug("secondary storage: host " + nfshost + ", path " + nfspath)
        tempPath = "/tmp/tempary" 
         
        bash("mkdir -p " + tempPath)
        bash("mount %s:%s %s"%(nfshost, nfspath, tempPath))
        destIsoPath = tempPath + "/systemvm/systemvm-" + version + ".iso"
        if os.path.exists(destIsoPath):
            bash("cp -f %s %s"%(systemisoPath, destIsoPath))
        else:
            logging.debug("Can't find the %s"%destIsoPath)
            files = os.listdir(tempPath)
            for f in files:
                if f.endswith("iso"):
                    bash("cp -f %s %s"%(systemisoPath, tempPath + "/" + f))

        bash("umount " + tempPath)
        bash("rm -rf " + tempPath)
   
class remoteSSHClient(object):
    def __init__(self, host, port, user, passwd):
        self.host = host
        self.port = port
        self.user = user
        self.passwd = passwd
        self.ssh = paramiko.SSHClient()
        self.ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        try:
            self.ssh.connect(str(host),int(port), user, passwd)
        except paramiko.SSHException, sshex:
            logging.debug(repr(sshex))
        
        
    def execute(self, command):
        stdin, stdout, stderr = self.ssh.exec_command(command)
        output = stdout.readlines()
        errors = stderr.readlines()
        results = []
        if output is not None and len(output) == 0:
            if errors is not None and len(errors) > 0:
                for error in errors:
                    results.append(error.rstrip())
            
        else:
            for strOut in output:
                results.append(strOut.rstrip())
    
        return results
            
    def scp(self, srcFile, destPath):
        sftp = self.ssh.open_sftp()
        sftp.put(srcFile, destPath)
        
def upgradeKVM(hostIp, systemisoPath):
    bash("scp %s root@%s://usr/lib64/cloud/agent/vms/systemvm.iso"%(systemisoPath,hostIp))

def upgradeXenserver(hostIp, hostId, db, systemisoPath):
    db.execute("""select value from host_details where host_id=%s and name="username" """, (hostId,))
    userName=db.fetchone()[0]
    db.execute("""select value from host_details where host_id=%s and name="password" """, (hostId,))
    password=db.fetchone()[0]
    logging.debug("scp " + systemisoPath + " " + userName +":" + password + "@" + hostIp + " "  + " /opt/xensource/packages/iso/systemvm.iso " )
    sshClient = remoteSSHClient(hostIp, "22", userName, password)
    sshClient.scp(systemisoPath, "/opt/xensource/packages/iso/systemvm.iso")
    
if __name__ == '__main__':
    initLoging()
    parser = OptionParser()
    parser.add_option("-m", "--host", dest="mgt", help="management server name or IP")
    parser.add_option("-d", "--db", dest="db", help="DB server name or IP")
    parser.add_option("-u", "--dbUsr", dest="dbUsr", help="DB User name")
    parser.add_option("-p", "--dbPasswd", dest="dbPasswd", help="DB Password")
    parser.add_option("-o", "--oldIsoPath", dest="oldIsoPath", help="Old system vm Iso Path")
    parser.add_option("-n", "--newIsoPath", dest="newIsoPath", help="New system vm Iso Path")
    parser.add_option("-c", action="store_true", default=False, dest="console", help="don't upgrade console proxy")
    (options, args) = parser.parse_args()
    
    if options.mgt is None or options.db is None or options.newIsoPath is None:
        logging.debug("mgt server or db server or new iso path can not be empty")
        os.sys.exit()
        
    if options.dbUsr is None:
        options.dbUsr = "cloud"
        
    if options.dbPasswd is None:
        options.dbPasswd = ""
    
    if options.oldIsoPath is None:
        options.oldIsoPath = "/usr/lib64/cloud/agent/vms/systemvm.iso"
    
    '''patch iso'''
    if options.console:
        bash("./upgrade_console_proxy.sh -c -o " + options.oldIsoPath + " -n " + options.newIsoPath)
    else:
        bash("./upgrade_console_proxy.sh -o " + options.oldIsoPath + " -n " + options.newIsoPath)
    
    try:
        db=MySQLdb.connect(options.db, options.dbUsr, options.dbPasswd, "cloud")
    except:
        logging.debug("Can't not connect ot db")
        os.sys.exit()

    c=db.cursor()
    c.execute("select id from data_center")
    result=c.fetchall()
    for dc in result:
        dcId = dc[0]
        c.execute('select hypervisor_type,private_ip_address,id from host where removed is null and type = "Routing" and status = "Up" and data_center_id = %s', (dcId,))
        hosts = c.fetchall()
        print hosts
        vmwareUpgraded = False
        for host in hosts:
            if host[0] == "VMware" and not vmwareUpgraded:
                upgradeVmware(dcId, options.oldIsoPath, c) 
                vmwareUpgraded = True
            elif host[0] == "KVM":
                upgradeKVM(host[1], options.oldIsoPath)
            elif host[0] == "XenServer":
                upgradeXenserver(host[1], host[2], c, options.oldIsoPath)
          
    #get all the console vm, restart
    c.execute('select id from vm_instance where type = "ConsoleProxy" and state= "Running"')
    cpvmId=c.fetchall()
    for cpvm in cpvmId:
        bash('curl "http://%s:8096/?command=rebootSystemVm&id=%s"'%(options.mgt,cpvm[0]))
   
