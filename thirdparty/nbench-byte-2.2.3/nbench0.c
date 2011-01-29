
/*
** nbench0.c
*/

/*******************************************
**             BYTEmark (tm)              **
** BYTE MAGAZINE'S NATIVE MODE BENCHMARKS **
**           FOR CPU/FPU                  **
**             ver 2.0                    **
**       Rick Grehan, BYTE Magazine       **
********************************************
** NOTE: These benchmarks do NOT check for the presence
** of an FPU.  You have to find that out manually.
**
** REVISION HISTORY FOR BENCHMARKS
**  9/94 -- First beta. --RG
**  12/94 -- Bug discovered in some of the integer routines
**    (IDEA, Huffman,...).  Routines were not accurately counting
**    the number of loops.  Fixed. --RG (Thanks to Steve A.)
**  12/94 -- Added routines to calculate and display index
**    values. Indexes based on DELL XPS 90 (90 MHz Pentium).
**  1/95 -- Added Mac time manager routines for more accurate
**    timing on Macintosh (said to be good to 20 usecs) -- RG
**  1/95 -- Re-did all the #defines so they made more
**    sense.  See NMGLOBAL.H -- RG
**  3/95 -- Fixed memory leak in LU decomposition.  Did not
**    invalidate previous results, just made it easier to run.--RG
**  3/95 -- Added TOOLHELP.DLL timing routine to Windows timer. --RG
**  10/95 -- Added memory array & alignment; moved memory
**      allocation out of LU Decomposition -- RG
**
** DISCLAIMER
** The source, executable, and documentation files that comprise
** the BYTEmark benchmarks are made available on an "as is" basis.
** This means that we at BYTE Magazine have made every reasonable
** effort to verify that the there are no errors in the source and
** executable code.  We cannot, however, guarantee that the programs
** are error-free.  Consequently, McGraw-HIll and BYTE Magazine make
** no claims in regard to the fitness of the source code, executable
** code, and documentation of the BYTEmark.
**  Furthermore, BYTE Magazine, McGraw-Hill, and all employees
** of McGraw-Hill cannot be held responsible for any damages resulting
** from the use of this code or the results obtained from using
** this code.
*/

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include "nmglobal.h"
#include "nbench0.h"
#include "hardware.h"

