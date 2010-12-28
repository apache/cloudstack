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

import java.util.Map;

import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Bond;
import com.xensource.xenapi.Console;
import com.xensource.xenapi.Crashdump;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.HostCpu;
import com.xensource.xenapi.HostCrashdump;
import com.xensource.xenapi.HostMetrics;
import com.xensource.xenapi.HostPatch;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.PIFMetrics;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.PoolPatch;
import com.xensource.xenapi.SM;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VBDMetrics;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VIFMetrics;
import com.xensource.xenapi.VLAN;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.VMGuestMetrics;
import com.xensource.xenapi.VMMetrics;

/**
 * Does what it says on the tin.
 */
public class GetAllRecordsOfAllTypes extends TestBase
{
    public static void RunTest(ILog logger, TargetServer server) throws Exception
    {
        TestBase.logger = logger;

        logln("We'll try to retrieve all the records for all types of objects");
        logln("This should exercise most of the marshalling code");

        try
        {
            connect(server);
            callAutoCode();
        } finally
        {
            disconnect();
        }
    }

    /*
     * Here is a way to do it with common lisp in emacs. Not sure about the
     * sanity aspect. lisp code ----------- load into emacs, start slime with
     * M-x slime, edit this file as lisp with M-x slime-mode, and then we can
     * use C-1 M-C-x to evaluate arbitrary lisp expressions and paste them into
     * the buffer. M-x lisp-mode and M-x java-mode can be used to switch between
     * the two views of the file
     * 
     * (format nil "~A" "test")
     * 
     * ;;list of all database objects culled from javadoc ;;missing Event,
     * Session, User, VTPM (because they don't have get_all_records methods.)
     * (defvar objects '( "Console" "Crashdump" "Host" "HostCpu" "HostCrashdump"
     * "HostMetrics" "HostPatch" "Network" "PBD" "PIF" "PIFMetrics" "Pool" "SM"
     * "SR" "Task" "VBD" "VBDMetrics" "VDI" "VIF" "VIFMetrics" "VM"
     * "VMGuestMetrics" "VMMetrics")) (defvar miami-only-objects '( "Bond"
     * "PoolPatch" "VLAN" ))
     * 
     * 
     * (defun java-test-getAllRecords-proc(string) (format nil " //automatically
     * generated. Do not modify public static void test~As() throws Exception {
     * announce( \"Get all the ~:* ~A Records\" ); Map<~:* ~A,~:* ~A.Record>
     * allrecords = ~:* ~A.getAllRecords(connection); logln( \"got:
     * \"+ allrecords.size() + \" records\" ); if (allrecords.size()>0){
     * announce( \"Print out a ~:* ~A record \" ); logln(
     * allrecords.values().toArray()[0]); } hRule(); } " string))
     * 
     * (defun string-concat-map (fn lst) (format nil "~{ ~A ~}" (mapcar fn
     * lst)))
     * 
     * (defun many-test-getAllRecords-procedures (stringlist) (string-concat-map
     * #'java-test-getAllRecords-proc stringlist))
     * 
     * (defun calltestproc(String) (format nil "test~As();" String))
     * 
     * (defun callmiamionlytestproc(String) (format nil "if (!rio) test~As();"
     * String))
     * 
     * (defun callAutoCode(stringlist miami-only-stringlist) (format nil
     * "~{~A~}" (list (format nil " public static void callAutoCode(Boolean rio)
     * throws Exception~%") (format nil " {~%") (format nil "~{ ~A~%~}" (mapcar
     * #'calltestproc stringlist)) (format nil "~{ ~A~%~}" (mapcar
     * #'callmiamionlytestproc miami-only-stringlist)) (format nil " }~%"))))
     * 
     * (progn (format t "//********** Automatically generated code **********")
     * (format t "~A~%" (many-test-getAllRecords-procedures objects)) (format t
     * "~A~%" (many-test-getAllRecords-procedures miami-only-objects)) (format t
     * "~A~%" (callautocode objects miami-only-objects)) (format t "//**********
     * End of automatically generated code **********")) ;;to create the
     * auto-generated code, edit the file in lisp-mode. ;;use C-M-x to evaluate
     * the above expressions, checking that the output of the last one looks
     * right. ;;then place the cursor below this line and type C-1 M-C-x to
     * paste that output into this file. If wrong use C-_ to undo the paste
     */

