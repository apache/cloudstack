import factory
from marvin.base import Cluster
from marvin.factory import CloudStackBaseFactory
from marvin.utils import random_gen

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
