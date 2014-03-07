package com.cloud.api.dispatch;

import java.util.Map;

import org.apache.cloudstack.api.BaseCmd;

/**
 * This class wraps all the data that any worker could need. If we don't wrap it this
 * way and we pass the parameters one by one, in the end we could end up having all the
 * N workers receiving plenty of parameters and changing the signature, each time one
 * of them changes. This way, if a certain worker needs something else, you just need
 * to change it in this wrapper class and the worker itself.
 */
@SuppressWarnings("rawtypes")
public class DispatchTask {

    protected BaseCmd cmd;

    protected Map params;

    public DispatchTask(final BaseCmd cmd, final Map params) {
        this.cmd = cmd;
        this.params = params;
    }

    public BaseCmd getCmd() {
        return cmd;
    }

    public void setCmd(final BaseCmd cmd) {
        this.cmd = cmd;
    }

    public Map getParams() {
        return params;
    }

    public void setParams(final Map params) {
        this.params = params;
    }
}
