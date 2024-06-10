package org.apache.cloudstack.storage.resource;

import com.cloud.utils.script.Script;
import org.apache.cloudstack.storage.template.DownloadManagerImpl;
import org.apache.log4j.Logger;

public class IpTablesHelper {
    public static final Logger LOGGER = Logger.getLogger(IpTablesHelper.class);

    public static final String OUTPUT_CHAIN = "OUTPUT";
    public static final String INPUT_CHAIN = "INPUT";
    public static final String INSERT = " -I ";
    public static final String APPEND = " -A ";

    public static boolean needsAdding(String chain, String rule) {
        Script command = new Script("/bin/bash", LOGGER);
        command.add("-c");
        command.add("iptables -C " + chain + " " + rule);

        String r1 = command.execute();
        boolean needsAdding = (r1 != null && r1.contains("iptables: Bad rule (does a matching rule exist in that chain?)."));
//        int rc = Script.runSimpleBashScriptForExitValue(command.toString());
//        boolean needsAdding = rc != 0; // (executionResult != null && executionResult.contains("iptables: Bad rule (does a matching rule exist in that chain?)."));
        LOGGER.debug(String.format("Rule [%s], %s need adding to [%s] : %s",
                rule,
                needsAdding ? "does indeed" : "doesn't",
                chain,
                r1
        ));
        return needsAdding;
    }

    public static String addConditionally(String chain, boolean insert, String rule, String errMsg) {
        LOGGER.info(String.format("Adding rule [%s] to [%s] if required.", rule, chain));
        if (needsAdding(chain, rule)) {
            Script command = new Script("/bin/bash", DownloadManagerImpl.LOGGER);
            command.add("-c");
            command.add("iptables" + (insert ? INSERT : APPEND) + chain + " " + rule);
            String result = command.execute();
            LOGGER.debug(String.format("Executed [%s] with result [%s]", command, result));
            if (result != null) {
                LOGGER.warn(errMsg + result);
                return errMsg + result;
            }
        } else {
            LOGGER.warn("Rule already defined in SVM: " + rule);
        }
        return null;
    }
}