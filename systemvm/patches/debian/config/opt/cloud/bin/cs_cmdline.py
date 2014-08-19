from pprint import pprint

def merge(dbag, cmdline):
    if 'redundant' in cmdline['cmd_line']:
        cmdline['cmd_line']['redundant'] = "true"
    else:
        cmdline['cmd_line']['redundant'] = "false"
    dbag['config'] = cmdline['cmd_line']
    return dbag
