/*
 * Copyright (c) 2008 Citrix Systems, Inc.
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

import java.util.HashMap;
import java.util.Set;

import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VDI;

/**
 * Performs various SR and VDI tests, including creating a dummy SR.
 */
public class VdiAndSrOps extends TestBase
{
    public static void RunTest(ILog logger, TargetServer server) throws Exception
    {
        TestBase.logger = logger;

        try
        {
            connect(server);

            APIVersion version = connection.getAPIVersion();
            boolean rio = version == APIVersion.API_1_1;

            for (boolean nullSmConfig : new boolean[] { true, false })
            {
                testSrOp("SR.create", false, nullSmConfig, version);
                testSrOp("SR.forget", false, nullSmConfig, version);
                testSrOp("SR.createAsync", false, nullSmConfig, version);
                testSrOp("SR.forget", false, nullSmConfig, version);
                testSrOp("SR.makeAsync", false, nullSmConfig, version);
                testSrOp("SR.forget", false, nullSmConfig, version);
                if (rio)
                {
                    testSrOp("SR.make", true, nullSmConfig, version);
                    testSrOp("SR.forget", false, nullSmConfig, version);
                }
                testSrOp("SR.introduce", false, nullSmConfig, version);
                testSrOp("SR.forget", false, nullSmConfig, version);
                testSrOp("SR.introduceAsync", false, nullSmConfig, version);
                testSrOp("SR.forget", false, nullSmConfig, version);
            }
            testVdiOp("VDI.snapshot", version);
            testVdiOp("VDI.createClone", version);
            testVdiOp("VDI.snapshotAsync", version);
            testVdiOp("VDI.createCloneAsync", version);
        } finally
        {
            disconnect();
        }
    }

    private static void testVdiOp(String op, APIVersion version) throws Exception
    {
        /* VDI.create and its allied functions got an extra parameter 'driverParams' between rio and miami.
         * However it's not commonly used. With the extra parameter void of data, the call should work on
         * either machine (the bindings should silently drop the extra bit and make the call anyway). But
         * if we provide a non-null last parameter, and attempt to make the call on Rio, we should get
         * an exception. */
        boolean rio = version == APIVersion.API_1_1;
        logf("With %s host, ", rio ? "Rio" : "post-Rio");

        log("--attempting " + op + " with null driverParams");
        vdiOpWithNullDriverParams(connection, op);

        log("--attempting " + op + " with non-null driverParams");
        if (rio)
        {
            log("(should fail)");
        }
        try
        {
            vdiOpWithNonNullDriverParams(connection, op);
            if (rio)
            {
                throw new Exception(op
                        + " call with non-null driverParams on rio host succeeded. It is not supposed to!");
            }
        } catch (Types.XenAPIException ex)
        {
            if (rio)
            {
                logln("As intended, " + op + " with non-null driverParams on rio host threw an exception");
                logf("\"%s\"\n", ex.getClass());
                logf("\"%s\"\n", ex);
            } else
            {
                throw ex;
            }
        }
    }

    private static void testSrOp(String op, boolean deprecated_response_ok, boolean nullSmConfig, APIVersion version)
            throws Exception
    {
        /* SR.create and its allied functions got an extra parameter 'SMConfig' between rio and miami.
         * However it's not commonly used. With the extra parameter void of data, the call should work on
         * either machine (the bindings should silently drop the extra bit and make the call anyway). But
         * if we provide a non-null last parameter, and attempt to make the call on Rio, we should get
         * an exception. */
        boolean rio = version == APIVersion.API_1_1;
        logf("With %s host, ", rio ? "Rio" : "post-Rio");

        try
        {
            if (nullSmConfig)
            {
                log("--attempting " + op + " with null smConfig... ");
                srOpWithNullSmConfig(connection, op);
                logln("success");
            } else
            {
                log("--attempting " + op + " with non-null smConfig... ");
                if (rio)
                {
                    log("(should fail)... ");
                }
                try
                {
                    srOpWithNonNullSmConfig(connection, op);
                    if (rio)
                    {
                        throw new Exception(op + " call with non-null smConfig host succeeded. It is not supposed to!");
                    }
                    logln("success");
                } catch (Types.XenAPIException ex)
                {
                    if (rio)
                    {
                        logln("As intended, " + op + " with non-null smConfig on rio host threw an exception");
                        logf("\"%s\"\n", ex.getClass());
                        logf("\"%s\"\n", ex);
                    } else
                    {
                        throw ex;
                    }
                }
            }
        } catch (Types.XenAPIException ex)
        {
            // Bug (CA-23404) workaround: sometimes the server doesn't send a proper MESSAGE_DEPRECATED,
            // so the bindings can't parse it properly. Perform manual check...
            if (errorDescriptionIs(ex.errorDescription, "MESSAGE_DEPRECATED"))
            {
                if (deprecated_response_ok)
                {
                    logln(op + " threw MessageDeprecated (but this is OK)");
                } else
                {
                    throw ex;
                }
            }
        }
    }

    private static void srOpWithNonNullSmConfig(Connection c, String op) throws Exception
    {
        HashMap<String, String> smConfig = new HashMap<String, String>();
        smConfig.put("testKey", "testValue");
        srOpLong(c, smConfig, op);
    }

    private static void srOpWithNullSmConfig(Connection c, String op) throws Exception
    {
        HashMap<String, String> smConfig = new HashMap<String, String>();
        srOpLong(c, smConfig, op);
    }

