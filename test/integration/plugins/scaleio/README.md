# PowerFlex/ScaleIO storage plugin
==================================
This directory contains the basic VM, Volume life cycle tests for PowerFlex/ScaleIO storage pool (in KVM hypervisor).

# Running tests
===============
To run these tests, first update the below test data of the CloudStack environment

````
TestData.zoneId: <id of zone>
TestData.clusterId: <id of cluster>
TestData.domainId: <id of domain>
TestData.url: <management server IP>
````
            
and PowerFlex/ScaleIO storage pool url at TestData.primaryStorage in the below format

````
powerflex://<api_user>:<api_password>@<gateway>/<storagepool>

    where,
    -	<api_user> : user name for API access
    -	<api_password> : url-encoded password for API access
    -	<gateway> : scaleio gateway host
    -	<storagepool> : storage pool name (case sensitive)


For example: "powerflex://admin:P%40ssword123@10.10.2.130/cspool"
````

Then run the tests using python unittest runner: nosetests

````
nosetests --with-marvin --marvin-config=<marvin-cfg-file> <cloudstack-dir>/test/integration/plugins/scaleio/test_scaleio_volumes.py --zone=<zone> --hypervisor=kvm
````

You can also run these tests out of the box with PyDev or PyCharm or whatever.