/*************
**** main ****
*************/
#ifdef MAC
void main(void)
#else
int main(int argc, char *argv[])
#endif
{
int i;                  /* Index */
time_t time_and_date;   /* Self-explanatory */
struct tm *loctime;
double bmean;           /* Benchmark mean */
double bstdev;          /* Benchmark stdev */
double lx_memindex;     /* Linux memory index (mainly integer operations)*/
double lx_intindex;     /* Linux integer index */
double lx_fpindex;      /* Linux floating-point index */
double intindex;        /* Integer index */
double fpindex;         /* Floating-point index */
ulong bnumrun;          /* # of runs */

#ifdef MAC
        MaxApplZone();
#endif

#ifdef MACTIMEMGR
/* Set up high res timer */
MacHSTdelay=600*1000*1000;      /* Delay is 10 minutes */

memset((char *)&myTMTask,0,sizeof(TMTask));

/* Prime and remove the task, calculating overhead */
PrimeTime((QElemPtr)&myTMTask,-MacHSTdelay);
RmvTime((QElemPtr)&myTMTask);
MacHSTohead=MacHSTdelay+myTMTask.tmCount;
#endif

#ifdef WIN31TIMER
/* Set up the size of the timer info structure */
win31tinfo.dwSize=(DWORD)sizeof(TIMERINFO);
/* Load library */
if((hThlp=LoadLibrary("TOOLHELP.DLL"))<32)
{       printf("Error loading TOOLHELP\n");
        exit(0);
}
if(!(lpfn=GetProcAddress(hThlp,"TimerCount")))
{       printf("TOOLHELP error\n");
        exit(0);
}
#endif

/*
** Set global parameters to default.
*/
global_min_ticks=MINIMUM_TICKS;
global_min_seconds=MINIMUM_SECONDS;
global_allstats=0;
global_custrun=0;
global_align=8;
write_to_file=0;
lx_memindex=(double)1.0;        /* set for geometric mean computations */
lx_intindex=(double)1.0;
lx_fpindex=(double)1.0;
intindex=(double)1.0;
fpindex=(double)1.0;
mem_array_ents=0;               /* Nothing in mem array */

/*
** We presume all tests will be run unless told
** otherwise
*/
for(i=0;i<NUMTESTS;i++)
        tests_to_do[i]=1;
/* Don't do floating point */
tests_to_do[4] = tests_to_do[8] = tests_to_do[9] = 0;

/*
** Initialize test data structures to default
** values.
*/
set_request_secs();     /* Set all request_secs fields */
global_numsortstruct.adjust=0;
global_numsortstruct.arraysize=NUMARRAYSIZE;

global_strsortstruct.adjust=0;
global_strsortstruct.arraysize=STRINGARRAYSIZE;

global_bitopstruct.adjust=0;
global_bitopstruct.bitfieldarraysize=BITFARRAYSIZE;

global_emfloatstruct.adjust=0;
global_emfloatstruct.arraysize=EMFARRAYSIZE;

global_fourierstruct.adjust=0;

global_assignstruct.adjust=0;

global_ideastruct.adjust=0;
global_ideastruct.arraysize=IDEAARRAYSIZE;

global_huffstruct.adjust=0;
global_huffstruct.arraysize=HUFFARRAYSIZE;

global_nnetstruct.adjust=0;

global_lustruct.adjust=0;

/*
** For Macintosh -- read the command line.
*/
#ifdef MAC
UCommandLine();
#endif

/*
** Handle any command-line arguments.
*/
if(argc>1)
        for(i=1;i<argc;i++)
                if(parse_arg(argv[i])==-1)
                {       display_help(argv[0]);
                        exit(0);
                }
/*
** Output header
*/
#ifdef LINUX
/*
output_string("\nBYTEmark* Native Mode Benchmark ver. 2 (10/95)\n");
output_string("Index-split by Andrew D. Balsa (11/97)\n");
output_string("Linux/Unix* port by Uwe F. Mayer (12/96,11/97)\n");
*/
#else
output_string("BBBBBB   YYY   Y  TTTTTTT  EEEEEEE\n");
output_string("BBB   B  YYY   Y    TTT    EEE\n");
output_string("BBB   B  YYY   Y    TTT    EEE\n");
output_string("BBBBBB    YYY Y     TTT    EEEEEEE\n");
output_string("BBB   B    YYY      TTT    EEE\n");
output_string("BBB   B    YYY      TTT    EEE\n");
output_string("BBBBBB     YYY      TTT    EEEEEEE\n\n");
output_string("\nBYTEmark (tm) Native Mode Benchmark ver. 2 (10/95)\n");
#endif
/*
** See if the user wants all stats.  Output heading info
** if so.
*/
if(global_allstats)
{
                output_string("\n");
                output_string("============================== ALL STATISTICS ===============================\n");
        time(&time_and_date);
        loctime=localtime(&time_and_date);
        sprintf(buffer,"**Date and time of benchmark run: %s",asctime(loctime));
        output_string(buffer);
        sprintf(buffer,"**Sizeof: char:%u short:%u int:%u long:%u u8:%u u16:%u u32:%u int32:%u\n",
                (unsigned int)sizeof(char),
                (unsigned int)sizeof(short),
                (unsigned int)sizeof(int),
                (unsigned int)sizeof(long),
                (unsigned int)sizeof(u8),
                (unsigned int)sizeof(u16),
                (unsigned int)sizeof(u32),
                (unsigned int)sizeof(int32));
        output_string(buffer);
#ifdef LINUX
#include "sysinfo.c"
#else
        sprintf(buffer,"**%s\n",sysname);
        output_string(buffer);
        sprintf(buffer,"**%s\n",compilername);
        output_string(buffer);
        sprintf(buffer,"**%s\n",compilerversion);
        output_string(buffer);
#endif
                output_string("=============================================================================\n");
}

/*
** Execute the tests.
*/
#ifdef LINUX
/*
output_string("\nTEST                : Iterations/sec.  : Old Index   : New Index\n");
output_string("                    :                  : Pentium 90* : AMD K6/233*\n");
output_string("--------------------:------------------:-------------:------------\n");
*/
#endif

for(i=0;i<NUMTESTS;i++)
{
        if(tests_to_do[i])
        {       /* sprintf(buffer,"%s    :",ftestnames[i]);
                                output_string(buffer);
*/
                if (0!=bench_with_confidence(i,
                        &bmean,
                        &bstdev,
                        &bnumrun)){
/*
		  output_string("\n** WARNING: The current test result is NOT 95 % statistically certain.\n");
		  output_string("** WARNING: The variation among the individual results is too large.\n");
		  output_string("                    :");
*/
		}
#ifdef LINUX
                sprintf(buffer," %15.5g  :  %9.2f  :  %9.2f\n",
                        bmean,bmean/bindex[i],bmean/lx_bindex[i]);
#else
		sprintf(buffer,"  Iterations/sec.: %13.2f  Index: %6.2f\n",
                        bmean,bmean/bindex[i]);
#endif
/*
                output_string(buffer);
*/
		/*
		** Gather integer or FP indexes
		*/
		if((i==4)||(i==8)||(i==9)){
		  /* FP index */
		  fpindex=fpindex*(bmean/bindex[i]);
		  /* Linux FP index */
		  lx_fpindex=lx_fpindex*(bmean/lx_bindex[i]);
		}
		else{
		  /* Integer index */
		  intindex=intindex*(bmean/bindex[i]);
		  if((i==0)||(i==3)||(i==6)||(i==7))
		    /* Linux integer index */
		    lx_intindex=lx_intindex*(bmean/lx_bindex[i]);
		  else
		    /* Linux memory index */
		    lx_memindex=lx_memindex*(bmean/lx_bindex[i]);
		}

                if(global_allstats)
                {
                        sprintf(buffer,"  Absolute standard deviation: %g\n",bstdev);
                        output_string(buffer);
			if (bmean>(double)1e-100){
			  /* avoid division by zero */
			  sprintf(buffer,"  Relative standard deviation: %g %%\n",
				  (double)100*bstdev/bmean);
			  output_string(buffer);
			}
                        sprintf(buffer,"  Number of runs: %lu\n",bnumrun);
                        output_string(buffer);
                        show_stats(i);
                        sprintf(buffer,"Done with %s\n\n",ftestnames[i]);
                        output_string(buffer);
                }
        }
}
/* printf("...done...\n"); */

/*
** Output the total indexes
*/
if(global_custrun==0)
{
        sprintf(buffer,"%d\n",
                       (int)(pow(intindex,(double).142857)*30.0));
        output_string(buffer);
/*
        output_string("==========================ORIGINAL BYTEMARK RESULTS==========================\n");
        sprintf(buffer,"INTEGER INDEX       : %.3f\n",
                       pow(intindex,(double).142857));
        output_string(buffer);
        sprintf(buffer,"FLOATING-POINT INDEX: %.3f\n",
                        pow(fpindex,(double).33333));
        output_string(buffer);
        output_string("Baseline (MSDOS*)   : Pentium* 90, 256 KB L2-cache, Watcom* compiler 10.0\n");
#ifdef LINUX
        output_string("==============================LINUX DATA BELOW===============================\n");
	hardware(write_to_file, global_ofile);
#include "sysinfoc.c"
        sprintf(buffer,"MEMORY INDEX        : %.3f\n",
                       pow(lx_memindex,(double).3333333333));
        output_string(buffer);
        sprintf(buffer,"INTEGER INDEX       : %.3f\n",
                       pow(lx_intindex,(double).25));
        output_string(buffer);
        sprintf(buffer,"FLOATING-POINT INDEX: %.3f\n",
                        pow(lx_fpindex,(double).3333333333));
        output_string(buffer);
        output_string("Baseline (LINUX)    : AMD K6/233*, 512 KB L2-cache, gcc 2.7.2.3, libc-5.4.38\n");
#endif
output_string("* Trademarks are property of their respective holder.\n");
*/
}

exit(0);
}

