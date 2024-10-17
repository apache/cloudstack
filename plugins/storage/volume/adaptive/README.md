# CloudStack Volume Provider Adaptive Plugin Base

The Adaptive Plugin Base is an abstract volume storage provider that
provides a generic implementation for managing volumes that are exposed
to hosts through FiberChannel and similar methods but managed independently
through a storage API or interface.  The ProviderAdapter, and associated
classes, provide a decoupled interface from the rest of
Cloudstack that covers the exact actions needed
to interface with a storage provider.  Each storage provider can extend
and implement the ProviderAdapter without needing to understand the internal
logic of volume management, database structure, etc.

## Implement the Provider Interface
To implement a provider, create another module -- or a standalone project --
and implement the following interfaces from the **org.apache.cloudstack.storage.datastore.adapter** package:

1. **ProviderAdapter** - this is the primary interface used to communicate with the storage provider when volume management actions are required.
2. **ProviderAdapterFactory** - the implementation of this class creates the correct ProviderAdapter when needed.

Follow Javadoc for each class on further instructions for implementing each function.

## Implement the Primary Datastore Provider Plugin
Once the provider interface is implemented, you will need to extend the **org.apache.cloudstack.storage.datastore.provider.AdaptiveProviderDatastoreProviderImpl** class.  When extending it, you simply need to implement a default
constructor that creates an instance of the ProviderAdapterFactory implementation created in #2 above.  Once created, you need to call the parent constructor and pass the factory object.

## Provide the Configuration for the Provider Plugin
Lastly, you need to include a module file and Spring configuration for your Primary Datastore Provider Plugin class so Cloudstack will load it during startup.

### Module Properties
This provides the hint to Cloudstack to load this as a module during startup.
```
#resources/META-INF/cloudstack/storage-volume-<providername>/module.properties
name=storage-volume-<providername>
parent=storage
```
### Spring Bean Context Configuration
This provides instructions of which provider implementation class to load when the Spring bean initilization is running.
```
<!-- resources/META-INF/cloudstack/storage-volume-<providername>/spring-storage-volume-<providername>-context.xml -->
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                      http://www.springframework.org/schema/beans/spring-beans.xsd
                      http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd
                      http://www.springframework.org/schema/context
                      http://www.springframework.org/schema/context/spring-context.xsd"
                      >

    <bean id="<providername>DataStoreProvider"
        class="org.apache.cloudstack.storage.datastore.provider.<providername>PrimaryDatastoreProviderImpl">
	  </bean>
</beans>
```
## Build and Deploy the Jar
Once you build the new jar, start Cloudstack Management Server or, if a standalone jar, add it to the classpath before start.  You should now have a new storage provider of the designated name once Cloudstack finishes loading
all configured modules.

### Test Cases
The following test cases should be run against configured installations of each storage array in a working Cloudstack installation.
1. Create New Primera Storage Pool for Zone
2. Create New Primera Storage Pool for Cluster
3. Update Primera Storage Pool for Zone
4. Update Primera Storage Pool for Cluster
5. Create VM with Root Disk using Primera pool
6. Create VM with Root and Data Disk using Primera pool
7. Create VM with Root Disk using NFS and Data Disk on Primera pool
8. Create VM with Root Disk on Primera Pool and Data Disk on NFS
9. Snapshot root disk with VM using Primera Pool for root disk
10. Snapshot data disk with VM using Primera Pool for data disk
11. Snapshot VM (non-memory) with root and data disk using Primera pool
12. Snapshot VM (non-memory) with root disk using Primera pool and data disk using NFS
13. Snapshot VM (non-memory) with root disk using NFS pool and data disk using Primera pool
14. Create new template from previous snapshot root disk on Primera pool
15. Create new volume from previous snapshot root disk on Primera pool
16. Create new volume from previous snapshot data disk on Primera pool
17. Create new VM using template created from Primera root snapshot and using Primera as root volume pool
18. Create new VM using template created from Primera root snapshot and using NFS as root volume pool
19. Delete previously created Primera snapshot
20. Create previously created Primera volume attached to a VM that is running (should fail)
21. Create previously created Primera volume attached to a VM that is not running (should fail)
22. Detach a Primera volume from a non-running VM (should work)
23. Attach a Primera volume to a running VM (should work)
24. Attach a Primera volume to a non-running VM (should work)
25. Create a 'thin' Disk Offering tagged for Primera pool and provision and attach a data volume to a VM using this offering (ttpv=true, reduce=false)
26. Create a 'sparse' Disk Offering tagged for Primera pool and provision and attach a data volume to a VM using this offering (ttpv=false, reduce=true)
27. Create a 'fat' Disk Offering and tagged for Primera pool and provision and attach a data volume to a VM using this offering (should fail as 'fat' not supported)
28. Perform volume migration of root volume from Primera pool to NFS pool on stopped VM
29. Perform volume migration of root volume from NFS pool to Primera pool on stopped VM
30. Perform volume migration of data volume from Primera pool to NFS pool on stopped VM
31. Perform volume migration of data volume from NFS pool to Primera pool on stopped VM
32. Perform VM data migration for a VM with 1 or more data volumes from all volumes on Primera pool to all volumes on NFS pool
33. Perform VM data migration for a VM with 1 or more data volumes from all volumes on NFS pool to all volumes on Primera pool
34. Perform live migration of a VM with a Primera root disk
35. Perform live migration of a VM with a Primera data disk and NFS root disk
36. Perform live migration of a VM with a Primera root disk and NFS data disk
37. Perform volume migration between 2 Primera pools on the same backend Primera IP address
38. Perform volume migration between 2 Primera pools on different Primera IP address
