import factory
from marvin.base import TrafficType
class TrafficTypeFactory(factory.Factory):

    FACTORY_FOR = TrafficType

    physicalnetworkid = None
    traffictype = None
    id = None
