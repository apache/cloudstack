import paramiko 
import cloudstackException
class remoteSSHClient(object):
    def __init__(self, host, port, user, passwd, timeout=120):
        self.host = host
        self.port = port
        self.user = user
        self.passwd = passwd
        self.ssh = paramiko.SSHClient()
        self.ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        try:
            self.ssh.connect(str(host),int(port), user, passwd, timeout=timeout)
        except paramiko.SSHException, sshex:
            raise cloudstackException.InvalidParameterException(repr(sshex))
        
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
            
            
if __name__ == "__main__":
    ssh = remoteSSHClient("192.168.137.2", 22, "root", "password")
    print ssh.execute("ls -l")
    print ssh.execute("rm x")
