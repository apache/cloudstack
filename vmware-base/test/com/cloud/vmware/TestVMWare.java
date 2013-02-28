// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.vmware;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DistributedVirtualSwitchMO;
import com.cloud.hypervisor.vmware.mo.HypervisorHostHelper;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.utils.PropertiesUtil;
import com.vmware.vim25.HostIpConfig;
import com.vmware.vim25.HostVirtualNicSpec;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.DVPortgroupConfigInfo;
import com.vmware.vim25.DVPortgroupConfigSpec;
import com.vmware.vim25.DVSSecurityPolicy;
import com.vmware.vim25.DVSTrafficShapingPolicy;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.HostConfigManager;
import com.vmware.vim25.HostIpConfig;
import com.vmware.vim25.HostPortGroupSpec;
import com.vmware.vim25.HostVirtualNicSpec;
import com.vmware.vim25.HttpNfcLeaseDeviceUrl;
import com.vmware.vim25.HttpNfcLeaseInfo;
import com.vmware.vim25.HttpNfcLeaseState;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.OvfCreateImportSpecParams;
import com.vmware.vim25.OvfCreateImportSpecResult;
import com.vmware.vim25.OvfFileItem;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VMwareDVSPortSetting;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualNicManagerNetConfig;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VmwareDistributedVirtualSwitchVlanSpec;

public class TestVMWare {
	private static ExtendedAppUtil cb;
    private static String[] _args;

