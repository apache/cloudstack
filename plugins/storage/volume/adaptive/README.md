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