/**************
** parse_arg **
***************
** Given a pointer to a string, we assume that's an argument.
** Parse that argument and act accordingly.
** Return 0 if ok, else return -1.
*/
static int parse_arg(char *argptr)
{
int i;          /* Index */
FILE *cfile;    /* Command file identifier */

/*
** First character has got to be a hyphen.
*/
if(*argptr++!='-') return(-1);

/*
** Convert the rest of the argument to upper case
** so there's little chance of confusion.
*/
for(i=0;i<strlen(argptr);i++)
        argptr[i]=(char)toupper((int)argptr[i]);

/*
** Next character picks the action.
*/
switch(*argptr++)
{
        case '?':       return(-1);     /* Will display help */

        case 'V': global_allstats=1; return(0); /* verbose mode */

        case 'C':                       /* Command file name */
                /*
                ** First try to open the file for reading.
                */
                cfile=fopen(argptr,"r");
                if(cfile==(FILE *)NULL)
                {       printf("**Error opening file: %s\n",argptr);
                        return(-1);
                }
                read_comfile(cfile);    /* Read commands */
                fclose(cfile);
                break;
        default:
                return(-1);
}
return(0);
}

/*******************
** display_help() **
********************
** Display a help message showing argument requirements and such.
** Exit when you're done...I mean, REALLY exit.
*/
void display_help(char *progname)
{
        printf("Usage: %s [-v] [-c<FILE>]\n",progname);
        printf(" -v = verbose\n");
        printf(" -c = input parameters thru command file <FILE>\n");
        exit(0);
}


