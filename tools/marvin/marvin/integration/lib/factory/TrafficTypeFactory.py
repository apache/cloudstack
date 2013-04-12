import factory
from marvin.integration.lib.base import TrafficType
class TrafficTypeFactory(factory.Factory):

    FACTORY_FOR = TrafficType

    physicalnetworkid = None
    traffictype = None
    id = None
