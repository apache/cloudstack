from pprint import pprint

def merge(dbag, gn):
    added = False
    for dev in dbag:
        if dev == "id":
           continue
        if dev == n['device']:
           dbag[dev].remove(dev)
    if gn['add']:
       dbag.setdefault(gn['device'], []).append( gn )
    return dbag
