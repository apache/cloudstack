from pprint import pprint

def merge(dbag, gn):
    added = False
    for dev in dbag:
        if dev == "id":
           continue
        if dbag[dev][0]['device'] == gn['device']:
           dbag[dev].remove(dbag[dev][0])
    if gn['add']:
       dbag.setdefault(gn['device'], []).append( gn )
    return dbag
