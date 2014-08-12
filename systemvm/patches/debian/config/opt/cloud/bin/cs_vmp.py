from pprint import pprint
from netaddr import *

def merge(dbag, data):
    """
    Track vm passwords
    """
    dbag[data['ip_address']] = data['password']
    return dbag