/*****************
** read_comfile **
******************
** Read the command file.  Set global parameters as
** specified.  This routine assumes that the command file
** is already open.
*/
static void read_comfile(FILE *cfile)
{
char inbuf[40];
char *eptr;             /* Offset to "=" sign */
int i;                  /* Index */

/*
** Sit in a big loop, reading a line from the file at each
** pass.  Terminate on EOF.
*/
while(fgets(inbuf,39,cfile)!=(char *)NULL)
{
        /* Overwrite the CR character */
        if(strlen(inbuf)>0)
                inbuf[strlen(inbuf)-1]='\0';

        /*
        ** Parse up to the "=" sign.  If we don't find an
        ** "=", then flag an error.
        */
        if((eptr=strchr(inbuf,(int)'='))==(char *)NULL)
        {       printf("**COMMAND FILE ERROR at LINE:\n %s\n",
                        inbuf);
                goto skipswitch;        /* A GOTO!!!! */
        }

        /*
        ** Insert a null where the "=" was, then convert
        ** the substring to uppercase.  That will enable
        ** us to perform the match.
        */
        *eptr++='\0';
        strtoupper((char *)&inbuf[0]);
        i=MAXPARAM;
        do {
                if(strcmp(inbuf,paramnames[i])==0)
                        break;
        } while(--i>=0);

        if(i<0)
        {       printf("**COMMAND FILE ERROR -- UNKNOWN PARAM: %s",
                        inbuf);
                goto skipswitch;
        }

        /*
        ** Advance eptr to the next field...which should be
        ** the value assigned to the parameter.
        */
        switch(i)
        {
                case PF_GMTICKS:        /* GLOBALMINTICKS */
                        global_min_ticks=(ulong)atol(eptr);
                        break;

                case PF_MINSECONDS:     /* MINSECONDS */
                        global_min_seconds=(ulong)atol(eptr);
                        set_request_secs();
                        break;

                case PF_ALLSTATS:       /* ALLSTATS */
                        global_allstats=getflag(eptr);
                        break;

                case PF_OUTFILE:        /* OUTFILE */
                        strcpy(global_ofile_name,eptr);
                        global_ofile=fopen(global_ofile_name,"a");
                        /*
                        ** Open the output file.
                        */
                        if(global_ofile==(FILE *)NULL)
                        {       printf("**Error opening output file: %s\n",
                                        global_ofile_name);
                                ErrorExit();
                        }
                        write_to_file=-1;
                        break;

                case PF_CUSTOMRUN:      /* CUSTOMRUN */
                        global_custrun=getflag(eptr);
                        for(i=0;i<NUMTESTS;i++)
                                tests_to_do[i]=1-global_custrun;
                        break;

                case PF_DONUM:          /* DONUMSORT */
                        tests_to_do[TF_NUMSORT]=getflag(eptr);
                        break;

                case PF_NUMNUMA:        /* NUMNUMARRAYS */
                        global_numsortstruct.numarrays=
                                (ushort)atoi(eptr);
                        global_numsortstruct.adjust=1;
                        break;

                case PF_NUMASIZE:       /* NUMARRAYSIZE */
                        global_numsortstruct.arraysize=
                                (ulong)atol(eptr);
                        break;

                case PF_NUMMINS:        /* NUMMINSECONDS */
                        global_numsortstruct.request_secs=
                                (ulong)atol(eptr);
                        break;

                case PF_DOSTR:          /* DOSTRINGSORT */
                        tests_to_do[TF_SSORT]=getflag(eptr);
                        break;

                case PF_STRASIZE:       /* STRARRAYSIZE */
                        global_strsortstruct.arraysize=
                                (ulong)atol(eptr);
                        break;

                case PF_NUMSTRA:        /* NUMSTRARRAYS */
                        global_strsortstruct.numarrays=
                                (ushort)atoi(eptr);
                        global_strsortstruct.adjust=1;
                        break;

                case PF_STRMINS:        /* STRMINSECONDS */
                        global_strsortstruct.request_secs=
                                (ulong)atol(eptr);
                        break;

                case PF_DOBITF: /* DOBITFIELD */
                        tests_to_do[TF_BITOP]=getflag(eptr);
                        break;

                case PF_NUMBITOPS:      /* NUMBITOPS */
                        global_bitopstruct.bitoparraysize=
                                (ulong)atol(eptr);
                        global_bitopstruct.adjust=1;
                        break;

                case PF_BITFSIZE:       /* BITFIELDSIZE */
                        global_bitopstruct.bitfieldarraysize=
                                (ulong)atol(eptr);
                        break;

                case PF_BITMINS:        /* BITMINSECONDS */
                        global_bitopstruct.request_secs=
                                (ulong)atol(eptr);
                        break;

                case PF_DOEMF:          /* DOEMF */
                        tests_to_do[TF_FPEMU]=getflag(eptr);
                        break;

                case PF_EMFASIZE:       /* EMFARRAYSIZE */
                        global_emfloatstruct.arraysize=
                                (ulong)atol(eptr);
                        break;

                case PF_EMFLOOPS:       /* EMFLOOPS */
                        global_emfloatstruct.loops=
                                (ulong)atol(eptr);
                        break;

                case PF_EMFMINS:        /* EMFMINSECOND */
                        global_emfloatstruct.request_secs=
                                (ulong)atol(eptr);
                        break;

                case PF_DOFOUR: /* DOFOUR */
                        tests_to_do[TF_FFPU]=getflag(eptr);
                        break;

                case PF_FOURASIZE:      /* FOURASIZE */
                        global_fourierstruct.arraysize=
                                (ulong)atol(eptr);
                        global_fourierstruct.adjust=1;
                        break;

                case PF_FOURMINS:       /* FOURMINSECONDS */
                        global_fourierstruct.request_secs=
                                (ulong)atol(eptr);
                        break;

                case PF_DOASSIGN:       /* DOASSIGN */
                        tests_to_do[TF_ASSIGN]=getflag(eptr);
                        break;

                case PF_AARRAYS:        /* ASSIGNARRAYS */
                        global_assignstruct.numarrays=
                                (ulong)atol(eptr);
                        break;

                case PF_ASSIGNMINS:     /* ASSIGNMINSECONDS */
                        global_assignstruct.request_secs=
                                (ulong)atol(eptr);
                        break;

                case PF_DOIDEA: /* DOIDEA */
                        tests_to_do[TF_IDEA]=getflag(eptr);
                        break;

                case PF_IDEAASIZE:      /* IDEAARRAYSIZE */
                        global_ideastruct.arraysize=
                                (ulong)atol(eptr);
                        break;

                case PF_IDEALOOPS:      /* IDEALOOPS */
                        global_ideastruct.loops=
                                (ulong)atol(eptr);
                        break;

                case PF_IDEAMINS:       /* IDEAMINSECONDS */
                        global_ideastruct.request_secs=
                                (ulong)atol(eptr);
                        break;

                case PF_DOHUFF: /* DOHUFF */
                        tests_to_do[TF_HUFF]=getflag(eptr);
                        break;

                case PF_HUFFASIZE:      /* HUFFARRAYSIZE */
                        global_huffstruct.arraysize=
                                (ulong)atol(eptr);
                        break;

                case PF_HUFFLOOPS:      /* HUFFLOOPS */
                        global_huffstruct.loops=
                                (ulong)atol(eptr);
                        global_huffstruct.adjust=1;
                        break;

                case PF_HUFFMINS:       /* HUFFMINSECONDS */
                        global_huffstruct.request_secs=
                                (ulong)atol(eptr);
                        break;

                case PF_DONNET: /* DONNET */
                        tests_to_do[TF_NNET]=getflag(eptr);
                        break;

                case PF_NNETLOOPS:      /* NNETLOOPS */
                        global_nnetstruct.loops=
                                (ulong)atol(eptr);
                        global_nnetstruct.adjust=1;
                        break;

                case PF_NNETMINS:       /* NNETMINSECONDS */
                        global_nnetstruct.request_secs=
                                (ulong)atol(eptr);
                        break;

                case PF_DOLU:           /* DOLU */
                        tests_to_do[TF_LU]=getflag(eptr);
                        break;

                case PF_LUNARRAYS:      /* LUNUMARRAYS */
                        global_lustruct.numarrays=
                                (ulong)atol(eptr);
                        global_lustruct.adjust=1;
                        break;

                case PF_LUMINS: /* LUMINSECONDS */
                        global_lustruct.request_secs=
                                (ulong)atol(eptr);
                        break;

                                case PF_ALIGN:          /* ALIGN */
                                                global_align=atoi(eptr);
                                                break;
        }
skipswitch:
        continue;
}       /* End while */

return;
}

