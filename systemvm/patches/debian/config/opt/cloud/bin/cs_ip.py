from pprint import pprint
#[{u'accountId': 2,
  #u'add': True,
  #u'broadcastUri': u'vlan://untagged',
  #u'firstIP': False,
  #u'networkRate': 200,
  #u'newNic': False,
  #u'nicDevId': 1,
  #u'oneToOneNat': False,
  #u'publicIp': u'10.0.2.102',
  #u'sourceNat': True,
  #u'trafficType': u'Public',
  #u'vifMacAddress': u'06:f6:5e:00:00:03',
  #u'vlanGateway': u'10.0.2.1',
  #u'vlanNetmask': u'255.255.255.0'}]

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
