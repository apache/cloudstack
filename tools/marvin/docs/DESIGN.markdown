# Marvin Refactor
The Marvin test framework will undergo some key improvements as part of this
refactor:

1. All CloudStack resources modelled as entities which are more object-oriented
2. Data modelled as factories that form basic building blocks
3. DSL support for assertions

## Introduction
Marvin which has been used thus far for testing has undergone several
significant changes in this refactor. Many of these changes were driven by the
need for succinctly describing a test scenario in a few lines of code. This
document describes the changes and the reasons behind this refactor. While this
makes the framework simple to use the internals of marvin have become a bit
complex. For this reason we will cover some of the internal workings as part of
this document.

## Rationale
Two main rationale were responsible for this refactor

1. Brittle nature of the integration library
2. Separating data from the test

### Integration library
Typically to write a test case previously the test case author was expected to
know (in advance) all the APIs he was going to call to complete his scenario.
With the growing list of APIs, their parameters and optional arguments it
becomes tedious often to compose a single API call. To overcome this the
integration libraries were written.  These libraries (`integration.lib.base,
integration.lib.common` etc) present a list of resources or entities - eg:
VirtualMachine, VPC, VLAN to the library user. Each entity can perform a set of
operations that in turn transform into an API call.

```python
class VirtualMachine(object):
    def deploy(self, apiclient, service, template, zone):
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        cmd.serviceofferingid = service
        cmd.templateid = template
    ...
    ...
    def list(self,apiclient)
        cmd = listVirtualMachines.listVirtualMachinesCmd()
        return apiclient.listVirtualMachines(cmd)
```
This makes the library usage more object-oriented. So in the testcase the
author only has to make a call to the VirtualMachine class when
creating/destroying/starting/stopping virtualmachine instances.

The disadvantage of this approach is that the integration library is
hand-written and brittle. When changes are made several tests are affected in
the process. There are also inconsistencies caused by mixing the data required
for the API call with the arguments of the operation being performed. eg:

```python
class VirtualMachine(object):
....
    @classmethod
    def create(cls, apiclient, services, templateid=None, accountid=None,
                    domainid=None, zoneid=None, networkids=None, serviceofferingid=None,
                    securitygroupids=None, projectid=None, startvm=None,
                    diskofferingid=None, affinitygroupnames=None, group=None,
                    hostid=None, keypair=None, mode='basic', method='GET'):
             ....
             ....
````
In this call, every argument is optionally lookedup in the services dictionary
or as part of the argument thereby complicating the body of the create(..)
call. Also the naming and the size of the API call is daunting for anyone using
the library.

### Data vs Test
Another major disadvantage of the previous approach was data required for the
test was mixed with the test itself.  This made it difficult to generate new
data from existing data objects. Data being highly coupled with the test
reduces readability.

Additionaly due to the strict structure of this data it would impose itself
onto the implementation of a resource's methods in the integration library.

However all of the data is reusable by other tests if presented as factories.
The refactor will address this using factories that act as building blocks for
creating reusable data. The document also describes how these blocks are extended.

## CloudStack API Generation
The process of API module generation remains the same as before. CloudStack
expresses its API in XML and JSON via the ApiDiscovery plugin. For instance the
createFirewallRule API looks as follows (some fields removed for brevity)

```json
 "api": [
            {
                "name": "createFirewallRule",
                "description": "Creates a firewall rule for a given ip address",
                "isasync": true,
                "params": [
                    {
                        "name": "cidrlist",
                        "description": "the cidr list to forward traffic from",
                        "type": "list",
                        "length": 255,
                        "required": false
                    },
                    {
                        "name": "icmpcode",
                    },
                    {
                        "name": "icmptype",
                    },
                    {
                        "name": "type",
                    },
                ],
                "response": [
                    {
                        "name": "state",
                        "description": "the state of the rule",
                        "type": "string"
                    },
                    {
                        "name": "endport",
                    },
                    {
                        "name": "protocol",
                    },
                ],
                "entity": "Firewall"
            }
        ]
 ```

This JSON/XML can be used to create a binding in your favorite language and for
Marvin's purpose this will be python.  An API module named
createFirewallRule.py with two classes (request and response) -
createFirewallRuleCmd and createFirewallRuleResponse represents the creation of
firewall rules.

### Changes to API Discovery
Generated API modules now include the `entity` attribute from the listApi
response. The API discovery plugin has been enhanced to include the type of
entity that an API is acting upon. For instance when doing createFirewallRule
the entity that the user is dealing with is the `Firewall`. We do not
intuitively guess what entity an API acts upon but depend on the CloudStack
endpoint to tell us this information. Mostly because we cannot always predict
the entity an API acts upon using the name of the API

eg: dedicatePublicIpRange

```json
listapisresponse: {
    count: 1,
    api: [
    {
        name: "dedicatePublicIpRange",
        description: "Dedicates a Public IP range to an account",
        isasync: false,
        related: "listVlanIpRanges",
        params: [],
        response: [],
        entity: "VlanIpRange"
     }
    ]
  }
}
```

This transforms into the following Marvin entity class through auto-generation:

```python
class VlanIpRange(CloudStackEntity):

    def dedicate(self, apiclient, account, domainid, **kwargs):
        cmd = dedicatePublicIpRange.dedicatePublicIpRangeCmd()
        cmd.id = self.id
        cmd.account = account
        cmd.domainid = domainid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        publiciprange = apiclient.dedicatePublicIpRange(cmd)
        return publiciprange if publiciprange else None

