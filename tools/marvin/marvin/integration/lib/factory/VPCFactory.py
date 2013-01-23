import factory
from marvin.integration.lib.newbase import VPC
class VPCFactory(factory.Factory):

    FACTORY_FOR = VPC

    cidr = None
    displaytext = None
    name = None
    vpcofferingid = None
    zoneid = None
