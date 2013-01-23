import factory
from marvin.integration.lib.newbase import VpnGateway
class VpnGatewayFactory(factory.Factory):

    FACTORY_FOR = VpnGateway

    vpcid = None
