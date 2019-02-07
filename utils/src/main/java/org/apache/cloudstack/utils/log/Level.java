package org.apache.cloudstack.utils.log;

public class Level extends org.apache.log4j.Level {
    protected Level(int level, String levelStr, int syslogEquivalent) {
        super(level, levelStr, syslogEquivalent);
    }
}
