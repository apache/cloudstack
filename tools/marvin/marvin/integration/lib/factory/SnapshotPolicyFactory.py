import factory
from marvin.integration.lib.newbase import SnapshotPolicy
class SnapshotPolicyFactory(factory.Factory):

    FACTORY_FOR = SnapshotPolicy

    intervaltype = None
    maxsnaps = None
    schedule = None
    timezone = None
    volumeid = None
