from pprint import pprint


def merge(dbag, metadata):
    dbag[metadata["vm_ip_address"]] = metadata["vm_metadata"]
    return dbag
