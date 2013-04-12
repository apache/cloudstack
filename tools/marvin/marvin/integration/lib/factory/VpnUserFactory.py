import factory
from marvin.integration.lib.base import VpnUser
class VpnUserFactory(factory.Factory):

    FACTORY_FOR = VpnUser

    password = None
    username = None
