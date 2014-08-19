from pprint import pprint
from netaddr import *

def merge(dbag, data):

    # A duplicate ip address wil clobber the old value
    # This seems desirable ....
    dbag[data['ipv4_adress']] = data
    return dbag