    private static void vdiOpWithNonNullDriverParams(Connection c, String op) throws Exception
    {
        HashMap<String, String> smConfig = new HashMap<String, String>();
        smConfig.put("testKey", "testValue");
        vdiOpLong(c, smConfig, op);
    }

    private static void vdiOpWithNullDriverParams(Connection c, String op) throws Exception
    {
        HashMap<String, String> smConfig = new HashMap<String, String>();
        vdiOpLong(c, smConfig, op);
    }

    private static Boolean errorDescriptionIs(String[] errDesc, String firstElement)
    {
        return errDesc != null && errDesc.length > 0 && errDesc[0].compareTo(firstElement) == 0;
    }

    private static final String FAKE_VDI_NAME = "madeupvdi";

    private static void vdiOpLong(Connection c, HashMap<String, String> driverParams, String op) throws Exception
    {
        try
        {
            VDI dummy = Types.toVDI(FAKE_VDI_NAME);
            if (op.equals("VDI.snapshot"))
            {
                dummy.snapshot(c, driverParams);
            } else if (op.equals("VDI.createClone"))
            {
                dummy.createClone(c, driverParams);
            } else if (op.equals("VDI.snapshotAsync"))
            {
                dummy.snapshotAsync(c, driverParams);
            } else if (op.equals("VDI.createCloneAsync"))
            {
                dummy.createCloneAsync(c, driverParams);
            } else
            {
                throw new Exception("bad op");
            }
        } catch (Types.HandleInvalid ex)
        {
            logln("Expected error: HANDLE_INVALID. that's ok.");
            /* We're happy with this, since it means that the call made it through to xen
             * and an attempt was made to execute it. */
        }
    }

    private static final String TEST_SR_NAME = "TestSR: DO NOT USE (created by VdiAndSrOps.java)";
    private static final String TEST_SR_DESC = "Should be automatically deleted";
    private static final String TEST_SR_TYPE = "dummy";
    private static final String TEST_SR_CONTENT = "contenttype";
    private static final long TEST_SR_SIZE = 100000L;

    private static void srOpLong(Connection c, HashMap<String, String> smConfig, String op) throws Exception
    {
        try
        {
            Host our_host = (Host) Host.getAll(c).toArray()[0];
            if (op.equals("SR.create"))
            {
                SR.create(c, our_host, new HashMap<String, String>(), TEST_SR_SIZE, TEST_SR_NAME, TEST_SR_DESC,
                        TEST_SR_TYPE, TEST_SR_CONTENT, true, smConfig);
            } else if (op.equals("SR.createAsync"))
            {
                SR.createAsync(c, our_host, new HashMap<String, String>(), TEST_SR_SIZE, TEST_SR_NAME, TEST_SR_DESC,
                        TEST_SR_TYPE, TEST_SR_CONTENT, true, smConfig);
            } else if (op.equals("SR.make"))
            {
                SR.make(c, our_host, new HashMap<String, String>(), TEST_SR_SIZE, TEST_SR_NAME, TEST_SR_DESC,
                        TEST_SR_TYPE, TEST_SR_CONTENT, smConfig);
            } else if (op.equals("SR.makeAsync"))
            {
                SR.makeAsync(c, our_host, new HashMap<String, String>(), TEST_SR_SIZE, TEST_SR_NAME, TEST_SR_DESC,
                        TEST_SR_TYPE, TEST_SR_CONTENT, smConfig);
            } else if (op.equals("SR.forget"))
            {
                Set<SR> srs = SR.getByNameLabel(c, TEST_SR_NAME);
                for (SR sr : srs)
                {
                    // First destroy any PBDs associated with the SR
                    Set<PBD> pbds = PBD.getAll(c);
                    for (PBD pbd : pbds)
                    {
                        if (pbd.getSR(c).equals(sr))
                        {
                            pbd.unplug(c);
                            pbd.destroy(c);
                        }
                    }
                    sr.forget(c);
                    break;
                }
            } else if (op.equals("SR.introduce"))
            {
                SR.introduce(c, "uuid", TEST_SR_NAME, TEST_SR_DESC, TEST_SR_TYPE, TEST_SR_CONTENT, true, smConfig);
            } else if (op.equals("SR.introduceAsync"))
            {
                SR.introduceAsync(c, "uuid", TEST_SR_NAME, TEST_SR_DESC, TEST_SR_TYPE, TEST_SR_CONTENT, true, smConfig);
            } else
            {
                throw new Exception("bad op");
            }
        } catch (Types.SrUnknownDriver ex)
        {
            logln("Expected error: SR unknown driver. that's ok");
        } catch (Types.XenAPIException ex)
        {
            // Our call parameters are not good and should cause a particular error
            if (errorDescriptionIs(ex.errorDescription, "SR_BACKEND_FAILURE_102"))
            {
                logln("Expected error: SR backend failure 102. that's ok.");
                /* 'The request is missing the server parameter'
                 * 
                 * We're happy with this, since it means that the call made it through to xen
                 * and an attempt was made to execute it. */
                return;
            } else if (errorDescriptionIs(ex.errorDescription, "SR_BACKEND_FAILURE_101"))
            {
                /* 'The request is missing the serverpath parameter' */
                logln("Expected error: SR backend failure 101. that's ok.");
                return;
            } else
            {
                // otherwise, there was a more serious error, which should be
                // passed upwards
                throw ex;
            }
        }
    }
}