    // ********** Automatically generated code **********
    // automatically generated. Do not modify
    public static void testConsoles() throws Exception
    {
        announce("Get all the  Console Records");
        Map<Console, Console.Record> allrecords = Console.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  Console record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testCrashdumps() throws Exception
    {
        announce("Get all the  Crashdump Records");
        Map<Crashdump, Crashdump.Record> allrecords = Crashdump.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  Crashdump record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testHosts() throws Exception
    {
        announce("Get all the  Host Records");
        Map<Host, Host.Record> allrecords = Host.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  Host record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testHostCpus() throws Exception
    {
        announce("Get all the  HostCpu Records");
        Map<HostCpu, HostCpu.Record> allrecords = HostCpu.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  HostCpu record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testHostCrashdumps() throws Exception
    {
        announce("Get all the  HostCrashdump Records");
        Map<HostCrashdump, HostCrashdump.Record> allrecords = HostCrashdump.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  HostCrashdump record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testHostMetricss() throws Exception
    {
        announce("Get all the  HostMetrics Records");
        Map<HostMetrics, HostMetrics.Record> allrecords = HostMetrics.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  HostMetrics record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testHostPatchs() throws Exception
    {
        announce("Get all the  HostPatch Records");
        Map<HostPatch, HostPatch.Record> allrecords = HostPatch.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  HostPatch record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testNetworks() throws Exception
    {
        announce("Get all the  Network Records");
        Map<Network, Network.Record> allrecords = Network.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  Network record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testPBDs() throws Exception
    {
        announce("Get all the  PBD Records");
        Map<PBD, PBD.Record> allrecords = PBD.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  PBD record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testPIFs() throws Exception
    {
        announce("Get all the  PIF Records");
        Map<PIF, PIF.Record> allrecords = PIF.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  PIF record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testPIFMetricss() throws Exception
    {
        announce("Get all the  PIFMetrics Records");
        Map<PIFMetrics, PIFMetrics.Record> allrecords = PIFMetrics.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  PIFMetrics record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testPools() throws Exception
    {
        announce("Get all the  Pool Records");
        Map<Pool, Pool.Record> allrecords = Pool.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        for (Pool key : allrecords.keySet())
        {
            announce("Print out a  Pool record ");
            logln(allrecords.get(key).toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testSMs() throws Exception
    {
        announce("Get all the  SM Records");
        Map<SM, SM.Record> allrecords = SM.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  SM record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testSRs() throws Exception
    {
        announce("Get all the  SR Records");
        Map<SR, SR.Record> allrecords = SR.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  SR record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testTasks() throws Exception
    {
        announce("Get all the  Task Records");
        Map<Task, Task.Record> allrecords = Task.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  Task record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testVBDs() throws Exception
    {
        announce("Get all the  VBD Records");
        Map<VBD, VBD.Record> allrecords = VBD.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  VBD record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testVBDMetricss() throws Exception
    {
        announce("Get all the  VBDMetrics Records");
        Map<VBDMetrics, VBDMetrics.Record> allrecords = VBDMetrics.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  VBDMetrics record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testVDIs() throws Exception
    {
        announce("Get all the  VDI Records");
        Map<VDI, VDI.Record> allrecords = VDI.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  VDI record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testVIFs() throws Exception
    {
        announce("Get all the  VIF Records");
        Map<VIF, VIF.Record> allrecords = VIF.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  VIF record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testVIFMetricss() throws Exception
    {
        announce("Get all the  VIFMetrics Records");
        Map<VIFMetrics, VIFMetrics.Record> allrecords = VIFMetrics.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  VIFMetrics record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testVMs() throws Exception
    {
        announce("Get all the  VM Records");
        Map<VM, VM.Record> allrecords = VM.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  VM record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testVMGuestMetricss() throws Exception
    {
        announce("Get all the  VMGuestMetrics Records");
        Map<VMGuestMetrics, VMGuestMetrics.Record> allrecords = VMGuestMetrics.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  VMGuestMetrics record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testVMMetricss() throws Exception
    {
        announce("Get all the  VMMetrics Records");
        Map<VMMetrics, VMMetrics.Record> allrecords = VMMetrics.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  VMMetrics record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testBonds() throws Exception
    {
        announce("Get all the  Bond Records");
        Map<Bond, Bond.Record> allrecords = Bond.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  Bond record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testPoolPatchs() throws Exception
    {
        announce("Get all the  PoolPatch Records");
        Map<PoolPatch, PoolPatch.Record> allrecords = PoolPatch.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  PoolPatch record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    // automatically generated. Do not modify
    public static void testVLANs() throws Exception
    {
        announce("Get all the  VLAN Records");
        Map<VLAN, VLAN.Record> allrecords = VLAN.getAllRecords(connection);
        logln("got: " + allrecords.size() + " records");
        if (allrecords.size() > 0)
        {
            announce("Print out a  VLAN record ");
            logln(allrecords.values().toArray()[0].toString());
        }
        hRule();
    }

    public static void callAutoCode() throws Exception
    {
        testConsoles();
        testCrashdumps();
        testHosts();
        testHostCpus();
        testHostCrashdumps();
        testHostMetricss();
        testHostPatchs();
        testNetworks();
        testPBDs();
        testPIFs();
        testPIFMetricss();
        testPools();
        testSMs();
        testSRs();
        testTasks();
        testVBDs();
        testVBDMetricss();
        testVDIs();
        testVIFs();
        testVIFMetricss();
        testVMs();
        testVMGuestMetricss();
        testVMMetricss();

        if (connection.getAPIVersion() == APIVersion.API_1_1)
        {
            log("Rio connection detected; skipping getting records on Bonds, Patches, and VLANs");
        } else
        {
            testBonds();
            testPoolPatchs();
            testVLANs();
        }
    }

    // ********** End of automatically generated code **********
}