/************
** getflag **
*************
** Return 1 if cptr points to "T"; 0 otherwise.
*/
static int getflag(char *cptr)
{
        if(toupper((int)*cptr)=='T') return(1);
return(0);
}

/***************
** strtoupper **
****************
** Convert's a string to upper case.  The string is presumed
** to consist only of alphabetic characters, and to be terminated
** with a null.
*/
static void strtoupper(char *s)
{

do {
/*
** Oddly enough, the following line did not work under THINK C.
** So, I modified it....hmmmm. --RG
        *s++=(char)toupper((int)*s);
*/
        *s=(char)toupper((int)*s);
        s++;
} while(*s!=(char)'\0');
return;
}

/*********************
** set_request_secs **
**********************
** Set everyone's "request_secs" entry to whatever
** value is in global_min_secs.  This is done
** at the beginning, and possibly later if the
** user redefines global_min_secs in the command file.
*/
static void set_request_secs(void)
{

global_numsortstruct.request_secs=global_min_seconds;
global_strsortstruct.request_secs=global_min_seconds;
global_bitopstruct.request_secs=global_min_seconds;
global_emfloatstruct.request_secs=global_min_seconds;
global_fourierstruct.request_secs=global_min_seconds;
global_assignstruct.request_secs=global_min_seconds;
global_ideastruct.request_secs=global_min_seconds;
global_huffstruct.request_secs=global_min_seconds;
global_nnetstruct.request_secs=global_min_seconds;
global_lustruct.request_secs=global_min_seconds;

return;
}


