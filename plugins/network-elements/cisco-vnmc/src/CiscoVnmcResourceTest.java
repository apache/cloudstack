import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloud.network.resource.CiscoVnmcResource;
import com.cloud.utils.exception.ExecutionException;


public class CiscoVnmcResourceTest {
	static CiscoVnmcResource resource;
	@BeforeClass
	public static void setUpClass() throws Exception {
		resource = new CiscoVnmcResource("10.223.56.5", "admin", "C1sco123");
		try {
			boolean response = resource.login();
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testLogin() {
		//fail("Not yet implemented");
		try {
			boolean response = resource.login();
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testCreateTenant() {
		//fail("Not yet implemented");
		try {
			boolean response = resource.createTenant("TenantA");
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCreateTenantVDC() {
		//fail("Not yet implemented");
		try {
			boolean response = resource.createTenantVDC("TenantA");
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testCreateTenantVDCEdgeDeviceProfile() {
		//fail("Not yet implemented");
		try {
			boolean response = resource.createTenantVDCEdgeDeviceProfile("TenantA");
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCreateTenantVDCEdgeDeviceRoutePolicy() {
		try {
			boolean response = resource.createTenantVDCEdgeStaticRoutePolicy("TenantA");
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCreateTenantVDCEdgeDeviceRoute() {
		try {
			boolean response = resource.createTenantVDCEdgeStaticRoute("TenantA", 
					"10.223.136.1", "Edge_Outside", "0.0.0.0", "0.0.0.0");
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
