from requester import make_request
from precache import apicache
from config import *
import re

def get_error_code(error):
    return int(re.findall("\d{3}",error)[0]) #Find the error code by regular expression
    #    return int(error[11:14]) #Ugly

def get_command(verb, subject):
    commandlist = apicache.get(verb, None)
    if commandlist is not None:
        command = commandlist.get(subject, None)
        if command is not None:
            return command["name"]
    return None

def apicall(command, data ):
    response, error = make_request(command, data, None, host, port, apikey, secretkey, protocol, path)
    if error is not None:
        return error, get_error_code(error)
    return response
