from pprint import pprint

def merge(dbag, rules):
    for rule in rules["rules"]:
        source_ip = rule["source_ip_address"]
        destination_ip = rule["destination_ip_address"]
        revoke = rule["revoke"]
        if not revoke:
            if rules["type"] == "staticnatrules":
                snatrule = dict()
                snatrule["type"] = "staticnat"
                snatrule["public_ip"] = source_ip
                snatrule["internal_ip"] = destination_ip
                dbag[source_ip] = ( snatrule )
            elif rules["type"] == "forwardrules":
                pfrule = dict()
                pfrule["type"] = "forward"
                pfrule["public_ip"] = source_ip
                pfrule["public_ports"] = rule["source_port_range"]
                pfrule["internal_ip"] = destination_ip
                pfrule["interal_ports"] = rule["destination_port_range"]
                pfrule["prootocol"] = rule["protocol"]
                if source_ip in dbag.keys():
                    for forward in dbag[source_ip]:
                        print "find duplicate here"
                else:
                    dbag[source_ip] = ( pfrule )
        elif revoke:
            if rules["type"] == "staticnatrules":
                if source_ip in dbag.keys():
                    del dbag[source_ip]
    
    return dbag
