import factory
from marvin.integration.lib.newbase import DefaultZoneForAccount
class DefaultZoneForAccountFactory(factory.Factory):

    FACTORY_FOR = DefaultZoneForAccount

    account = None
    domainid = None
    zoneid = None