    private static final int IND_DATACENTER_MOR = 3;
    private static final int IND_DVSWITCH_MOR = 4;
    private static final int IND_DVSWITCH_NAME = 5;
    private static final int IND_DVPORTGROUP_NAME = 6;
    private static final int IND_DVPORTGROUP_VLAN = 7;
    private static final int IND_DVPORTGROUP_PORTCOUNT = 8;
    private static final int MAX_ARGS = 9;
	static {
		try {
			javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1]; 
			javax.net.ssl.TrustManager tm = new TrustAllManager(); 
			trustAllCerts[0] = tm; 
			javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL"); 
			sc.init(null, trustAllCerts, null); 
			javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
		}
	}
	
	private static void setupLog4j() {
	   File file = PropertiesUtil.findConfigFile("log4j-cloud.xml");

	   if(file != null) {
		   System.out.println("Log4j configuration from : " + file.getAbsolutePath());
		   DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
	   } else {
		   System.out.println("Configure log4j with default properties");
	   }
	}
	
	private void getAndPrintInventoryContents() throws Exception {
      TraversalSpec resourcePoolTraversalSpec = new TraversalSpec();
      resourcePoolTraversalSpec.setName("resourcePoolTraversalSpec");
      resourcePoolTraversalSpec.setType("ResourcePool");
      resourcePoolTraversalSpec.setPath("resourcePool");
      resourcePoolTraversalSpec.setSkip(new Boolean(false));
      resourcePoolTraversalSpec.setSelectSet(
      new SelectionSpec [] { new SelectionSpec(null,null,"resourcePoolTraversalSpec") });

      TraversalSpec computeResourceRpTraversalSpec = new TraversalSpec();
      computeResourceRpTraversalSpec.setName("computeResourceRpTraversalSpec");
      computeResourceRpTraversalSpec.setType("ComputeResource");
      computeResourceRpTraversalSpec.setPath("resourcePool");
      computeResourceRpTraversalSpec.setSkip(new Boolean(false));
      computeResourceRpTraversalSpec.setSelectSet(
      new SelectionSpec [] { new SelectionSpec(null,null,"resourcePoolTraversalSpec") });

      TraversalSpec computeResourceHostTraversalSpec = new TraversalSpec();
      computeResourceHostTraversalSpec.setName("computeResourceHostTraversalSpec");
      computeResourceHostTraversalSpec.setType("ComputeResource");
      computeResourceHostTraversalSpec.setPath("host");
      computeResourceHostTraversalSpec.setSkip(new Boolean(false));

      TraversalSpec datacenterHostTraversalSpec = new TraversalSpec();
      datacenterHostTraversalSpec.setName("datacenterHostTraversalSpec");
      datacenterHostTraversalSpec.setType("Datacenter");
      datacenterHostTraversalSpec.setPath("hostFolder");
      datacenterHostTraversalSpec.setSkip(new Boolean(false));
      datacenterHostTraversalSpec.setSelectSet(
      new SelectionSpec [] { new SelectionSpec(null,null,"folderTraversalSpec") });

      TraversalSpec datacenterVmTraversalSpec = new TraversalSpec();
      datacenterVmTraversalSpec.setName("datacenterVmTraversalSpec");
      datacenterVmTraversalSpec.setType("Datacenter");
      datacenterVmTraversalSpec.setPath("vmFolder");
      datacenterVmTraversalSpec.setSkip(new Boolean(false));
      datacenterVmTraversalSpec.setSelectSet(
      new SelectionSpec [] { new SelectionSpec(null,null,"folderTraversalSpec") });

      TraversalSpec folderTraversalSpec = new TraversalSpec();
      folderTraversalSpec.setName("folderTraversalSpec");
      folderTraversalSpec.setType("Folder");
      folderTraversalSpec.setPath("childEntity");
      folderTraversalSpec.setSkip(new Boolean(false));
      folderTraversalSpec.setSelectSet(
      new SelectionSpec [] { new SelectionSpec(null,null,"folderTraversalSpec"),
                             datacenterHostTraversalSpec,
                             datacenterVmTraversalSpec,
                             computeResourceRpTraversalSpec,
                             computeResourceHostTraversalSpec,
                             resourcePoolTraversalSpec });      

      PropertySpec[] propspecary = new PropertySpec[] { new PropertySpec() };
      propspecary[0].setAll(new Boolean(false));
      propspecary[0].setPathSet(new String[] { "name" });
      propspecary[0].setType("ManagedEntity");

      PropertyFilterSpec spec = new PropertyFilterSpec();
      spec.setPropSet(propspecary);
      spec.setObjectSet(new ObjectSpec[] { new ObjectSpec() });
      spec.getObjectSet(0).setObj(cb.getServiceConnection3().getRootFolder());
      spec.getObjectSet(0).setSkip(new Boolean(false));
      spec.getObjectSet(0).setSelectSet(
      new SelectionSpec[] { folderTraversalSpec });      

      // Recursively get all ManagedEntity ManagedObjectReferences 
      // and the "name" property for all ManagedEntities retrieved
      ObjectContent[] ocary = 
        cb.getServiceConnection3().getService().retrieveProperties(
        cb.getServiceConnection3().getServiceContent().getPropertyCollector(), 
           new PropertyFilterSpec[] { spec }
      );

      // If we get contents back. print them out.
      if (ocary != null) {
         ObjectContent oc = null;
         ManagedObjectReference mor = null;
         DynamicProperty[] pcary = null;
         DynamicProperty pc = null;
         for (int oci = 0; oci < ocary.length; oci++) {
            oc = ocary[oci];
            mor = oc.getObj();
            pcary = oc.getPropSet();

            System.out.println("Object Type : " + mor.getType());
            System.out.println("Reference Value : " + mor.get_value());

            if (pcary != null) {
               for (int pci = 0; pci < pcary.length; pci++) {
                  pc = pcary[pci];
                  System.out.println("   Property Name : " + pc.getName());
                  if (pc != null) {
                     if (!pc.getVal().getClass().isArray()) {
                        System.out.println("   Property Value : " + pc.getVal());
                     } 
                     else {
                        Object[] ipcary = (Object[])pc.getVal();
                        System.out.println("Val : " + pc.getVal());
                        for (int ii = 0; ii < ipcary.length; ii++) {
                           Object oval = ipcary[ii];
                           if (oval.getClass().getName().indexOf("ManagedObjectReference") >= 0) {
                              ManagedObjectReference imor = (ManagedObjectReference)oval;

                              System.out.println("Inner Object Type : " + imor.getType());
                              System.out.println("Inner Reference Value : " + imor.get_value());
                           } 
                           else {
                              System.out.println("Inner Property Value : " + oval);
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         System.out.println("No Managed Entities retrieved!");
      }
	}
	
	private void listDataCenters() {
		try { 
			ManagedObjectReference[] morDatacenters = getDataCenterMors();
			if(morDatacenters != null) {
				for(ManagedObjectReference mor : morDatacenters) {
					System.out.println("Datacenter : " + mor.get_value());
					
					Map<String, Object> properites = new HashMap<String, Object>();
					properites.put("name", null);
					properites.put("vmFolder", null);
					properites.put("hostFolder", null);
					
					getProperites(mor, properites);
					for(Map.Entry<String, Object> entry : properites.entrySet()) {
						if(entry.getValue() instanceof ManagedObjectReference) {
							ManagedObjectReference morProp = (ManagedObjectReference)entry.getValue();
							System.out.println("\t" + entry.getKey() + ":(" + morProp.getType() + ", " + morProp.get_value() + ")");
						} else {
							System.out.println("\t" + entry.getKey() + ":" + entry.getValue());
						}
					}
					
					System.out.println("Datacenter clusters");
					ManagedObjectReference[] clusters = getDataCenterClusterMors(mor);
					if(clusters != null) {
						for(ManagedObjectReference morCluster : clusters) {
							Object[] props = this.getProperties(morCluster, new String[] {"name"});
							System.out.println("cluster : " + props[0]);
							
							System.out.println("cluster hosts");
							ManagedObjectReference[] hosts = getClusterHostMors(morCluster);
							if(hosts != null) {
								for(ManagedObjectReference morHost : hosts) {
									Object[] props2 = this.getProperties(morHost, new String[] {"name"});
									System.out.println("host : " + props2[0]);
								}
							}
						}
					}
					
					System.out.println("Datacenter standalone hosts");
					ManagedObjectReference[] hosts = getDataCenterStandaloneHostMors(mor);
					if(hosts != null) {
						for(ManagedObjectReference morHost : hosts) {
							Object[] props = this.getProperties(morHost, new String[] {"name"});
							System.out.println("host : " + props[0]);
						}
					}
					
					System.out.println("Datacenter datastores");
					ManagedObjectReference[] stores = getDataCenterDatastoreMors(mor);
					if(stores != null) {
						for(ManagedObjectReference morStore : stores) {
							// data store name property does not work for some reason
							Object[] props = getProperties(morStore, new String[] {"info" });
							
							System.out.println(morStore.getType() + ": " + ((DatastoreInfo)props[0]).getName());
						}
					}
					
					System.out.println("Datacenter VMs");
					ManagedObjectReference[] vms = getDataCenterVMMors(mor);
					if(stores != null) {
						for(ManagedObjectReference morVm : vms) {
							Object[] props = this.getProperties(morVm, new String[] {"name"});
							System.out.println("VM name: " + props[0] + ", ref val: " + morVm.get_value());
						}
					}
				}
			}
		} catch(RuntimeFault e) {
			e.printStackTrace();
		} catch(RemoteException e) {
			e.printStackTrace();
		}
	}
	
	private void listInventoryFolders() {
	    TraversalSpec folderTraversalSpec = new TraversalSpec();
	    folderTraversalSpec.setName("folderTraversalSpec");
	    folderTraversalSpec.setType("Folder");
	    folderTraversalSpec.setPath("childEntity");
	    folderTraversalSpec.setSkip(new Boolean(false));
	    folderTraversalSpec.setSelectSet(
	    	new SelectionSpec [] { new SelectionSpec(null, null, "folderTraversalSpec")} 
	    );
	    
	    PropertySpec[] propSpecs = new PropertySpec[] { new PropertySpec() };
	    propSpecs[0].setAll(new Boolean(false));
	    propSpecs[0].setPathSet(new String[] { "name" });
	    propSpecs[0].setType("ManagedEntity");
	    
	    PropertyFilterSpec filterSpec = new PropertyFilterSpec();
	    filterSpec.setPropSet(propSpecs);
	    filterSpec.setObjectSet(new ObjectSpec[] { new ObjectSpec() });
	    filterSpec.getObjectSet(0).setObj(cb.getServiceConnection3().getRootFolder());
	    filterSpec.getObjectSet(0).setSkip(new Boolean(false));
	    filterSpec.getObjectSet(0).setSelectSet(
    		new SelectionSpec[] { folderTraversalSpec }
    	);      
	    
	    try {
			ObjectContent[] objContent = cb.getServiceConnection3().getService().retrieveProperties(
				cb.getServiceConnection3().getServiceContent().getPropertyCollector(), 
				new PropertyFilterSpec[] { filterSpec } 
			);
			printContent(objContent);
		} catch (InvalidProperty e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	private TraversalSpec getFolderRecursiveTraversalSpec() {
	    SelectionSpec recurseFolders = new SelectionSpec();
	    recurseFolders.setName("folder2childEntity");
	      
	    TraversalSpec folder2childEntity = new TraversalSpec();
	    folder2childEntity.setType("Folder");
	    folder2childEntity.setPath("childEntity");
	    folder2childEntity.setName(recurseFolders.getName());
	    folder2childEntity.setSelectSet(new SelectionSpec[] { recurseFolders });
	    
	    return folder2childEntity;
	}
	
	private ManagedObjectReference[] getDataCenterMors() throws RuntimeFault, RemoteException {
		PropertySpec pSpec = new PropertySpec();
	    pSpec.setType("Datacenter");
	    pSpec.setPathSet(new String[] { "name"} );

	    ObjectSpec oSpec = new ObjectSpec();
	    oSpec.setObj(cb.getServiceConnection3().getRootFolder());
	    oSpec.setSkip(Boolean.TRUE);
	    oSpec.setSelectSet(new SelectionSpec[] { getFolderRecursiveTraversalSpec() });

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.setPropSet(new PropertySpec[] { pSpec });
	    pfSpec.setObjectSet(new ObjectSpec[] { oSpec });
	      
	    ObjectContent[] ocs = cb.getServiceConnection3().getService().retrieveProperties(
            cb.getServiceConnection3().getServiceContent().getPropertyCollector(),
            new PropertyFilterSpec[] { pfSpec });
	    
	    if(ocs != null) {
	    	ManagedObjectReference[] morDatacenters = new ManagedObjectReference[ocs.length];
	    	for(int i = 0; i < ocs.length; i++)
	    		morDatacenters[i] = ocs[i].getObj();
	    	
	    	return morDatacenters;
	    }
	    return null;
	}
	
	private ManagedObjectReference[] getDataCenterVMMors(ManagedObjectReference morDatacenter) throws RuntimeFault, RemoteException {
		PropertySpec pSpec = new PropertySpec();
	    pSpec.setType("VirtualMachine");
	    pSpec.setPathSet(new String[] { "name"} );

	    ObjectSpec oSpec = new ObjectSpec();
	    oSpec.setObj(morDatacenter);
	    oSpec.setSkip(Boolean.TRUE);

	    TraversalSpec tSpec = new TraversalSpec();
	    tSpec.setName("dc2VMFolder");
	    tSpec.setType("Datacenter");
	    tSpec.setPath("vmFolder");
	    tSpec.setSelectSet(new SelectionSpec[] { getFolderRecursiveTraversalSpec() } );
	    
	    oSpec.setSelectSet(new SelectionSpec[] { tSpec });

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.setPropSet(new PropertySpec[] { pSpec });
	    pfSpec.setObjectSet(new ObjectSpec[] { oSpec });
	      
	    ObjectContent[] ocs = cb.getServiceConnection3().getService().retrieveProperties(
            cb.getServiceConnection3().getServiceContent().getPropertyCollector(),
            new PropertyFilterSpec[] { pfSpec });
	    
	    if(ocs != null) {
	    	ManagedObjectReference[] morVMs = new ManagedObjectReference[ocs.length];
	    	for(int i = 0; i < ocs.length; i++)
	    		morVMs[i] = ocs[i].getObj();
	    	
	    	return morVMs;
	    }
	    return null;
	}
	
	private ManagedObjectReference[] getDataCenterDatastoreMors(ManagedObjectReference morDatacenter) throws RuntimeFault, RemoteException {
		Object[] stores = getProperties(morDatacenter, new String[] { "datastore" });
		if(stores != null && stores.length == 1) {
			return ((ArrayOfManagedObjectReference)stores[0]).getManagedObjectReference();
		}
		return null;
	}
	
	private ManagedObjectReference[] getDataCenterClusterMors(ManagedObjectReference morDatacenter) throws RuntimeFault, RemoteException {
		PropertySpec pSpec = new PropertySpec();
	    pSpec.setType("ClusterComputeResource");
	    pSpec.setPathSet(new String[] { "name"} );

	    ObjectSpec oSpec = new ObjectSpec();
	    oSpec.setObj(morDatacenter);
	    oSpec.setSkip(Boolean.TRUE);
	    
	    TraversalSpec tSpec = new TraversalSpec();
	    tSpec.setName("traversalHostFolder");
	    tSpec.setType("Datacenter");
	    tSpec.setPath("hostFolder");
	    tSpec.setSkip(false);
	    tSpec.setSelectSet(new SelectionSpec[] { getFolderRecursiveTraversalSpec() });
	    
	    oSpec.setSelectSet(new TraversalSpec[] { tSpec });

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.setPropSet(new PropertySpec[] { pSpec });
	    pfSpec.setObjectSet(new ObjectSpec[] { oSpec });
	      
	    ObjectContent[] ocs = cb.getServiceConnection3().getService().retrieveProperties(
            cb.getServiceConnection3().getServiceContent().getPropertyCollector(),
            new PropertyFilterSpec[] { pfSpec });
	    
	    if(ocs != null) {
	    	ManagedObjectReference[] morDatacenters = new ManagedObjectReference[ocs.length];
	    	for(int i = 0; i < ocs.length; i++)
	    		morDatacenters[i] = ocs[i].getObj();
	    	
	    	return morDatacenters;
	    }
	    return null;
	}
	
	private ManagedObjectReference[] getDataCenterStandaloneHostMors(ManagedObjectReference morDatacenter) throws RuntimeFault, RemoteException {
		PropertySpec pSpec = new PropertySpec();
	    pSpec.setType("ComputeResource");
	    pSpec.setPathSet(new String[] { "name"} );

	    ObjectSpec oSpec = new ObjectSpec();
	    oSpec.setObj(morDatacenter);
	    oSpec.setSkip(Boolean.TRUE);
	    
	    TraversalSpec tSpec = new TraversalSpec();
	    tSpec.setName("traversalHostFolder");
	    tSpec.setType("Datacenter");
	    tSpec.setPath("hostFolder");
	    tSpec.setSkip(false);
	    tSpec.setSelectSet(new SelectionSpec[] { getFolderRecursiveTraversalSpec() });
	    
	    oSpec.setSelectSet(new TraversalSpec[] { tSpec });

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.setPropSet(new PropertySpec[] { pSpec });
	    pfSpec.setObjectSet(new ObjectSpec[] { oSpec });
	      
	    ObjectContent[] ocs = cb.getServiceConnection3().getService().retrieveProperties(
            cb.getServiceConnection3().getServiceContent().getPropertyCollector(),
            new PropertyFilterSpec[] { pfSpec });
	    
	    if(ocs != null) {
	    	List<ManagedObjectReference> listComputeResources = new ArrayList<ManagedObjectReference>();
	    	for(ObjectContent oc : ocs) {
	    		if(oc.getObj().getType().equalsIgnoreCase("ComputeResource"))
	    			listComputeResources.add(oc.getObj());
	    	}
	    	
	    	List<ManagedObjectReference> listHosts = new ArrayList<ManagedObjectReference>();
	    	for(ManagedObjectReference morComputeResource : listComputeResources) {
	    		ManagedObjectReference[] hosts = getComputeResourceHostMors(morComputeResource);
	    		if(hosts != null) {
	    			for(ManagedObjectReference host: hosts)
	    				listHosts.add(host);
	    		}
	    	}
	    	
	    	return listHosts.toArray(new ManagedObjectReference[0]);
	    }
	    return null;
	}
	
	private ManagedObjectReference[] getComputeResourceHostMors(ManagedObjectReference morCompute) throws RuntimeFault, RemoteException {
		PropertySpec pSpec = new PropertySpec();
	    pSpec.setType("HostSystem");
	    pSpec.setPathSet(new String[] { "name"} );

	    ObjectSpec oSpec = new ObjectSpec();
	    oSpec.setObj(morCompute);
	    oSpec.setSkip(true);
	    
	    TraversalSpec tSpec = new TraversalSpec();
	    tSpec.setName("computeResource2Host");
	    tSpec.setType("ComputeResource");
	    tSpec.setPath("host");
	    tSpec.setSkip(false);
	    oSpec.setSelectSet(new TraversalSpec[] { tSpec });

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.setPropSet(new PropertySpec[] { pSpec });
	    pfSpec.setObjectSet(new ObjectSpec[] { oSpec });
	    
	    ObjectContent[] ocs = cb.getServiceConnection3().getService().retrieveProperties(
            cb.getServiceConnection3().getServiceContent().getPropertyCollector(),
            new PropertyFilterSpec[] { pfSpec });
	    
	    if(ocs != null) {
	    	ManagedObjectReference[] morDatacenters = new ManagedObjectReference[ocs.length];
	    	for(int i = 0; i < ocs.length; i++)
	    		morDatacenters[i] = ocs[i].getObj();
	    	
	    	return morDatacenters;
	    }
	    return null;
	}
	
	private ManagedObjectReference[] getClusterHostMors(ManagedObjectReference morCluster) throws RuntimeFault, RemoteException {
		// ClusterComputeResource inherits from ComputeResource
		return getComputeResourceHostMors(morCluster);
	}
	
	private ObjectContent[] getDataCenterProperites(String[] properites) throws RuntimeFault, RemoteException {
		PropertySpec pSpec = new PropertySpec();
	    pSpec.setType("Datacenter");
	    pSpec.setPathSet(properites );

	    SelectionSpec recurseFolders = new SelectionSpec();
	    recurseFolders.setName("folder2childEntity");
	      
	    TraversalSpec folder2childEntity = new TraversalSpec();
	    folder2childEntity.setType("Folder");
	    folder2childEntity.setPath("childEntity");
	    folder2childEntity.setName(recurseFolders.getName());
	    folder2childEntity.setSelectSet(new SelectionSpec[] { recurseFolders });

	    ObjectSpec oSpec = new ObjectSpec();
	    oSpec.setObj(cb.getServiceConnection3().getRootFolder());
	    oSpec.setSkip(Boolean.TRUE);
	    oSpec.setSelectSet(new SelectionSpec[] { folder2childEntity });

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.setPropSet(new PropertySpec[] { pSpec });
	    pfSpec.setObjectSet(new ObjectSpec[] { oSpec });
	      
	    return cb.getServiceConnection3().getService().retrieveProperties(
            cb.getServiceConnection3().getServiceContent().getPropertyCollector(),
            new PropertyFilterSpec[] { pfSpec });
	}
	
	private void printContent(ObjectContent[] objContent) {
		if(objContent != null) {
			for(ObjectContent oc : objContent) {
				ManagedObjectReference mor = oc.getObj();
		        DynamicProperty[] objProps = oc.getPropSet();

		        System.out.println("Object type: " + mor.getType());
		        if(objProps != null) {
		        	for(DynamicProperty objProp : objProps) {
		        		if(!objProp.getClass().isArray()) {
		        			System.out.println("\t" + objProp.getName() + "=" + objProp.getVal());
		        		} else {
	                        Object[] ipcary = (Object[])objProp.getVal();
	                        System.out.print("\t" + objProp.getName() + "=[");
	                        int i = 0;
	                        for(Object item : ipcary) {
	                            if (item.getClass().getName().indexOf("ManagedObjectReference") >= 0) {
	                                ManagedObjectReference imor = (ManagedObjectReference)item;
	                            	System.out.print("(" + imor.getType() + "," + imor.get_value() + ")");
	                            } else {
	                            	System.out.print(item);
	                            }
	                            
	                            if(i < ipcary.length - 1)
	                            	System.out.print(", ");
	                            i++;
	                        }
	                        
	                        System.out.println("]");
		        		}
		        	}
		        }
			}
		}
	}
	
	private void getProperites(ManagedObjectReference mor, Map<String, Object> properties) throws RuntimeFault, RemoteException {
		PropertySpec pSpec = new PropertySpec();
		pSpec.setType(mor.getType());
		pSpec.setPathSet(properties.keySet().toArray(new String[0]));
		
		ObjectSpec oSpec = new ObjectSpec();
		oSpec.setObj(mor);
		
		PropertyFilterSpec pfSpec = new PropertyFilterSpec();
		pfSpec.setPropSet(new PropertySpec[] {pSpec} );
		pfSpec.setObjectSet(new ObjectSpec[] {oSpec} );
		
		ObjectContent[] ocs = cb.getServiceConnection3().getService().retrieveProperties(
	         cb.getServiceConnection3().getServiceContent().getPropertyCollector(),
	         new PropertyFilterSpec[] {pfSpec} );
		
		if(ocs != null) {
			for(ObjectContent oc : ocs) {
				DynamicProperty[] propSet = oc.getPropSet();
				if(propSet != null) {
					for(DynamicProperty prop : propSet) {
						properties.put(prop.getName(), prop.getVal());
					}
				}
			}
		}
	}
	
	private Object[] getProperties(ManagedObjectReference moRef, String[] properties) throws RuntimeFault, RemoteException {
		PropertySpec pSpec = new PropertySpec();
		pSpec.setType(moRef.getType());
		pSpec.setPathSet(properties);

		ObjectSpec oSpec = new ObjectSpec();
		// Set the starting object
		oSpec.setObj(moRef);
		
		PropertyFilterSpec pfSpec = new PropertyFilterSpec();
		pfSpec.setPropSet(new PropertySpec[] {pSpec} );
		pfSpec.setObjectSet(new ObjectSpec[] {oSpec} );
		ObjectContent[] ocs = cb.getServiceConnection3().getService().retrieveProperties(
	         cb.getServiceConnection3().getServiceContent().getPropertyCollector(),
	         new PropertyFilterSpec[] {pfSpec} );

		Object[] ret = new Object[properties.length];
		if(ocs != null) {
			for(int i = 0; i< ocs.length; ++i) {
				ObjectContent oc = ocs[i];
				DynamicProperty[] dps = oc.getPropSet();
				if(dps != null) {
					for(int j = 0; j < dps.length; ++j) {
						DynamicProperty dp = dps[j];
						for(int p = 0; p < ret.length; ++p) {
							if(properties[p].equals(dp.getName())) {
								ret[p] = dp.getVal();
							}
						}
					}
				}
			}
		}
		return ret;
	}

	private void powerOnVm() throws Exception {
		ManagedObjectReference morVm = new ManagedObjectReference();
		morVm.setType("VirtualMachine");
		morVm.set_value("vm-480");
		
		cb.getServiceConnection3().getService().powerOnVM_Task(morVm, null);
	}
	
	private void powerOffVm() throws Exception {
		ManagedObjectReference morVm = new ManagedObjectReference();
		morVm.setType("VirtualMachine");
		morVm.set_value("vm-66");
		
		cb.getServiceConnection3().getService().powerOffVM_Task(morVm);
	}
	
	private void createSnapshot() throws Exception {
		ManagedObjectReference morVm = new ManagedObjectReference();
		morVm.setType("VirtualMachine");
		morVm.set_value("vm-66");
		cb.getServiceConnection3().getService().createSnapshot_Task(morVm, "RunningSnapshotProg", "", false, false);
	}
	
	private void registerTemplate() throws Exception {
      ManagedObjectReference morFolder = new ManagedObjectReference();
      morFolder.setType("Folder");
      morFolder.set_value("group-v3");
      
      ManagedObjectReference morHost = new ManagedObjectReference();
      morHost.setType("HostSystem");
      morHost.set_value("host-48");

      System.out.println("Begin registerVM_Task");
      ManagedObjectReference taskmor = cb.getServiceConnection3().getService().registerVM_Task(
    		  morFolder, "[NFS datastore] Template-Fedora/Template-Fedora.vmtx", "Template-Fedora", true, 
		  null, morHost);
      System.out.println("End registerVM_Task");

      String result = cb.getServiceUtil3().waitForTask(taskmor);
      if (result.equalsIgnoreCase("Sucess")) {
    	  System.out.println("Registering The Virtual Machine ..........Done");
      } else {
    	  System.out.println("Some Exception While Registering The VM");
      }
	}
	
	private void createVmFromTemplate() throws Exception {
	     VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
	     
	     ManagedObjectReference morDatastore = new ManagedObjectReference();
	     morDatastore.setType("Datastore");
	     morDatastore.set_value("datastore-30");
	     
	     ManagedObjectReference morHost = new ManagedObjectReference();
	     morHost.setType("HostSystem");
	     morHost.set_value("host-48");
	     
	     ManagedObjectReference morPool = new ManagedObjectReference();
	     morPool.setType("ResourcePool");
	     morPool.set_value("resgroup-41");
	     
	     VirtualMachineRelocateSpec relocSpec = new VirtualMachineRelocateSpec();
	     cloneSpec.setLocation(relocSpec);
	     cloneSpec.setPowerOn(false);
	     cloneSpec.setTemplate(false);
	     
	     relocSpec.setDatastore(morDatastore);
	     relocSpec.setHost(morHost);
	     relocSpec.setPool(morPool);
	     
	     ManagedObjectReference morTemplate = new ManagedObjectReference();
	     morTemplate.setType("VirtualMachine");
	     morTemplate.set_value("vm-76");
	     
	     ManagedObjectReference morFolder = new ManagedObjectReference();
	     morFolder.setType("Folder");
	     morFolder.set_value("group-v3");
	      
         ManagedObjectReference cloneTask 
            = cb.getServiceConnection3().getService().cloneVM_Task(morTemplate, morFolder, 
            	"Fedora-clone-test", cloneSpec);
         
         String status = cb.getServiceUtil3().waitForTask(cloneTask);
         if(status.equalsIgnoreCase("failure")) {
            System.out.println("Failure -: Virtual Machine cannot be cloned");
         }
                  
         if(status.equalsIgnoreCase("sucess")) {
            System.out.println("Virtual Machine Cloned  successfully.");
         }
	}
	
	private void addNic() throws Exception {
		ManagedObjectReference morVm = new ManagedObjectReference();
		morVm.setType("VirtualMachine");
		morVm.set_value("vm-77");
		
		ManagedObjectReference morNetwork = new ManagedObjectReference();
		morNetwork.setType("DistributedVirtualPortgroup");
		morNetwork.set_value("dvportgroup-56");
		
		VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
        nicSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
        VirtualEthernetCard nic =  new VirtualPCNet32();
        VirtualEthernetCardNetworkBackingInfo nicBacking 
           = new VirtualEthernetCardNetworkBackingInfo();
        nicBacking.setDeviceName("Adapter to dSwitch-vlan26");
        nicBacking.setNetwork(morNetwork);
        
        nic.setAddressType("generated");
        nic.setBacking(nicBacking);
        nic.setKey(4);
        nicSpec.setDevice(nic);
        
        VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
        VirtualDeviceConfigSpec [] nicSpecArray = {nicSpec};                     
        vmConfigSpec.setDeviceChange(nicSpecArray);
        
        ManagedObjectReference tmor 
        	= cb.getServiceConnection3().getService().reconfigVM_Task(
            morVm, vmConfigSpec);
        
        String status = cb.getServiceUtil3().waitForTask(tmor);
        if(status.equalsIgnoreCase("failure")) {
           System.out.println("Failure -: Virtual Machine cannot be cloned");
        }
                 
        if(status.equalsIgnoreCase("sucess")) {
           System.out.println("Virtual Machine Cloned  successfully.");
        }
	}
	
	// add virtual NIC to vmkernel
	private void addNicToNetwork() throws Exception {
		ManagedObjectReference morHost = new ManagedObjectReference();
		morHost.setType("HostSystem");
		morHost.set_value("host-48");
		
        HostPortGroupSpec portgrp = new HostPortGroupSpec();
        portgrp.setName("VM Network vlan26");
		
        Object cmobj = cb.getServiceUtil3().getDynamicProperty(morHost, "configManager");
        HostConfigManager configMgr = (HostConfigManager)cmobj;
        ManagedObjectReference nwSystem = configMgr.getNetworkSystem();
        
        HostVirtualNicSpec vNicSpec = new HostVirtualNicSpec();
        HostIpConfig ipConfig = new HostIpConfig();
        ipConfig.setDhcp(false);
        ipConfig.setIpAddress("192.168.26.177");
        ipConfig.setSubnetMask("255.255.255.0");
        
        vNicSpec.setIp(ipConfig);
        vNicSpec.setPortgroup("VM Network vlan26");
        
        cb.getServiceConnection3().getService().addVirtualNic(nwSystem, 
        		"dvPortGroup-vlan26", vNicSpec);
	}
	
	private void createDatacenter() throws Exception {
		cb.getServiceConnection3().getService().createDatacenter(
			cb.getServiceConnection3().getRootFolder(), 
			"cloud.dc.test");
	}
	
	private void getPropertyWithPath() throws Exception {
		ManagedObjectReference morHost = new ManagedObjectReference();
		morHost.setType("HostSystem");
		morHost.set_value("host-161");
		
		VirtualNicManagerNetConfig[] netConfigs = (VirtualNicManagerNetConfig[])cb.getServiceUtil3().getDynamicProperty(morHost, "config.virtualNicManagerInfo.netConfig");
	}
	
	private void getHostVMs() throws Exception {
		ManagedObjectReference morHost = new ManagedObjectReference();
		morHost.setType("HostSystem");
		morHost.set_value("host-48");
		
		PropertySpec pSpec = new PropertySpec();
		pSpec.setType("VirtualMachine");
		pSpec.setPathSet(new String[] { "name", "runtime.powerState", "config.template" });
		
	    TraversalSpec host2VmTraversal = new TraversalSpec();
	    host2VmTraversal.setType("HostSystem");
	    host2VmTraversal.setPath("vm");
	    host2VmTraversal.setName("host2VmTraversal");

	    ObjectSpec oSpec = new ObjectSpec();
	    oSpec.setObj(morHost);
	    oSpec.setSkip(Boolean.TRUE);
	    oSpec.setSelectSet(new SelectionSpec[] { host2VmTraversal });

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.setPropSet(new PropertySpec[] { pSpec });
	    pfSpec.setObjectSet(new ObjectSpec[] { oSpec });
		      
	    ObjectContent[] ocs = cb.getServiceConnection3().getService().retrieveProperties(
            cb.getServiceConnection3().getServiceContent().getPropertyCollector(),
            new PropertyFilterSpec[] { pfSpec });
	    this.printContent(ocs);
	}
	
	private void testFT() throws Exception {
		
		ManagedObjectReference morVm = new ManagedObjectReference();
		morVm.setType("VirtualMachine");
		morVm.set_value("vm-480");
		
		ManagedObjectReference morHost = new ManagedObjectReference();
		morHost.setType("HostSystem");
		morHost.set_value("host-470");

		System.out.println("Create secondary VM");
		ManagedObjectReference morTask = cb.getServiceConnection3().getService().createSecondaryVM_Task(morVm, morHost);
		String result = cb.getServiceUtil3().waitForTask(morTask);
		
		System.out.println("Create secondary VM resutl : " + result);
	}
	
	private void testFTEnable() throws Exception {
		ManagedObjectReference morVm = new ManagedObjectReference();
		morVm.setType("VirtualMachine");
		morVm.set_value("vm-480");
		
		ManagedObjectReference morHost = new ManagedObjectReference();
		morHost.setType("HostSystem");
		morHost.set_value("host-470");

		ManagedObjectReference morSecondaryVm = new ManagedObjectReference();
		morSecondaryVm.setType("VirtualMachine");
		morSecondaryVm.set_value("vm-485");
		
		System.out.println("Enable FT");
		ManagedObjectReference morTask = cb.getServiceConnection3().getService().enableSecondaryVM_Task(morVm, 
			morSecondaryVm, morHost);
		String result = cb.getServiceUtil3().waitForTask(morTask);
		
		System.out.println("Enable FT resutl : " + result);
	}
    private DatacenterMO setupDatacenterObject(String serverAddress, String dcMor) {
        VmwareContext context = new VmwareContext(cb, serverAddress);
	
        ManagedObjectReference morDc = new ManagedObjectReference();
        morDc.setType("Datacenter");
        morDc.set_value(dcMor);

        return new DatacenterMO(context, morDc);
    }

    private DistributedVirtualSwitchMO setupDistributedVirtualSwitchObject(String dvsMor, String serverAddress) {
        VmwareContext context = new VmwareContext(cb, serverAddress);
        return new DistributedVirtualSwitchMO(context, setupDVS(dvsMor));
    }

    private ManagedObjectReference setupDVS(String dvsMor) {
        ManagedObjectReference morDvs = new ManagedObjectReference();
        morDvs.setType("VmwareDistributedVirtualSwitch");
        morDvs.set_value(dvsMor);
        return morDvs;
    }

    private void testDvSwitchOperations() throws Exception {
        String dvSwitchName, dcMor;
        ManagedObjectReference queriedDvs;
        ManagedObjectReference morDvs;
        DatacenterMO dcMo;
        URL serviceUrl;

        // Initialize mor for existing DVS
        if (_args.length <= IND_DVSWITCH_NAME) {
            System.out.println("Using default parameters as required command line arguments are not provided.");
            System.out.println("Sequence of arguments: <SERVERADDRESS> <USER> <PASSWORD> <DATACENTER> <DVSWITCH_MOR> <DVSWITCH_NAME>");
            morDvs = setupDVS("dvs-921");
            dvSwitchName = "dvSwitch0";
            dcMor = "datacenter-2";
        } else {
            morDvs = setupDVS(_args[IND_DVSWITCH_MOR]);
            dvSwitchName = _args[IND_DVSWITCH_NAME];
            dcMor = _args[IND_DATACENTER_MOR];
        }

        serviceUrl = new URL(cb.getServiceUrl());

        // Initialize Datacenter Object that pertains to above DVS
        dcMo = setupDatacenterObject(serviceUrl.getHost(), dcMor);

        // Query for DVS with name
        queriedDvs = dcMo.getDvSwitchMor(dvSwitchName);

        System.out.print("\nTest fetch dvSwitch object from vCenter : ");
        if (morDvs.equals(queriedDvs)) {
            System.out.println("Success\n");
        } else {
            System.out.println("Failed\n");
        }
    }

    private void testDvPortGroupOpearations() throws Exception {
        // addDvPortGroup, updateDvPortGroup, getDvPortGroup, hasDvPortGroup
        ManagedObjectReference morDvs, morDvPortGroup;
        DatacenterMO dcMo;
        DistributedVirtualSwitchMO dvsMo;
        int networkRateMbps;
        int networkRateMbpsToUpdate;
        DVSTrafficShapingPolicy shapingPolicy;
        VmwareDistributedVirtualSwitchVlanSpec vlanSpec;
        DVSSecurityPolicy secPolicy;
        VMwareDVSPortSetting dvsPortSetting;
        DVPortgroupConfigSpec dvPortGroupSpec;
        DVPortgroupConfigInfo dvPortgroupInfo = null;
        String dvPortGroupName, dcMor;
        Integer vid;
        int numPorts;
        int timeOutMs;
        URL serviceUrl;

        if (_args.length < MAX_ARGS) {
            System.out.println("Using default parameters as required command line arguments are not provided.");
            System.out.println("Sequence of arguments: <SERVERADDRESS> <USER> <PASSWORD> <DATACENTER> <DVSWITCH_MOR> <DVSWITCH_NAME> <DVPORTGROUP_NAME> <DVPORTGROUP_VLAN> <DVPORTGROUP_PORTCOUNT>");
            morDvs = setupDVS("dvs-921");
            dvPortGroupName = "cloud.public.201.dvSwitch0.1";
            networkRateMbps = 201;
            vid = new Integer(399); // VLAN 399
            timeOutMs = 7000;
            numPorts = 64;
            dcMor = "datacenter-2";
        } else {
            morDvs = setupDVS(_args[IND_DVSWITCH_MOR]);
            dvPortGroupName = _args[IND_DVPORTGROUP_NAME];
            vid = new Integer(IND_DVPORTGROUP_VLAN);
            dcMor = _args[IND_DATACENTER_MOR];
            numPorts = Integer.parseInt(_args[IND_DVPORTGROUP_PORTCOUNT]);
            timeOutMs = 7000;
            networkRateMbps = 201;
        }
        serviceUrl = new URL(cb.getServiceUrl());

        // Initialize Datacenter Object that pertains to above DVS
        dcMo = setupDatacenterObject(serviceUrl.getHost(), dcMor);
        // Create dvPortGroup configuration spec
        dvsMo = setupDistributedVirtualSwitchObject(morDvs.get_value(), serviceUrl.getHost());

        shapingPolicy = HypervisorHostHelper.getDVSShapingPolicy(networkRateMbps);
        secPolicy = HypervisorHostHelper.createDVSSecurityPolicy();
        if (vid != null) {
            vlanSpec = HypervisorHostHelper.createDVPortVlanIdSpec(vid);
        } else {
            vlanSpec = HypervisorHostHelper.createDVPortVlanSpec();
        }
        dvsPortSetting = HypervisorHostHelper.createVmwareDVPortSettingSpec(shapingPolicy, secPolicy, vlanSpec);
        dvPortGroupSpec = HypervisorHostHelper.createDvPortGroupSpec(dvPortGroupName, dvsPortSetting, numPorts);
        if (!dcMo.hasDvPortGroup(dvPortGroupName)) {
            System.out.print("\nTest create dvPortGroup : ");
            try {
                // Call method to create dvPortGroup
                dvsMo.createDVPortGroup(dvPortGroupSpec);
                System.out.println("Success\n");
                HypervisorHostHelper.waitForDvPortGroupReady(dcMo, dvPortGroupName, timeOutMs);
            } catch (Exception e) {
                System.out.println("Failed\n");
                throw new Exception(e);
            }
        }

        // Test for presence of dvPortGroup
        System.out.print("\nTest presence of dvPortGroup : ");
        if (dcMo.hasDvPortGroup(dvPortGroupName)) {
            System.out.println("Success\n");
        } else {
            System.out.println("Failed\n");
        }

        // Test get existing dvPortGroup
        System.out.print("\nTest fetch dvPortGroup configuration : ");
        try {
            dvPortgroupInfo = dcMo.getDvPortGroupSpec(dvPortGroupName);
            if (dvPortgroupInfo != null)
                System.out.println("Success\n");
        } catch (Exception e) {
            System.out.println("Failed\n");
        }
        // Test compare dvPortGroup configuration
        System.out.print("\nTest compare dvPortGroup configuration : ");

        if (HypervisorHostHelper.isSpecMatch(dvPortgroupInfo, vid, shapingPolicy)) {
            System.out.println("Success\n");
            // We haven't modified the dvPortGroup after creating above.
            // Hence expecting to be matching.
            // NOTE : Hopefully nothing changes the configuration externally.
        } else {
            System.out.println("Failed\n");
        }

        // Test update dvPortGroup configuration
        networkRateMbpsToUpdate = 210;
        shapingPolicy = HypervisorHostHelper.getDVSShapingPolicy(networkRateMbpsToUpdate);
        dvsPortSetting = HypervisorHostHelper.createVmwareDVPortSettingSpec(shapingPolicy, secPolicy, vlanSpec);
        dvPortGroupSpec.setDefaultPortConfig(dvsPortSetting);
        dvPortGroupSpec.setConfigVersion(dvPortgroupInfo.getConfigVersion());
        morDvPortGroup = dcMo.getDvPortGroupMor(dvPortGroupName);
        System.out.print("\nTest update dvPortGroup configuration : ");
        if (!HypervisorHostHelper.isSpecMatch(dvPortgroupInfo, vid, shapingPolicy)) {
            try {
                dvsMo.updateDvPortGroup(morDvPortGroup, dvPortGroupSpec);
                System.out.println("Success\n");
            } catch (Exception e) {
                System.out.println("Failed\n");
                throw new Exception(e);
            }
        }
    }
	private void importOVF() throws Exception {
		ManagedObjectReference morHost = new ManagedObjectReference();
		morHost.setType("HostSystem");
		morHost.set_value("host-223");
		
		ManagedObjectReference morRp = new ManagedObjectReference();
		morRp.setType("ResourcePool");
		morRp.set_value("resgroup-222");
		
		ManagedObjectReference morDs = new ManagedObjectReference();
		morDs.setType("Datastore");
		morDs.set_value("datastore-30");
		
		ManagedObjectReference morVmFolder = new ManagedObjectReference();
		morVmFolder.setType("Folder");
		morVmFolder.set_value("group-v3");
		
		ManagedObjectReference morNetwork = new ManagedObjectReference();
		morNetwork.setType("Network");
		morNetwork.set_value("network-32");
		
		ManagedObjectReference morOvf = cb.getServiceConnection3().getServiceContent().getOvfManager();
		
		OvfCreateImportSpecParams importSpecParams = new OvfCreateImportSpecParams();  
		importSpecParams.setHostSystem(morHost);  
		importSpecParams.setLocale("US");  
		importSpecParams.setEntityName("winxpsp3-ovf-deployed");  
		importSpecParams.setDeploymentOption("");
		importSpecParams.setDiskProvisioning("thin");

/*		
		OvfNetworkMapping networkMapping = new OvfNetworkMapping();  
		networkMapping.setName("VM Network");  
		networkMapping.setNetwork(morNetwork); // network);  
		importSpecParams.setNetworkMapping(new OvfNetworkMapping[] { networkMapping });
*/		
		importSpecParams.setPropertyMapping(null);
		
		String ovfDescriptor = readOvfContent("C:\\research\\vmware\\winxpsp3-ovf\\winxpsp3-ovf.ovf");
		OvfCreateImportSpecResult ovfImportResult = cb.getServiceConnection3().getService().createImportSpec(
			morOvf, ovfDescriptor, morRp, morDs, importSpecParams);
		
		if(ovfImportResult != null) {
			long totalBytes = addTotalBytes(ovfImportResult);
			
			ManagedObjectReference morLease = cb.getServiceConnection3().getService().importVApp(morRp, 
				ovfImportResult.getImportSpec(), morVmFolder, morHost);
			
			HttpNfcLeaseState state;
			for(;;) {
				state = (HttpNfcLeaseState)cb.getServiceUtil3().getDynamicProperty(morLease, "state");
				if(state == HttpNfcLeaseState.ready || state == HttpNfcLeaseState.error)
					break;
			}
			
			if(state == HttpNfcLeaseState.ready) {
				HttpNfcLeaseInfo httpNfcLeaseInfo = (HttpNfcLeaseInfo)cb.getServiceUtil3().getDynamicProperty(morLease, "info");
		        HttpNfcLeaseDeviceUrl[] deviceUrls = httpNfcLeaseInfo.getDeviceUrl();  
		        long bytesAlreadyWritten = 0;  
		        for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrls) {
		        	
		        	String deviceKey = deviceUrl.getImportKey();  
		        	for (OvfFileItem ovfFileItem : ovfImportResult.getFileItem()) {  
		        		if (deviceKey.equals(ovfFileItem.getDeviceId())) {  
		        			System.out.println("Import key==OvfFileItem device id: " + deviceKey);
		        			System.out.println("device URL: " + deviceUrl.getUrl());
		        			
		        			String absoluteFile = "C:\\research\\vmware\\winxpsp3-ovf\\" + ovfFileItem.getPath();
		        			String urlToPost = deviceUrl.getUrl().replace("*", "esxhost-1.lab.vmops.com");  
		        			  	
	        			  	uploadVmdkFile(ovfFileItem.isCreate(), absoluteFile, urlToPost, bytesAlreadyWritten, totalBytes);  
	        			  	bytesAlreadyWritten += ovfFileItem.getSize();  
	        			  	System.out.println("Completed uploading the VMDK file:" + absoluteFile);  
	        			 }  
		        	 }  
		         }
		         cb.getServiceConnection3().getService().httpNfcLeaseProgress(morLease, 100);
		         cb.getServiceConnection3().getService().httpNfcLeaseComplete(morLease);
			}
		}
	}
	
	 private static void uploadVmdkFile(boolean put, String diskFilePath, String urlStr, long bytesAlreadyWritten, long totalBytes) throws IOException {  
		 HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {  
			 public boolean verify(String urlHostName, SSLSession session) {  
				 return true;  
			 }  
		 });  

		 HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr).openConnection();
		 
		 conn.setDoOutput(true);  
		 conn.setUseCaches(false);
		 
		 int CHUCK_LEN = 64*1024;
		 conn.setChunkedStreamingMode(CHUCK_LEN);  
		 conn.setRequestMethod(put? "PUT" : "POST"); // Use a post method to write the file.  
		 conn.setRequestProperty("Connection", "Keep-Alive");  
		 conn.setRequestProperty("Content-Type", "application/x-vnd.vmware-streamVmdk");  
		 conn.setRequestProperty("Content-Length", Long.toString(new File(diskFilePath).length()));  
		 BufferedOutputStream bos = new BufferedOutputStream(conn.getOutputStream());  
		 BufferedInputStream diskis = new BufferedInputStream(new FileInputStream(diskFilePath));  
		 int bytesAvailable = diskis.available();  
		 int bufferSize = Math.min(bytesAvailable, CHUCK_LEN);  
		 byte[] buffer = new byte[bufferSize];  
		 long totalBytesWritten = 0;  
		 while (true) {  
			 int bytesRead = diskis.read(buffer, 0, bufferSize);  
			 if (bytesRead == -1)  
			 {  
				 System.out.println("Total bytes written: " + totalBytesWritten);  
				 break;  
			 }  
			 totalBytesWritten += bytesRead;  
			 bos.write(buffer, 0, bufferSize);  
			 bos.flush();  
			 System.out.println("Total bytes written: " + totalBytesWritten);
			 
/*			 
			int progressPercent = (int) (((bytesAlreadyWritten + totalBytesWritten) * 100) / totalBytes);  
			 	leaseUpdater.setPercent(progressPercent);  
*/			  
		}
	 	diskis.close();  
	 	bos.flush();  
	 	bos.close();  
	 	conn.disconnect();  
	}
	
	public static long addTotalBytes(OvfCreateImportSpecResult ovfImportResult) {  
		OvfFileItem[] fileItemArr = ovfImportResult.getFileItem();  
		long totalBytes = 0;  
		if (fileItemArr != null) {  
			for (OvfFileItem fi : fileItemArr) {  
				printOvfFileItem(fi);  
				totalBytes += fi.getSize();  
			}  
		}  
		return totalBytes;  
	}
	
	private static void printOvfFileItem(OvfFileItem fi) {  
		System.out.println("================ OvfFileItem ================");  
		System.out.println("chunkSize: " + fi.getChunkSize());  
		System.out.println("create: " + fi.isCreate());  
		System.out.println("deviceId: " + fi.getDeviceId());  
		System.out.println("path: " + fi.getPath());  
		System.out.println("size: " + fi.getSize());  
		System.out.println("==============================================");  
	}  
	
	public static String readOvfContent(String ovfFilePath) throws IOException	 {  
		StringBuffer strContent = new StringBuffer();  
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(ovfFilePath)));  
		String lineStr;  
		while ((lineStr = in.readLine()) != null) {  
			strContent.append(lineStr);  
		}  

		in.close();  
		return strContent.toString();  
	}
	
	public static String escapeSpecialChars(String str)	{  
		str = str.replaceAll("<", "&lt;");  
		return str.replaceAll(">", "&gt;"); // do not escape "&" -> "&amp;", "\"" -> "&quot;"  
	}  
	
	public static void main(String[] args) throws Exception {
		setupLog4j();
		TestVMWare client = new TestVMWare();
	   
		// skip certificate check
		System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
		 
		String serviceUrl = "https://" + args[0] + "/sdk/vimService";
		
		try {
			String[] params = new String[] {"--url", serviceUrl, "--username", args[1], "--password", args[2] };
		 
			cb = ExtendedAppUtil.initialize("Connect", params);
			cb.connect();
			System.out.println("Connection Succesful.");
            _args = args;

			// client.listInventoryFolders();
			// client.listDataCenters();
			// client.powerOnVm();
			// client.createSnapshot();
			// client.registerTemplate();
			// client.createVmFromTemplate();
			// client.addNic();
			// client.addNicToNetwork();

			// client.createDatacenter();
			// client.getPropertyWithPath();
			// client.getHostVMs();
			// client.testFT();
			// client.testFTEnable();
			
            // client.importOVF();

            // Test get DvSwitch
            client.testDvSwitchOperations();
            // Test add DvPortGroup,
            // Test update vPortGroup,
            // Test get DvPortGroup,
            // Test compare DvPortGroup
            client.testDvPortGroupOpearations();

            // Test addDvNic
            // Test deleteDvNic
            // client.testDvNicOperations();
		
			cb.disConnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static class TrustAllManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {
		
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}
		
		public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
			return true;
		}
		
		public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
			return true;
		}
		
		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
	      	throws java.security.cert.CertificateException {
			return;
		} 
		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
	      	throws java.security.cert.CertificateException {
			return;
		}
	}
}

