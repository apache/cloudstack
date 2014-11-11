from pprint import pprint
from netaddr import *


def merge(dbag, data):
    dbag[data['device']] = data
    return dbag
