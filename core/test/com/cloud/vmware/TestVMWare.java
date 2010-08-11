package com.cloud.vmware;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.utils.PropertiesUtil;
import com.vmware.apputils.AppUtil;
import com.vmware.vim.ArrayOfManagedObjectReference;
import com.vmware.vim.DatastoreInfo;
import com.vmware.vim.DynamicProperty;
import com.vmware.vim.InvalidProperty;
import com.vmware.vim.ManagedObjectReference;
import com.vmware.vim.ObjectContent;
import com.vmware.vim.ObjectSpec;
import com.vmware.vim.PropertyFilterSpec;
import com.vmware.vim.PropertySpec;
import com.vmware.vim.RuntimeFault;
import com.vmware.vim.SelectionSpec;
import com.vmware.vim.TraversalSpec;

public class TestVMWare {
	private static AppUtil cb;
	
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
      spec.getObjectSet(0).setObj(cb.getConnection().getRootFolder());
      spec.getObjectSet(0).setSkip(new Boolean(false));
      spec.getObjectSet(0).setSelectSet(
      new SelectionSpec[] { folderTraversalSpec });      

      // Recursively get all ManagedEntity ManagedObjectReferences 
      // and the "name" property for all ManagedEntities retrieved
      ObjectContent[] ocary = 
        cb.getConnection().getService().retrieveProperties(
        cb.getConnection().getServiceContent().getPropertyCollector(), 
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
	    filterSpec.getObjectSet(0).setObj(cb.getConnection().getRootFolder());
	    filterSpec.getObjectSet(0).setSkip(new Boolean(false));
	    filterSpec.getObjectSet(0).setSelectSet(
    		new SelectionSpec[] { folderTraversalSpec }
    	);      
	    
	    try {
			ObjectContent[] objContent = cb.getConnection().getService().retrieveProperties(
				cb.getConnection().getServiceContent().getPropertyCollector(), 
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
	    oSpec.setObj(cb.getConnection().getRootFolder());
	    oSpec.setSkip(Boolean.TRUE);
	    oSpec.setSelectSet(new SelectionSpec[] { getFolderRecursiveTraversalSpec() });

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.setPropSet(new PropertySpec[] { pSpec });
	    pfSpec.setObjectSet(new ObjectSpec[] { oSpec });
	      
	    ObjectContent[] ocs = cb.getConnection().getService().retrieveProperties(
            cb.getConnection().getServiceContent().getPropertyCollector(),
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
	      
	    ObjectContent[] ocs = cb.getConnection().getService().retrieveProperties(
            cb.getConnection().getServiceContent().getPropertyCollector(),
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
	      
	    ObjectContent[] ocs = cb.getConnection().getService().retrieveProperties(
            cb.getConnection().getServiceContent().getPropertyCollector(),
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
	      
	    ObjectContent[] ocs = cb.getConnection().getService().retrieveProperties(
            cb.getConnection().getServiceContent().getPropertyCollector(),
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
	    
	    ObjectContent[] ocs = cb.getConnection().getService().retrieveProperties(
            cb.getConnection().getServiceContent().getPropertyCollector(),
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
	    oSpec.setObj(cb.getConnection().getRootFolder());
	    oSpec.setSkip(Boolean.TRUE);
	    oSpec.setSelectSet(new SelectionSpec[] { folder2childEntity });

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.setPropSet(new PropertySpec[] { pSpec });
	    pfSpec.setObjectSet(new ObjectSpec[] { oSpec });
	      
	    return cb.getConnection().getService().retrieveProperties(
            cb.getConnection().getServiceContent().getPropertyCollector(),
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
		
		ObjectContent[] ocs = cb.getConnection().getService().retrieveProperties(
	         cb.getConnection().getServiceContent().getPropertyCollector(),
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
		ObjectContent[] ocs = cb.getConnection().getService().retrieveProperties(
	         cb.getConnection().getServiceContent().getPropertyCollector(),
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
		morVm.set_value("vm-66");
		
		cb.getConnection().getService().powerOnVM_Task(morVm, null);
	}
	
	private void powerOffVm() throws Exception {
		ManagedObjectReference morVm = new ManagedObjectReference();
		morVm.setType("VirtualMachine");
		morVm.set_value("vm-66");
		
		cb.getConnection().getService().powerOffVM_Task(morVm);
	}
	
	private void testCustomField() throws Exception {
		// TODO
	}
	
	public static void main(String[] args) throws Exception {
		setupLog4j();
		TestVMWare client = new TestVMWare();
	   
		// skip certificate check
		System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
		 
		String serviceUrl = "https://vsphere-1.lab.vmops.com/sdk/vimService";
		try {
			String[] params = new String[] {"--url", serviceUrl, "--username", "Administrator", "--password", "Suite219" };
		 
			cb = AppUtil.initialize("Connect", params);
			cb.connect();
			System.out.println("Connection Succesful.");

			// client.listInventoryFolders();
			// client.listDataCenters();
			client.powerOnVm();
			
			cb.disConnect();
		} catch (Exception e) {
			System.out.println("Failed to connect to " + serviceUrl);
		}
	}
}
