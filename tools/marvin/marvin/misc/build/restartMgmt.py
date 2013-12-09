from ConfigParser import ConfigParser
from optparse import OptionParser
import marvin
from marvin import configGenerator
from marvin import sshClient
from time import sleep as delay
import telnetlib
import socket

if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option("-c", "--config", action="store", default="xen.cfg",
                      dest="config", help="the path where the server configurations is stored")
    (options, args) = parser.parse_args()
    
    if options.config is None:
        raise

    cscfg = configGenerator.getSetupConfig(options.config)
    mgmt_server = cscfg.mgtSvr[0].mgtSvrIp
    ssh = sshClient.SshClient(mgmt_server, 22, "root", "password")
    ssh.execute("service cloudstack-management restart")

    #Telnet wait until api port is open
    tn = None
    timeout = 120
    while timeout > 0:
        try:
            tn = telnetlib.Telnet(mgmt_server, 8096, timeout=120)
            break
        except Exception:
            delay(1)
            timeout = timeout - 1
    if tn is None:
        raise socket.error("Unable to reach API port")

