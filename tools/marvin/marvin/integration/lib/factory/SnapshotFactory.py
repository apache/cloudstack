import factory
from marvin.integration.lib.newbase import Snapshot
class SnapshotFactory(factory.Factory):

    FACTORY_FOR = Snapshot

    volumeid = None
