import factory
from marvin.integration.lib.newbase import SSHKeyPair
class SSHKeyPairFactory(factory.Factory):

    FACTORY_FOR = SSHKeyPair

    name = None