```

> kwargs represents all the optional arguments for dedicatePublicIpRange

The use of the entity in generating a higher level model for the CloudStack API
is described in the next section.

## Entity and Factory Generation
Marvin now includes a new module named `generate` that contains all the code
generators.

1. `xmltoapi.py` - this module is responsible for converting the JSON/XML
response to a python binding. Previously this was the `codegenerator.py`
2. `apitoentity.py` - this module is responsible for grouping actions on a
given entity into a single module and define all its actions as methods on the
entity object.
3. `entity.py` - is the base entity creator that transforms an API into a
cloudstackEntity
4. `factory.py` - is the base factory creator that transforms an API into a
factory

For eg: in the method createFirewallRule the `entity` is the Firewall and the
`action` being performed on the entity is `create`

So our entity becomes

```python
class Firewall:
    def create(...):
        createFirewallRule()
```

Almost all APIs are transformed naturally into this model but there are a few
exceptions. These exceptions are dealt with by the `linguist.py` module in
which APIs that don't split this way are broken down using special
transformers.

### Required and Optional Arguments
All required arguments to an API will be available in the API operation

```python
Entity.verb(reqd1=None, reqd2=None, ..., **kwargs)
```

Here the `Entity` (eg:Firewall) can perform an operation `verb()` (eg:create)
using the arguments `[reqd1, reqd2]`.  The optional arguments (if any) will be
passed as key, value pairs to the keyword args `**kwargs`.

All entity classes are autogenerated and placed in the `marvin.entity` module.
You may want to look at some sample entities like virtualmachine.py or
network.py. To anyone who has used the previous version of marvin, these will
look familiar. If you are looking at them for the first time, it will be
obvious to you that each entity is a simple class defined with CRUD operations
that map to the cloudStack API.

1. **Creators**
A creator of an entity is the API operation that brings the entity into
existence on the cloud. For instance a firewall rule is created using the
createFirewallRule API. Or a virtualmachine comes into existence with the
deployVirtualMachine command. These are our creators for entities firewall and
virtualmachines respectively. Every entity class's `__init__` method is
basically a call to its creator

2. **Enumerators**
Often it is not necessary to bring an entity into existence since it is already
present on the cloud infrastructure. We simply list* these entities and should
still be able to treat them and use them like entities created using their
corresponding creator methods. The list* APIs become our enumerators for each
entity.

## Factories
Factories in cloudstack are implemented using the
[factory_boy](http://factoryboy.readthedocs.org/en/latest/) framework.  The
factory_boy framework helps cloudstack define complex relationships in its
model. For eg.  In order to create a virtualmachine typically one needs a
service offering, a template and a zone present to be able to launch the VM.
Factory boy enables traversing these object relationships effectively
(top-down or bottom-up) to create those objects.

Every entity in the new framework is created using its corresponding factory
`EntityFactory`. Factories can be thought of as objects that carry necessary
and sufficient data to satisfy the API call that brings the entity into
existence.  For example in order to create an account the `AccountFactory` will
carry the `firstname, lastname, email, username` of the Account since these
are the required arguments to the `createAccount` API.

So the account factory looks as follows:

```python
import factory

class AccountFactory(factory):

    FACTORY_FOR = Account

    accounttype = None
    firstname = None
    lastname = None
    email = None
    username = None
    password = None
```

Here the `AccountFactory` is a bare representation with all None fields. These
are the default factories. The default factories are simply base classes for
defining hierarchical data using inheritance. For instance we have three
types of accounts in cloudstack - DomainAdmin, Admin and User

Each of these accounttypes represents an inheritance from the AccountFactory.
And for each factory we have a specific value for the `accounttype`. In fact we
don't have to repeat ourselves when defining a factory for each type of account:

> UserAccount(AccountFactory)

> AdminAccount(UserAccount) with (accounttype=1)

> DomainAdminAccount(UserAccount) with (accounttype=2)

By simply altering the accounttype and having Admin and DomainAdmin inherit
from User we have defined factories for all types of accounts in cloudstack

In order to create accounts in our tests all we have to do is the following:

```python
class TestAccounts(cloudstackTestCase):

    def setUp(...):
        apiclient = getApiClient()

    def test_AccountForUser(...):
        user = UserAccount(apiclient)
        assert user is valid

    def test_AccountForAdmin(...):
        admin = AdminAccount(apiclient)
        assert admin is valid

    def test_AccountForDomainAdmin(...):
        domadmin = DomainAdminAccount(apiclient)
        assert domadmin is active

    def tearDown(...):
        user.delete()
        admin.delete()
        domadmin.delete()
