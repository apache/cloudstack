import factory
from marvin.integration.lib.newbase import VpnConnection
class VpnConnectionFactory(factory.Factory):

    FACTORY_FOR = VpnConnection

    s2scustomergatewayid = None
    s2svpngatewayid = None