/**************************
** bench_with_confidence **
***************************
** Given a benchmark id that indicates a function, this routine
** repeatedly calls that benchmark, seeking to collect and replace
** scores to get 5 that meet the confidence criteria.
**
** The above is mathematically questionable, as the statistical theory
** depends on independent observations, and if we exchange data points
** depending on what we already have then this certainly violates
** independence of the observations. Hence I changed this so that at
** most 30 observations are done, but none are deleted as we go
** along. We simply do more runs and hope to get a big enough sample
** size so that things stabilize. Uwe F. Mayer
**
** Return 0 if ok, -1 if failure.  Returns mean
** and std. deviation of results if successful.
*/
static int bench_with_confidence(int fid,       /* Function id */
        double *mean,                   /* Mean of scores */
        double *stdev,                  /* Standard deviation */
        ulong *numtries)                /* # of attempts */
{
double myscores[30];            /* Need at least 5 scores, use at most 30 */
double c_half_interval;         /* Confidence half interval */
int i;                          /* Index */
/* double newscore; */          /* For improving confidence interval */

/*
** Get first 5 scores.  Then begin confidence testing.
*/
for (i=0;i<2;i++)
{       (*funcpointer[fid])();
        myscores[i]=getscore(fid);
#ifdef DEBUG
	printf("score # %d = %g\n", i, myscores[i]);
#endif
}
*numtries=2;            /* Show 5 attempts */

/*
** The system allows a maximum of 30 tries before it gives
** up.  Since we've done 5 already, we'll allow 25 more.
*/

/*
** Enter loop to test for confidence criteria.
*/
while(1)
{
        /*
        ** Calculate confidence. Should always return 0.
        */
        if (0!=calc_confidence(myscores,
		*numtries,
                &c_half_interval,
                mean,
                stdev)) return(-1);

        /*
        ** Is the length of the half interval 5% or less of mean?
        ** If so, we can go home.  Otherwise, we have to continue.
        */
        if(c_half_interval/ (*mean) <= (double)0.05)
                break;

#ifdef OLDCODE
#undef OLDCODE
#endif
#ifdef OLDCODE
/* this code is no longer valid, we now do not replace but add new scores */
/* Uwe F. Mayer */
	      /*
	      ** Go get a new score and see if it
	      ** improves existing scores.
	      */
	      do {
		      if(*numtries==10)
			      return(-1);
		      (*funcpointer[fid])();
		      *numtries+=1;
		      newscore=getscore(fid);
	      } while(seek_confidence(myscores,&newscore,
		      &c_half_interval,mean,stdev)==0);
#endif
	/* We now simply add a new test run and hope that the runs
           finally stabilize, Uwe F. Mayer */
	if(*numtries==30) return(-1);
	(*funcpointer[fid])();
	myscores[*numtries]=getscore(fid);
#ifdef DEBUG
	printf("score # %ld = %g\n", *numtries, myscores[*numtries]);
#endif
	*numtries+=1;
}

return(0);
}

