import sys
import traceback
class CloudRuntimeException(Exception):
    def __init__(self, errMsg):
        self.errMsg = errMsg

        value = sys.exc_info()[1]
        if value is not None:
            self.errMsg += ", due to:" + str(value)
      
        self.details = formatExceptionInfo()
    def __str__(self):
        return self.errMsg
    def getDetails(self):
        return self.details

class CloudInternalException(Exception):
    def __init__(self, errMsg):
        self.errMsg = errMsg
    def __str__(self):
        return self.errMsg

def formatExceptionInfo(maxTBlevel=5):
    cla, exc, trbk = sys.exc_info()
    excTb = traceback.format_tb(trbk, maxTBlevel)
    msg = str(exc) + "\n"
    for tb in excTb:
        msg += tb
    return msg
