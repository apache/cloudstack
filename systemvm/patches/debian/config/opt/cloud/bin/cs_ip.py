from pprint import pprint

def merge(dbag, ip):
    added = False
    for mac in dbag:
        if mac == "id":
           continue
        for address in dbag[mac]:
            if address['public_ip'] == ip['public_ip']:
               dbag[mac].remove(address)
    if ip['add']:
       dbag.setdefault('eth' + str(ip['nic_dev_id']), []).append( ip )
    return dbag
