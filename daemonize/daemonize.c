/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

/*
UNIX daemonizer.  Daemonizes any non-interactive console program and watches over it.
Whenever a signal is sent to this process, it halts the daemonized process as well.

To compile:	cc -o daemonize daemonize.c
Usage:		./daemonize -?
Users of this:	catalina initscript
*/

#include <stdio.h>
#include <fcntl.h>
#include <signal.h>
#include <unistd.h>
#include <syslog.h>
#include <string.h>
#include <stdlib.h>
#include <getopt.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <errno.h>
#include <pwd.h>

#define RUNNING_DIR	"/"
#define PIDFILE		"/var/run/daemonize.pid"
#define VARLOGFILE 	"/var/log/daemon.log"
#define PROGNAME	"daemonized"
#define DEFAULTUSER	"root"

char * pidfile = PIDFILE;
char * varlogfile = VARLOGFILE;
char * progname = PROGNAME;
char * user = PROGNAME;

void initialize_syslog(const char*pn) {
	openlog(pn,LOG_PID,LOG_DAEMON);
	syslog(LOG_INFO, "syslog connection opened");
}

void cleanup_syslog() {
	syslog(LOG_INFO, "syslog connection closed");
	closelog(); 
}

int killed = 0;
int killsignal = 0;
int pidfile_fd;
int varlogfile_fd;
int uid = 0; int gid = 0;
struct passwd *creds;

void signal_handler(sig)
int sig;
{
	killsignal = sig;
	switch(sig) {
	case SIGCHLD:
		syslog(LOG_INFO,"sigchild signal caught");
		break;
	case SIGHUP:
		syslog(LOG_INFO,"hangup signal caught");
		killed = 1;
		break;
	case SIGTERM:
		syslog(LOG_INFO,"terminate signal caught");
		killed = 1;
		break;
	case SIGINT:
		syslog(LOG_INFO,"keyboard interrupt signal caught");
		killed = 1;
		break;
	}
}

int daemonize(const char*prog_name)
{

	char str[10];
	int i;
	int bufsize=1024; char *buf = malloc(1024);
	
	umask( S_IWGRP | S_IROTH | S_IWOTH ); /* set newly created file permissions */
	
	/* test logfile */
	varlogfile_fd=open(varlogfile,O_RDWR|O_CREAT|O_APPEND,0666);
	if (varlogfile_fd == -1) {
		snprintf(buf,bufsize,"Could not open output file %s -- exiting",varlogfile); perror(buf);
		return 1; /* exitvalue */
	}
	if (uid != 0) {
		chown(varlogfile,uid,gid);
	}
	close(varlogfile_fd);
	pidfile_fd=open(pidfile,O_RDWR|O_CREAT,0666);
	if (pidfile_fd<0) {
		snprintf(buf,bufsize,"The PID file %s cannot be opened -- exiting",pidfile); perror(buf);
		return 2; /* exitvalue */
	}
	if (lockf(pidfile_fd,F_TEST,0)==1) {
		snprintf(buf,bufsize,"A daemon is already running (cannot lock PID file %s) -- exiting",pidfile); perror(buf);
		return 3; /* exitvalue */
	}
	close(pidfile_fd);
	
	if(getppid()==1) return 0; /* already a daemon */
	i=fork();
	if (i < 0) return 4; /* exitvalue */ /* fork error */
	if (i > 0) exit(0); /* parent exits */

	/* child (daemon) continues */
	setsid(); /* obtain a new process group */

	chdir(RUNNING_DIR); /* change running directory */
	 
	/* close FDs and reopen to logfile */
	for (i=getdtablesize();i>=0;--i) close(i); /* close all descriptors */
	varlogfile_fd=open(varlogfile,O_RDWR|O_APPEND,0666); dup(varlogfile_fd); dup(varlogfile_fd); /* handle standart I/O */
	initialize_syslog(prog_name); /* set up syslog */
	
	/* PID file */
	pidfile_fd=open(pidfile,O_RDWR|O_CREAT,0666);
	if (pidfile_fd<0) {
		syslog(LOG_ERR,"The PID file %s cannot be opened (%m) -- exiting",pidfile);
		return 2; /* exitvalue */
	}
	if (lockf(pidfile_fd,F_TLOCK,0)<0) {
		syslog(LOG_ERR,"A daemon is already running -- cannot lock PID file %s (%m) -- exiting",pidfile);
		return 3; /* exitvalue */
	}

	/* first instance continues */
	
	/* record pid to pidfile */
	sprintf(str,"%d\n",getpid());
	if (write(pidfile_fd,str,strlen(str)) < strlen(str)) {
		syslog(LOG_ERR,"Could not write PID into PID file %s (%m) -- exiting",pidfile);
		return 5; /* exitvalue */
	}
	signal(SIGTSTP,SIG_IGN); /* ignore tty signals */
	signal(SIGTTOU,SIG_IGN);
	signal(SIGTTIN,SIG_IGN);
	signal(SIGHUP,signal_handler); /* catch hangup signal */
	signal(SIGTERM,signal_handler); /* catch kill signal */
	signal(SIGINT,signal_handler); /* catch keyboard interrupt signal */
	
	return 0;
}

void cleanup() {
	cleanup_syslog();
	unlink(pidfile);
	close(pidfile_fd);
	close(varlogfile_fd);
}

