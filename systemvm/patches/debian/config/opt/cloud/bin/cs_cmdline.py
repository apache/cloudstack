from pprint import pprint

def merge(dbag, cmdline):
    dbag['config'] = cmdline
    return dbag
