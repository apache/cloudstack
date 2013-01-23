import factory
from marvin.integration.lib.newbase import VpnCustomerGateway
class VpnCustomerGatewayFactory(factory.Factory):

    FACTORY_FOR = VpnCustomerGateway

    cidrlist = None
    esppolicy = None
    gateway = None
    ikepolicy = None
    ipsecpsk = None
