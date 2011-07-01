package com.cloud.projects;

import java.util.List;


public interface ProjectService {
    /**
     * Creates a new project
     * 
     * @param name - project name
     * @param displayText - project display text
     * @param zoneId - id of the zone the project belongs to
     * @param accountName - account name of the project owner
     * @param domainId - domainid of the project owner
     * @return the project if created successfully, null otherwise
     */
    Project createProject(String name, String displayText, long zoneId, String accountName, Long domainId);
    
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
    
    List<? extends Project> listProjects(Long id, String name, String displayText, Long zoneId, String accountName, Long domainId, String keyword, Long startIndex, Long pageSize);
}