```

## Basic tools for extending factories

### Sequences
Sequences are provided by factory boy to randomize the object generated by each
call to the factory. Typically these are incremented integers but for the
CloudStack objects each distinguishing attribute is randomized to prevent
collisions and duplicate objects.

To define an attribute as a sequence we simply call the factory.Sequence(..)
method with a lambda function defining said sequence.

eg:

```python
    class SharedNetworkOffering(NetworkOfferingFactory):
        name = factory.Sequence(lambda n: 'SharedOffering' + my_random_generator_function(n))
        ...
```

### SubFactory
SubFactories are an important factory_boy building block for creating factories
that depend on other factories.

For eg: in order to create a SharedNetwork a networkofferingid of a
SharedNetworkOffering is required. So we first call on the factory of
SharedNetworkOffering using the factory.SubFactory(..) and use the id to create
the SharedNetwork using the SharedNetwork's factory

```python
class SharedNetwork(NetworkFactory):
    name = factory.Sequence(...)
    networkoffering = \
        factory.SubFactory(
            SharedNetworkOffering,
            attr1=val1
        )
    networkofferingid = networkoffering.id
```

RelatedFactory is a special case of SubFactory in that RelatedFactories are
created after the existing factory is created.

SubFactories are very powerful to chain many factories together to compose
complex objects in cloudstack.

### PostGeneration Hooks
In many cases additional hooks are done to simplify working with cloud
resources. For instance, when creating a virtual machine in an advanced zone it
is useful to associate a NAT rule to be able to SSH into the virtual machine
for post processing the effects on the virtualmachine like testing connectivity
to the internet for instance. PostGeneration hooks work after factories have
been created to perform such special functions. For examples, check the
`marvin.factory.data.vm` module for the VirtualMachineWithStaticNat factory
where we create a static nat rule allowing SSH access to the created VM.

## Guidelines for defining new factories
All factories are auto-generated and there is no need to define the default
factories. Test case authors will mostly be creating data factories inherited
from the default factories. All the data factories are defined in
`marvin.factory.data`. Currently implementations are provided for often used
data objects.

1. networkoffering
2. networks
3. service and disk offerings
4. security groups
5. virtualmachine
6. vpcoffering
7. vpcvirtualmachine
8. firewallrules
9. ingress and egress rules

and many more implementations should serve as examples to extend new data
objects.

Factory naming convention is simple. Any data inheriting from default factory
`EntityFactory` should be named without the suffix `Factory`. The data should
take the name of the purpose of the factory. Use simple prepositions
(Of,And,With etc) to combine words. For instance: VirtualMachineWithStaticNat
or VirtualMachineInIsolatedNetwork. Naming the data clearly aids its widespread
use. A badly named factory will likely not be used in more than one test.

## Should DSL assertions
The typical assertion capabilites of unittest are enough to express all
validation but it does not read naturally. Should_dsl is a library that makes
the assertions read like natural language. This is installed by default with
marvin now enabling all test cases to write assertions using simple dsl
statements

eg:

```python
    vm = VirtualMachineIsolatedNetwork(apiclient)
    vm.state | should | equal_to('Running')
    vm.nic | should_not | be(None)
```

## Utilities
All the pre-existing utilities from the previous `util.py` are still available
with enhancements in the util.py module. The legacy util.py module is
deprecated but retained since older tests refer to this module. All new changes
should go to the util.py under marvin/

## unittest2 and nose2
Marvin earlier was coupled with Python2.7 since python's unittest did not have
the same capabilites in versions <2.7. With unittest2 all features are now
backported to older python implementations. Marvin has also switched to
unittest2 so that we don't have to depend on the specific version of python to
be able to install and use marvin for testing. This change is internal and
should not be felt by the test case writer.

> There are plans to move to nose2 as well but this is separated from factory
> work at the moment.

## Legacy Libraries and Tests
In order to not disrupt the running of existing tests all the older libraries
in `base.py`, `common.py` and `util.py` are moved to the legacy module. Any new
tests should be written using factories. Older libraries are retained to be
able to run our existing tests whose imports will be switched as part of this
refactor.
