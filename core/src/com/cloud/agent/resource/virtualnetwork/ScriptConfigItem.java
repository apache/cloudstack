package com.cloud.agent.resource.virtualnetwork;

public class ScriptConfigItem extends ConfigItem {
    private String script;
    private String args;

    public ScriptConfigItem(String script, String args) {
        this.script = script;
        this.args = args;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    @Override
    public String getAggregateCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("<script>\n");
        sb.append("/opt/cloud/bin/");
        sb.append(script);
        sb.append(' ');
        sb.append(args);
        sb.append("\n</script>\n");
        return sb.toString();
    }

}
