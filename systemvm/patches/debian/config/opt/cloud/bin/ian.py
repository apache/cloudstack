from cs.CsGuestNetwork import CsGuestNetwork
import merge

merge.DataBag.DPATH = "."
csguestnetwork = CsGuestNetwork({}, {})
csguestnetwork.guest = True
csguestnetwork.set_dns("1.1.1.1,2.2.2.2")
csguestnetwork.set_router("3.3.3.3")
dns = csguestnetwork.get_dns()
print dns
