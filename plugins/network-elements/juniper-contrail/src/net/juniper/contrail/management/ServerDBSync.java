package net.juniper.contrail.management;

import java.io.IOException;

import com.cloud.domain.DomainVO;
import com.cloud.projects.ProjectVO;

public interface ServerDBSync {


    public final static short SYNC_STATE_IN_SYNC = 0;
    public final static short SYNC_STATE_OUT_OF_SYNC = 1;
    public final static short SYNC_STATE_UNKNOWN = -1;
    /*
     * API for syncing all classes of vnc objects with cloudstack
     * Sync cloudstack and vnc objects.
     */
    public short syncAll(short syncMode);
    public void syncClass(Class<?> cls);
    public void createProject(ProjectVO project, StringBuffer syncLogMesg) throws IOException;
    public void createDomain(DomainVO domain, StringBuffer logMesg)throws IOException;
}
