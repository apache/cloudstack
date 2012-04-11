
class cloudstackAPIException(Exception):
    def __init__(self, cmd = "", result = ""):
        self.errorMsg = "Execute cmd: %s failed, due to: %s"%(cmd, result)
    def __str__(self):
        return self.errorMsg
    
class InvalidParameterException(Exception):
    def __init__(self, msg=''):
        self.errorMsg = msg
    def __str__(self):
        return self.errorMsg
    
class dbException(Exception):
    def __init__(self, msg=''):
        self.errorMsg = msg
    def __str__(self):
        return self.errorMsg
    
class internalError(Exception):
    def __init__(self, msg=''):
        self.errorMsg = msg
    def __str__(self):
        return self.errorMsg