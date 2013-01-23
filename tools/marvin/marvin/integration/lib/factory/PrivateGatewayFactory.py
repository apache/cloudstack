import factory
from marvin.integration.lib.newbase import PrivateGateway
class PrivateGatewayFactory(factory.Factory):

    FACTORY_FOR = PrivateGateway

    gateway = None
    ipaddress = None
    netmask = None
    vlan = None
    vpcid = None
