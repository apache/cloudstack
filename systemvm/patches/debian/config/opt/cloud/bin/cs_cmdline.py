from pprint import pprint

def merge(dbag, cmdline):
    dbag.setdefault('config', []).append( cmdline )
    return dbag
