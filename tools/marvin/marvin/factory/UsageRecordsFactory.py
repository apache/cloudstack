import factory
from marvin.base import UsageRecords
class UsageRecordsFactory(factory.Factory):

    FACTORY_FOR = UsageRecords

    enddate = None
    startdate = None
