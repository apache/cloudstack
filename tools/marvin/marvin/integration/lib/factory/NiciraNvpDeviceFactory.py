import factory
from marvin.integration.lib.base import NiciraNvpDevice
class NiciraNvpDeviceFactory(factory.Factory):

    FACTORY_FOR = NiciraNvpDevice

    hostname = None
    password = None
    physicalnetworkid = None
    transportzoneuuid = None
    username = None
