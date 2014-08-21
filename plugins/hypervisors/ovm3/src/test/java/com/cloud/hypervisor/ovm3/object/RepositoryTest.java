package com.cloud.hypervisor.ovm3.object;
// import org.junit.Test;

public class RepositoryTest {
    ConnectionTest con = new ConnectionTest();
    Repository repo = new Repository(con);
    XmlTestResultTest results = new XmlTestResultTest();
    private String REPOID = "";
    private String MGRID = "";
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
            + "</string";
    private String REPODBDISCOVERXML = "<string>"
            + "&lt;?xml version=\"1.0\" ?&gt;"
            + "&lt;Discover_Repository_Db_Result&gt;"
            + "&lt;RepositoryDbList&gt;"
            + "&lt;Repository Uuid=\""+ REPOID +"\"&gt;"
            + "&lt;Fs_location&gt;cs-mgmt:/volumes/cs-data/primary/ovm&lt;/Fs_location&gt;"
            + "&lt;Mount_point&gt;/OVS/Repositories/" + REPOID + "&lt;/Mount_point&gt;"
            + "&lt;Filesystem_type&gt;nfs&lt;/Filesystem_type&gt;"
            + "&lt;Version&gt;3.0&lt;/Version&gt;"
            + "&lt;Alias&gt;My Comment&lt;/Alias&gt;"
            + "&lt;Manager_uuid&gt;" + MGRID + "&lt;/Manager_uuid&gt;"
            + "&lt;Status&gt;Mounted&lt;/Status&gt;"
            + "&lt;/Repository&gt;"
            + "&lt;/RepositoryDbList&gt;"
            + "&lt;/Discover_Repository_Db_Result&gt;"
            + "</string";

}
