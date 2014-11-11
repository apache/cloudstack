from pprint import pprint


def merge(dbag, gn):
    added = False
    for dev in dbag:
        if dev == "id":
            continue
        if len(dbag[dev]) == 0:
            continue
        if dbag[dev][0]['device'] == gn['device']:
            dbag[dev].remove(dbag[dev][0])
    if gn['add']:
        dbag.setdefault(gn['device'], []).append()
    return dbag
