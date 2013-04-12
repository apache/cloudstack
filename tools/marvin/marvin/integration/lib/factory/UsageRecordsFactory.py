import factory
from marvin.integration.lib.base import UsageRecords
class UsageRecordsFactory(factory.Factory):

    FACTORY_FOR = UsageRecords

    enddate = None
    startdate = None