#ifdef OLDCODE
/* this procecdure is no longer needed, Uwe F. Mayer */
  /********************
  ** seek_confidence **
  *********************
  ** Pass this routine an array of 5 scores PLUS a new score.
  ** This routine tries the new score in place of each of
  ** the other five scores to determine if the new score,
  ** when replacing one of the others, improves the confidence
  ** half-interval.
  ** Return 0 if failure.  Original 5 scores unchanged.
  ** Return -1 if success.  Also returns new half-interval,
  ** mean, and standard deviation of the sample.
  */
  static int seek_confidence( double scores[5],
  		double *newscore,
  		double *c_half_interval,
  		double *smean,
  		double *sdev)
  {
  double sdev_to_beat;    /* Original sdev to be beaten */
  double temp;            /* For doing a swap */
  int is_beaten;          /* Indicates original was beaten */
  int i;                  /* Index */

  /*
  ** First calculate original standard deviation
  */
  calc_confidence(scores,c_half_interval,smean,sdev);
  sdev_to_beat=*sdev;
  is_beaten=-1;

  /*
  ** Try to beat original score.  We'll come out of this
  ** loop with a flag.
  */
  for(i=0;i<2;i++)
  {
  	temp=scores[i];
  	scores[i]=*newscore;
  	calc_confidence(scores,c_half_interval,smean,sdev);
  	scores[i]=temp;
  	if(sdev_to_beat>*sdev)
  	{       is_beaten=i;
  		sdev_to_beat=*sdev;
  	}
  }

  if(is_beaten!=-1)
  {       scores[is_beaten]=*newscore;
  	return(-1);
  }
  return(0);
  }
#endif

/********************
** calc_confidence **
*********************
** Given a set of numtries scores, calculate the confidence
** half-interval.  We'll also return the sample mean and sample
** standard deviation.
** NOTE: This routines presumes a confidence of 95% and
** a confidence coefficient of .95
** returns 0 if there is an error, otherwise -1
*/
static int calc_confidence(double scores[], /* Array of scores */
		int num_scores,             /* number of scores in array */
                double *c_half_interval,    /* Confidence half-int */
                double *smean,              /* Standard mean */
                double *sdev)               /* Sample stand dev */
{
/* Here is a list of the student-t distribution up to 29 degrees of
   freedom. The value at 0 is bogus, as there is no value for zero
   degrees of freedom. */
double student_t[30]={0.0 , 12.706 , 4.303 , 3.182 , 2.776 , 2.571 ,
                             2.447 , 2.365 , 2.306 , 2.262 , 2.228 ,
                             2.201 , 2.179 , 2.160 , 2.145 , 2.131 ,
                             2.120 , 2.110 , 2.101 , 2.093 , 2.086 ,
                             2.080 , 2.074 , 2.069 , 2.064 , 2.060 ,
		             2.056 , 2.052 , 2.048 , 2.045 };
int i;          /* Index */
if ((num_scores<2) || (num_scores>30)) {
  output_string("Internal error: calc_confidence called with an illegal number of scores\n");
  return(-1);
}
/*
** First calculate mean.
*/
*smean=(double)0.0;
for(i=0;i<num_scores;i++){
  *smean+=scores[i];
}
*smean/=(double)num_scores;

/* Get standard deviation */
*sdev=(double)0.0;
for(i=0;i<num_scores;i++) {
  *sdev+=(scores[i]-(*smean))*(scores[i]-(*smean));
}
*sdev/=(double)(num_scores-1);
*sdev=sqrt(*sdev);

/* Now calculate the length of the confidence half-interval.  For a
** confidence level of 95% our confidence coefficient gives us a
** multiplying factor of the upper .025 quartile of a t distribution
** with num_scores-1 degrees of freedom, and dividing by sqrt(number of
** observations). See any introduction to statistics.
*/
*c_half_interval=student_t[num_scores-1] * (*sdev) / sqrt((double)num_scores);
return(0);
}

/*************
** getscore **
**************
** Return the score for a particular benchmark.
*/
static double getscore(int fid)
{

/*
** Fid tells us the function.  This is really a matter of
** doing the proper coercion.
*/
switch(fid)
{
        case TF_NUMSORT:
                return(global_numsortstruct.sortspersec);
        case TF_SSORT:
                return(global_strsortstruct.sortspersec);
        case TF_BITOP:
                return(global_bitopstruct.bitopspersec);
        case TF_FPEMU:
                return(global_emfloatstruct.emflops);
        case TF_FFPU:
                return(global_fourierstruct.fflops);
        case TF_ASSIGN:
                return(global_assignstruct.iterspersec);
        case TF_IDEA:
                return(global_ideastruct.iterspersec);
        case TF_HUFF:
                return(global_huffstruct.iterspersec);
        case TF_NNET:
                return(global_nnetstruct.iterspersec);
        case TF_LU:
                return(global_lustruct.iterspersec);
}
return((double)0.0);
}

/******************
** output_string **
*******************
** Displays a string on the screen.  Also, if the flag
** write_to_file is set, outputs the string to the output file.
** Note, this routine presumes that you've included a carriage
** return at the end of the buffer.
*/
static void output_string(char *buffer)
{

printf("%s",buffer);
if(write_to_file!=0)
        fprintf(global_ofile,"%s",buffer);
return;
}

