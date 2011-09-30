package com.cloud.projects;

import java.util.List;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.projects.ProjectAccount.Role;
import com.cloud.user.Account;


public interface ProjectService {
    /**
     * Creates a new project
     * 
     * @param name - project name
     * @param displayText - project display text
     * @param accountName - account name of the project owner
     * @param domainId - domainid of the project owner
     * @return the project if created successfully, null otherwise
     * @throws ResourceAllocationException 
     */
    Project createProject(String name, String displayText, String accountName, Long domainId) throws ResourceAllocationException;
    
    /**
     * Deletes a project
     * 
     * @param id - project id
     * @return true if the project was deleted successfully, false otherwise
     */
    boolean deleteProject(long id);
    
    /**
     * Gets a project by id
     * 
     * @param id - project id
     * @return project object
     */
    Project getProject(long id);
    
    List<? extends Project> listProjects(Long id, String name, String displayText, String accountName, Long domainId, String keyword, Long startIndex, Long pageSize);

    ProjectAccount assignAccountToProject(Project project, long accountId, Role accountRole);
    
    Account getProjectOwner(long projectId);

    boolean unassignAccountFromProject(long projectId, long accountId);
    
    Project findByProjectAccountId(long projectAccountId);
    
    Project findByNameAndDomainId(String name, long domainId);
    
    Project updateProject(long id, String displayText, String newOwnerName);
    
    boolean addAccountToProject(long projectId, String accountName);

    boolean deleteAccountFromProject(long projectId, String accountName);
    
    List<? extends ProjectAccount> listProjectAccounts(long projectId, String accountName, String role, Long startIndex, Long pageSizeVal);
    
    List<? extends ProjectInvitation> listProjectInvitations(Long projectId, String accountName, Long domainId, String state, boolean activeOnly, Long startIndex, Long pageSizeVal);
    
    boolean joinProject(long projectId, String accountName);
}
