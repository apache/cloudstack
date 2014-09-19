package com.cloud.hypervisor.ovm3.object;

import org.junit.Test;
// import org.junit.Test;

public class RepositoryTest {
    ConnectionTest con = new ConnectionTest();
    Repository repo = new Repository(con);
    XmlTestResultTest results = new XmlTestResultTest();
    private String REPOID = "f12842ebf5ed3fe78da1eb0e17f5ede8";
    private String MGRID = "d1a749d4295041fb99854f52ea4dea97";
    private String REPODISCOVERXML = "<string>"
            + "&lt;?xml version=\"1.0\" ?&gt;"
            + "&lt;Discover_Repositories_Result&gt;"
            + "&lt;RepositoryList&gt;"
            + "&lt;Repository Name=\""+ REPOID + "\"&gt;"
            + "&lt;Version&gt;3.0&lt;/Version&gt;"
            + "&lt;Manager_UUID&gt;" + MGRID +"&lt;/Manager_UUID&gt;"
            + "&lt;Repository_UUID&gt;" + REPOID + "&lt;/Repository_UUID&gt;"
            + "&lt;Repository_Alias&gt;My Comment&lt;/Repository_Alias&gt;"
            + "&lt;Assemblies/&gt;"
            + "&lt;Templates/&gt;"
            + "&lt;VirtualMachines/&gt;"
            + "&lt;VirtualDisks/&gt;"
            + "&lt;ISOs&gt;"
            + "&lt;ISO&gt;e906ec779ab43c6cbfdf30db5cbb3f1c.iso&lt;/ISO&gt;"
            + "&lt;/ISOs&gt;"
            + "&lt;/Repository&gt;"
            + "&lt;/RepositoryList&gt;"
            + "&lt;/Discover_Repositories_Result&gt;"
            + "</string>";
    private String REPODBDISCOVERXML = "<string>"
            + "&lt;?xml version=\"1.0\" ?&gt;"
            + "&lt;Discover_Repositories_Result&gt;"
            + "&lt;RepositoryList&gt;"
            + "&lt;Repository Name=\""+ REPOID + "\"&gt;"
            + "&lt;Version&gt;3.0&lt;/Version&gt;"
            + "&lt;Manager_UUID&gt;" + MGRID +"&lt;/Manager_UUID&gt;"
            + "&lt;Repository_UUID&gt;" + REPOID + "&lt;/Repository_UUID&gt;"
            + "&lt;Repository_Alias&gt;OVS Repository&lt;/Repository_Alias&gt;"
            + "&lt;Assemblies/&gt;"
            + "&lt;Templates&gt;"
            + "&lt;Template Name=\"711b6b1f-dcc4-4e6d-a4a5-592d0543722a\"&gt;"
            + "&lt;File&gt;711b6b1f-dcc4-4e6d-a4a5-592d0543722a.raw&lt;/File&gt;"
            + "&lt;/Template&gt;"
            + "&lt;/Templates&gt;"
            + "&lt;VirtualMachines&gt;"
            + "&lt;VirtualMachine Name=\"34834822-3bce-34c7-8158-ef7816627fe5\"&gt;"
            + "&lt;File&gt;vm.cfg&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;VirtualMachine Name=\"cs-poolfs\"&gt;"
            + "&lt;File&gt;ovspoolfs.img&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;VirtualMachine Name=\"fef78931-5f53-3060-b72e-b392b80b1aa8\"&gt;"
            + "&lt;File&gt;vm.cfg&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;VirtualMachine Name=\"0f2cc613-5d91-3096-82a4-feeb1ec72697\"&gt;"
            + "&lt;File&gt;vm.cfg&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;VirtualMachine Name=\"14fc3846-45e5-3c08-ad23-432ceb07407b\"&gt;"
            + "&lt;File&gt;vm.cfg&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;VirtualMachine Name=\"91d56bcb-9d3f-3996-9376-49ace39b2e03\"&gt;"
            + "&lt;File&gt;vm.cfg&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;/VirtualMachines&gt;"
            + "&lt;VirtualDisks&gt;"
            + "&lt;Disk&gt;2936a3b6-827b-46b2-a287-12741562f7ff.raw&lt;/Disk&gt;"
            + "&lt;Disk&gt;84edd810-5450-4f61-bab3-892f42f2734f.raw&lt;/Disk&gt;"
            + "&lt;/VirtualDisks&gt;"
            + "&lt;ISOs&gt;"
            + "&lt;ISO&gt;systemvm-4.5.0-SNAPSHOT.iso&lt;/ISO&gt;"
            + "&lt;/ISOs&gt;"
            + "&lt;/Repository&gt;"
            + "&lt;/RepositoryList&gt;"
            + "&lt;/Discover_Repositories_Result&gt;"
            + "</string";

    @Test
    public void test() throws Ovm3ResourceException {
        con.setResult(this.REPODBDISCOVERXML);
        repo.discoverRepo("");
    }
}
