import factory
from marvin.integration.lib.newbase import CloudIdentifier
class CloudIdentifierFactory(factory.Factory):

    FACTORY_FOR = CloudIdentifier

    userid = None
