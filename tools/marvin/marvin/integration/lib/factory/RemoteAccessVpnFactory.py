import factory
from marvin.integration.lib.newbase import RemoteAccessVpn
class RemoteAccessVpnFactory(factory.Factory):

    FACTORY_FOR = RemoteAccessVpn

    publicipid = None
