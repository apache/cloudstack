import factory
from marvin.integration.lib.base import Cluster
from marvin.integration.lib.factory.CloudStackBaseFactory import CloudStackBaseFactory
from marvin.integration.lib.utils import random_gen

class ClusterFactory(CloudStackBaseFactory):

    FACTORY_FOR = Cluster

    clustername = None
    clustertype = None
    hypervisor = None
    podid = None
    zoneid = None

class XenClusterFactory(ClusterFactory):
    clustername = factory.Sequence(lambda n: "xencluster" + random_gen())
    clustertype = "XenServer"
    hypervisor = "XenServer"

class KvmClusterFactory(ClusterFactory):
    clustername = factory.Sequence(lambda n: "kvmcluster" + random_gen())
    clustertype = "KVM"
    hypervisor = "KVM"