/***************
** show_stats **
****************
** This routine displays statistics for a particular benchmark.
** The benchmark is identified by its id.
*/
static void show_stats (int bid)
{
char buffer[80];        /* Display buffer */

switch(bid)
{
        case TF_NUMSORT:                /* Numeric sort */
                sprintf(buffer,"  Number of arrays: %d\n",
                        global_numsortstruct.numarrays);
                output_string(buffer);
                sprintf(buffer,"  Array size: %ld\n",
                        global_numsortstruct.arraysize);
                output_string(buffer);
                break;

        case TF_SSORT:          /* String sort */
                sprintf(buffer,"  Number of arrays: %d\n",
                        global_strsortstruct.numarrays);
                output_string(buffer);
                sprintf(buffer,"  Array size: %ld\n",
                        global_strsortstruct.arraysize);
                output_string(buffer);
                break;

        case TF_BITOP:          /* Bitmap operation */
                sprintf(buffer,"  Operations array size: %ld\n",
                        global_bitopstruct.bitoparraysize);
                output_string(buffer);
                sprintf(buffer,"  Bitfield array size: %ld\n",
                        global_bitopstruct.bitfieldarraysize);
                output_string(buffer);
                break;

        case TF_FPEMU:          /* Floating-point emulation */
                sprintf(buffer,"  Number of loops: %lu\n",
                        global_emfloatstruct.loops);
                output_string(buffer);
                sprintf(buffer,"  Array size: %lu\n",
                        global_emfloatstruct.arraysize);
                output_string(buffer);
                break;

        case TF_FFPU:           /* Fourier test */
                sprintf(buffer,"  Number of coefficients: %lu\n",
                        global_fourierstruct.arraysize);
                output_string(buffer);
                break;

        case TF_ASSIGN:
                sprintf(buffer,"  Number of arrays: %lu\n",
                        global_assignstruct.numarrays);
                output_string(buffer);
                break;

        case TF_IDEA:
                sprintf(buffer,"  Array size: %lu\n",
                        global_ideastruct.arraysize);
                output_string(buffer);
                sprintf(buffer," Number of loops: %lu\n",
                        global_ideastruct.loops);
                output_string(buffer);
                break;

        case TF_HUFF:
                sprintf(buffer,"  Array size: %lu\n",
                        global_huffstruct.arraysize);
                output_string(buffer);
                sprintf(buffer,"  Number of loops: %lu\n",
                        global_huffstruct.loops);
                output_string(buffer);
                break;

        case TF_NNET:
                sprintf(buffer,"  Number of loops: %lu\n",
                        global_nnetstruct.loops);
                output_string(buffer);
                break;

        case TF_LU:
                sprintf(buffer,"  Number of arrays: %lu\n",
                        global_lustruct.numarrays);
                output_string(buffer);
                break;
}
return;
}

/*
** Following code added for Mac stuff, so that we can emulate command
** lines.
*/

#ifdef MAC

/*****************
** UCommandLine **
******************
** Reads in a command line, and sets up argc and argv appropriately.
** Note that this routine uses gets() to read in the line.  This means
** you'd better not enter more than 128 characters on a command line, or
** things will overflow, and oh boy...
*/
void UCommandLine(void)
{
printf("Enter command line\n:");
gets((char *)Uargbuff);
UParse();
return;
}

/***********
** UParse **
************
** Parse the pseudo command-line.  This code appeared as part of the
** Small-C library in Dr. Dobb's ToolBook of C.
** It expects the following globals:
** argc = arg count
** argv = Pointer to array of char pointers
** Uargbuff = Character array that holds the arguments.  Should be 129 bytes long.
** Udummy1 = This is a 2-byte buffer that holds a "*", and acts as the first
**  argument in the argument list.  This maintains compatibility with other
**  C's, though it does not provide access to the executable filename.
** This routine allows for up to 20 individual command-line arguments.
** Also note that this routine does NOT allow for redirection.
*/
void UParse(void)
{
unsigned char *ptr;

argc=0;         /* Start arg count */
Udummy[0]='*';  /* Set dummy first argument */
Udummy[1]='\0';
argv[argc++]=(char *)Udummy;

ptr=Uargbuff;           /* Start pointer */
while(*ptr)
{
        if(isspace(*ptr))
        {       ++ptr;
                continue;
        }
        if(argc<20) argv[argc++]=(char *)ptr;
        ptr=UField(ptr);
}
return;
}
/***********
** UField **
************
** Isolate the next command-line field.
*/
unsigned char *UField(unsigned char *ptr)
{
while(*ptr)
{       if(isspace(*ptr))
        {       *ptr=(unsigned char)NULL;
                return(++ptr);
        }
        ++ptr;
}
return(ptr);
}
#endif
