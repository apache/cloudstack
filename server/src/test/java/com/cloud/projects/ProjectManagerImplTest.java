package com.cloud.projects;
import com.cloud.exception.ResourceAllocationException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.junit.Assert;
import org.junit.Test;


@RunWith(MockitoJUnitRunner.class)
public class ProjectManagerImplTest {

    @Spy
    @InjectMocks
    private ProjectManagerImpl projectManagerImpl = new ProjectManagerImpl();

    private static long projectId = 1L;

    private static final String name  = "newProject";

    private static final String accountName = "account";

    @Test
    public void testUpdateProjectName() throws ResourceAllocationException {
 //       UpdateProjectCmd cmd = Mockito.mock(UpdateProjectCmd.class);
 //       when(cmd.getName()).thenReturn("testName");

        Project project  = projectManagerImpl.updateProject(projectId, name, null, accountName);
        Assert.assertEquals("newProject", project.getName());

    }

}