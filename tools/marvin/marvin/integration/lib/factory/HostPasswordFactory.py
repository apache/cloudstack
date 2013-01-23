import factory
from marvin.integration.lib.newbase import HostPassword
class HostPasswordFactory(factory.Factory):

    FACTORY_FOR = HostPassword

    password = None
    username = None