void usage(char * cmdname) {
	fprintf (stderr,
		"Usage: %s [options...] -- <command> [command-specific arguments...]\n"
		"Daemonize any program.\n"
		"\n"
		"Options:\n"
		"\n"
		"	-l <logfile>:   log stdout/stderr to this *absolute* path (default "VARLOGFILE")\n"
		"	-u <username>:   setuid() to this user name before starting the program (default "DEFAULTUSER")\n"
		"	-p <pidfile>:   lock and write the PID to this *absolute* path (default "PIDFILE")\n"
		"	-n <progname>:  name the daemon assumes (default "PROGNAME")\n"
		"	-h: show this usage guide\n"
		"\n"
		"Exit status:\n"
		" 0      if daemonized correctly\n"
		" other  if an error took place\n"
		"", cmdname);
	exit(0);
}

int parse_args(int argc,char ** argv) {
	int index;
	int c;
	
// 	pidfile = PIDFILE;
// 	varlogfile = VARLOGFILE;
// 	progname = PROGNAME;
	
	opterr = 0;
	
	while ((c = getopt (argc, argv, "l:p:n:u:")) != -1)
		switch (c)
		{
			case 'l':
				varlogfile = optarg;
				break;
			case 'p':
				pidfile = optarg;
				break;
			case 'n':
				progname = optarg;
				break;
			case 'u':
				if (getuid() != 0) {
					fprintf (stderr, "-u can only be used by root.\nSee help with -h\n", user);
					exit(64);
				}
				user = optarg;
				creds = getpwnam(user);
				if (creds == NULL) {
					fprintf (stderr, "User %s was not found in the user database.\nSee help with -h\n", user);
					exit(63);
				}
				uid = creds->pw_uid; gid = creds->pw_gid;
				break;
// 			case 'h':
// 				break;
// 				usage(argv[0]); /* halts after this */
			case '?':
				if (optopt == '?' || optopt == 'h')
					usage(argv[0]); /* halts after this */
				if (optopt == 'l' || optopt == 'p' || optopt == 'n')
					fprintf (stderr, "Option -%c requires an argument.\nSee help with -h\n", optopt);
				else if (isprint (optopt))
					fprintf (stderr, "Unknown option `-%c'.\nSee help with -h\n", optopt);
				else
					fprintf (stderr, "Unknown option character `\\x%x'.\nSee help with -h\n", optopt);
				exit(64); /* exitvalue */
			default:
				abort ();
		}
	
	for (index = optind; index < argc; index++);

	if (index == optind) {
		fprintf (stderr, "You need to specify a command to run.\nSee help with -h\n", optopt);
		exit(64); /* exitvalue */
	}

	return optind;
}

int main(int argc, char** argv)
{
	/* parse command line arguments, we will use the first non-option one as the starting point */
	int i;
	char ** newargv = calloc(argc+1, sizeof(char**));
	int startat = parse_args(argc,argv);
	int newargc = argc - startat; 
	for (i = startat; i < argc; i++) { newargv[i-startat] = argv[i]; }

	/* try and daemonize */
	int daemonret = daemonize(progname);
	if (daemonret) exit(daemonret);
	syslog(LOG_INFO,"successfully daemonized");

	/* fork */
	int pid, wpid, status, execret;
	syslog(LOG_INFO,"starting %s in subprocess",newargv[0]);
	pid = fork();
	if (pid < 0) {
		/* failed to fork, damnit! */
		syslog(LOG_ERR,"could not fork to run %s as a child process (%m)",newargv[0]);
		exit(4); /* exitvalue */
	}
	else if (pid == 0) {
		/* child */
		if (uid != 0) {
			execret = setgid(gid);
			if (execret == -1) {
				syslog(LOG_ERR,"could not setgid() to gid %d",gid);
				exit(8); /* exitvalue */
			}
			execret = setuid(uid);
			if (execret == -1) {
				syslog(LOG_ERR,"could not setuid() to uid %d",uid);
				exit(8); /* exitvalue */
			}
		}
		execret = execvp(newargv[0],newargv);
		if (errno == 2) {
			syslog(LOG_ERR,"could not run program: no such file or directory");
			exit(127);
		}
		if (errno == 13) {
			syslog(LOG_ERR,"could not run program: permission denied");
			exit(126);
		}
		syslog(LOG_ERR,"could not run program: unknown reason");
		exit(255);
	}

	/* parent continues here */
	syslog(LOG_INFO,"successfully started subprocess -- PID %d",pid);
	int finalexit = 0;
	int waitret = 0;
	while (1) {
		if (killed) {
			kill(pid,killsignal);
			killed = 0;
		}
		waitret = waitpid(pid,&status,WNOHANG);
		if (waitret == pid) break;
		usleep(250000);
	}
	
	
	if WIFEXITED(status) {
		switch (WEXITSTATUS(status)) {
			case 0:
				syslog(LOG_INFO,"%s exited normally",newargv[0]);
				break;
			case 126:
				syslog(LOG_ERR,"%s: permission denied",newargv[0]);
				finalexit = 126; /* exitvalue */
				break;
			case 127:
				syslog(LOG_ERR,"%s: command not found",newargv[0]);
				finalexit = 127; /* exitvalue */
				break;
			default:
				syslog(LOG_INFO,"%s exited abnormally with status %d",newargv[0],WEXITSTATUS(status));
				finalexit = 6; /* exitvalue */
		}
	}
	if WIFSIGNALED(status) {
		syslog(LOG_INFO,"%s was killed with signal %d",newargv[0],WTERMSIG(status));
		finalexit = 7; /* exitvalue */
	}

	syslog(LOG_INFO,"shutting down");
	cleanup();
	exit(finalexit);
}

/* EOF */

