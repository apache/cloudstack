from pprint import pprint
from netaddr import *


def merge(dbag, data):

    # A duplicate ip address wil clobber the old value
    # This seems desirable ....
    if "add" in data and data['add'] is False and \
            "ipv4_adress" in data:
        if data['ipv4_adress'] in dbag:
            del(dbag[data['ipv4_adress']])
        return dbag
    else:
        dbag[data['ipv4_adress']] = data
    return dbag
