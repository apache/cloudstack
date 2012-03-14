# Common function for Cloudstack's XenAPI plugins
# 
# Copyright (C) 2012 Citrix Systems

import ConfigParser
import logging
import os
import subprocess

from time import localtime, asctime

DEFAULT_LOG_FORMAT = "%(asctime)s %(levelname)8s [%(name)s] %(message)s"
DEFAULT_LOG_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"
DEFAULT_LOG_FILE = "/var/log/cloudstack_plugins.log"

PLUGIN_CONFIG_PATH='/etc/xensource/cloudstack_plugins.conf'
OVSDB_PID_PATH = "/var/run/openvswitch/ovsdb-server.pid"
OVSDB_DAEMON_PATH = "ovsdb-server"
OVS_PID_PATH = "/var/run/openvswitch/ovs-vswitchd.pid"
OVS_DAEMON_PATH = "ovs-vswitchd"
VSCTL_PATH='/usr/bin/ovs-vsctl'
OFCTL_PATH='/usr/bin/ovs-ofctl'
XE_PATH=  "/opt/xensource/bin/xe"

class PluginError(Exception):
    """Base Exception class for all plugin errors."""
    def __init__(self, *args):
        Exception.__init__(self, *args)

def setup_logging(log_file=None):
    debug = False
    verbose = False
    log_format = DEFAULT_LOG_FORMAT
    log_date_format = DEFAULT_LOG_DATE_FORMAT
    # try to read plugin configuration file
    if os.path.exists(PLUGIN_CONFIG_PATH):
        config = ConfigParser.ConfigParser()
        config.read(PLUGIN_CONFIG_PATH)
        try:
            options = config.options('LOGGING')
            if 'debug' in options:
                debug = config.getboolean('LOGGING','debug')
            if 'verbose' in options:
                verbose = config.getboolean('LOGGING','verbose')
            if 'format' in options:
                log_format = config.get('LOGGING','format')
            if 'date_format' in options:
                log_date_format = config.get('LOGGING','date_format')
            if 'file' in options:
                log_file_2 = config.get('LOGGING','file')
        except ValueError:
            # configuration file contained invalid attributes
            # ignore them
            pass
        except ConfigParser.NoSectionError:
            # Missing 'Logging' section in configuration file
            pass
    
    root_logger = logging.root
    if debug:
        root_logger.setLevel(logging.DEBUG)
    elif verbose:
        root_logger.setLevel(logging.INFO)
    else:
        root_logger.setLevel(logging.WARNING)
    formatter = logging.Formatter(log_format, log_date_format)

    log_filename = log_file or log_file_2 or DEFAULT_LOG_FILE
    
    logfile_handler = logging.FileHandler(log_filename)
    logfile_handler.setFormatter(formatter)
    root_logger.addHandler(logfile_handler)


def do_cmd(cmd):
    """Abstracts out the basics of issuing system commands. If the command
    returns anything in stderr, a PluginError is raised with that information.
    Otherwise, the output from stdout is returned.
    """

    pipe = subprocess.PIPE
    logging.debug("Executing:%s", cmd)
    proc = subprocess.Popen(cmd, shell=False, stdin=pipe, stdout=pipe,
                            stderr=pipe, close_fds=True)
    ret_code = proc.wait()
    err = proc.stderr.read()
    if ret_code:
        logging.debug("The command exited with the error code: " +
                      "%s (stderr output:%s)" % (ret_code, err))
        raise PluginError(err)
    output = proc.stdout.read()
    if output.endswith('\n'):
        output = output[:-1]
    return output


def _is_process_run (pidFile, name):
    try:
        fpid = open (pidFile, "r")
        pid = fpid.readline ()
        fpid.close ()
    except IOError, e:
        return -1

    pid = pid[:-1]
    ps = os.popen ("ps -ae")
    for l in ps:
        if pid in l and name in l:
            ps.close ()
            return 0

    ps.close ()
    return -2

def _is_tool_exist (name):
    if os.path.exists(name):
        return 0
    return -1


def check_switch ():
    global result

    ret = _is_process_run (OVSDB_PID_PATH, OVSDB_DAEMON_PATH)
    if ret < 0:
        if ret == -1: return "NO_DB_PID_FILE"
        if ret == -2: return "DB_NOT_RUN"

    ret = _is_process_run (OVS_PID_PATH, OVS_DAEMON_PATH)
    if ret < 0:
        if ret == -1: return "NO_SWITCH_PID_FILE"
        if ret == -2: return "SWITCH_NOT_RUN"

    if _is_tool_exist (VSCTL_PATH) < 0:
        return "NO_VSCTL"

    if _is_tool_exist (OFCTL_PATH) < 0:
        return "NO_OFCTL"

    return "SUCCESS"


def _build_flow_expr(**kwargs):
    is_delete_expr = kwargs.get('delete', False) 
    flow = ""
    if not is_delete_expr:
        flow = "hard_timeout=%s,idle_timeout=%s,priority=%s"\
                % (kwargs.get('hard_timeout','0'),
                   kwargs.get('idle_timeout','0'),
                   kwargs.get('priority','1'))
    in_port = 'in_port' in kwargs and ",in_port=%s" % kwargs['in_port'] or ''
    dl_type = 'dl_type' in kwargs and ",dl_type=%s" % kwargs['dl_type'] or ''
    dl_src = 'dl_src' in kwargs and ",dl_src=%s" % kwargs['dl_src'] or ''
    dl_dst = 'dl_dst' in kwargs and ",dl_dst=%s" % kwargs['dl_dst'] or ''
    nw_src = 'nw_src' in kwargs and ",nw_src=%s" % kwargs['nw_src'] or ''
    nw_dst = 'nw_dst' in kwargs and ",nw_dst=%s" % kwargs['nw_dst'] or ''
    proto = 'proto' in kwargs and ",%s" % kwargs['proto'] or ''
    ip = ('nw_src' in kwargs or 'nw_dst' in kwargs) and ',ip' or ''
    flow = (flow + in_port + dl_type + dl_src + dl_dst + 
            (ip or proto) + nw_src + nw_dst)
    return flow


def add_flow(bridge, **kwargs):
    """
    Builds a flow expression for **kwargs and adds the flow entry
    to an Open vSwitch instance
    """
    flow = _build_flow_expr(**kwargs)
    actions = 'actions' in kwargs and ",actions=%s" % kwargs['actions'] or ''
    flow = flow + actions
    addflow = [OFCTL_PATH, "add-flow", bridge, flow]
    do_cmd(addflow)


def del_flows(bridge, **kwargs):
    """ 
    Removes flows according to criteria passed as keyword.
    """
    flow = _build_flow_expr(delete=True, **kwargs)
    # out_port condition does not exist for all flow commands
    out_port = 'out_port' in kwargs and ",out_port=%s" % kwargs['out_port'] or ''
    flow = flow + out_port
    delFlow = [OFCTL_PATH, 'del-flows', bridge, flow]
    do_cmd(delFlow)
    
    
def del_all_flows(bridge):
    delFlow = [OFCTL_PATH, "del-flows", bridge]
    do_cmd(delFlow)

    normalFlow = "priority=0 idle_timeout=0 hard_timeout=0 actions=normal"
    add_flow(bridge, normalFlow)


def del_port(bridge, port):
    delPort = [VSCTL_PATH, "del-port", bridge, port]
    do_cmd(delPort)
