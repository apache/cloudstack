import factory
from marvin.integration.lib.base import TrafficMonitor
class TrafficMonitorFactory(factory.Factory):

    FACTORY_FOR = TrafficMonitor

    url = None
    zoneid = None
