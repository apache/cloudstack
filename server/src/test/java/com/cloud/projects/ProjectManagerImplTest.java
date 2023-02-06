package com.cloud.projects;
import com.cloud.exception.ResourceAllocationException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.junit.Assert;
import org.junit.Test;


@RunWith(MockitoJUnitRunner.class)
public class ProjectManagerImplTest {

    @Spy
    @InjectMocks
    private ProjectManagerImpl projectManagerImpl = new ProjectManagerImpl();

    @Mock
    private Project project;

    private static long projectId = 1L;

    private static final String name  = "newProject";

    private static final String accountName = "admin";

    @Test
    public void testUpdateProjectName() throws ResourceAllocationException {

        Project project  = projectManagerImpl.updateProject(projectId, name, "hello", accountName);
        Assert.assertEquals("newProject", project.getName());

    }

}
