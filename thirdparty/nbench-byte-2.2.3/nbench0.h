/*
** nbench0.h
** Header for nbench0.c
** BYTEmark (tm)
** BYTE's Native Mode Benchmarks
** Rick Grehan, BYTE Magazine
**
** Creation:
** Revision: 3/95;10/95
**  10/95 - Added memory array & alignment -- RG
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

/*
** Following should be modified accordingly per each
** compilation.
*/
char *sysname="You can enter your system description in nbench0.h";
char *compilername="It then will be printed here after you recompile";
char *compilerversion="Have a nice day";

/*  Parameter flags.  Must coincide with parameter names array
** which appears below. */
#define PF_GMTICKS 0            /* GLOBALMINTICKS */
#define PF_MINSECONDS 1         /* MINSECONDS */
#define PF_ALLSTATS 2           /* ALLSTATS */
#define PF_OUTFILE 3            /* OUTFILE */
#define PF_CUSTOMRUN 4          /* CUSTOMRUN */
#define PF_DONUM 5              /* DONUMSORT */
#define PF_NUMNUMA 6            /* NUMNUMARRAYS */
#define PF_NUMASIZE 7           /* NUMARRAYSIZE */
#define PF_NUMMINS 8            /* NUMMINSECONDS */
#define PF_DOSTR 9              /* DOSTRINGSORT */
#define PF_STRASIZE 10          /* STRARRAYSIZE */
#define PF_NUMSTRA 11           /* NUMSTRARRAYS */
#define PF_STRMINS 12           /* STRMINSECONDS */
#define PF_DOBITF 13            /* DOBITFIELD */
#define PF_NUMBITOPS 14         /* NUMBITOPS */
#define PF_BITFSIZE 15          /* BITFIELDSIZE */
#define PF_BITMINS 16           /* BITMINSECONDS */
#define PF_DOEMF 17             /* DOEMF */
#define PF_EMFASIZE 18          /* EMFARRAYSIZE */
#define PF_EMFLOOPS 19          /* EMFLOOPS */
#define PF_EMFMINS 20           /* EMFMINSECOND */
#define PF_DOFOUR 21            /* DOFOUR */
#define PF_FOURASIZE 22         /* FOURASIZE */
#define PF_FOURMINS 23          /* FOURMINSECONDS */
#define PF_DOASSIGN 24          /* DOASSIGN */
#define PF_AARRAYS 25           /* ASSIGNARRAYS */
#define PF_ASSIGNMINS 26        /* ASSIGNMINSECONDS */
#define PF_DOIDEA 27            /* DOIDEA */
#define PF_IDEAASIZE 28         /* IDEAARRAYSIZE */
#define PF_IDEALOOPS 29         /* IDEALOOPS */
#define PF_IDEAMINS 30          /* IDEAMINSECONDS */
#define PF_DOHUFF 31            /* DOHUFF */
#define PF_HUFFASIZE 32         /* HUFFARRAYSIZE */
#define PF_HUFFLOOPS 33         /* HUFFLOOPS */
#define PF_HUFFMINS 34          /* HUFFMINSECONDS */
#define PF_DONNET 35            /* DONNET */
#define PF_NNETLOOPS 36         /* NNETLOOPS */
#define PF_NNETMINS 37          /* NNETMINSECONDS */
#define PF_DOLU 38              /* DOLU */
#define PF_LUNARRAYS 39         /* LUNUMARRAYS */
#define PF_LUMINS 40            /* LUMINSECONDS */
#define PF_ALIGN 41		        /* ALIGN */

#define MAXPARAM 41

/* Tests-to-do flags...must coincide with test. */
#define TF_NUMSORT 0
#define TF_SSORT 1
#define TF_BITOP 2
#define TF_FPEMU 3
#define TF_FFPU 4
#define TF_ASSIGN 5
#define TF_IDEA 6
#define TF_HUFF 7
#define TF_NNET 8
#define TF_LU 9

#define NUMTESTS 10

/*
** GLOBALS
*/

#define BUF_SIZ 1024

/*
** Test names
*/
char *ftestnames[] = {
        "NUMERIC SORT    ",
        "STRING SORT     ",
        "BITFIELD        ",
        "FP EMULATION    ",
        "FOURIER         ",
        "ASSIGNMENT      ",
        "IDEA            ",
        "HUFFMAN         ",
        "NEURAL NET      ",
        "LU DECOMPOSITION" };

/*
** Indexes -- Baseline is DELL Pentium XP90
** 11/28/94
*/
double bindex[] = {
    38.993,                     /* Numeric sort */
    2.238,                      /* String sort */
    5829704,                    /* Bitfield */
    2.084,                      /* FP Emulation */
    879.278,                    /* Fourier */
    .2628,                      /* Assignment */
    65.382,                     /* IDEA */
    36.062,                     /* Huffman */
    .6225,                      /* Neural Net */
    19.3031 };                  /* LU Decomposition */

/*
** Indices -- Baseline is a AMD K6-233, 32MB RAM (60ns SDRAM),512k L2 cache,
** Linux kernel 2.0.32, libc-5.4.38, gcc-2.7.2.3)
** Nov/30/97
*/
double lx_bindex[] = {
      118.73, 	    /* Numeric sort */
      14.459,	    /* String sort */
    27910000,	    /* Bitfield */
      9.0314,	    /* FP Emulation */
      1565.5,	    /* Fourier */
      1.0132,	    /* Assignment */
      220.21,	    /* IDEA */
      112.93,	    /* Huffman */
      1.4799,	    /* Neural Net */
      26.732};      /* LU Decomposition */

/* Parameter names */
char *paramnames[]= {
        "GLOBALMINTICKS",
        "MINSECONDS",
        "ALLSTATS",
        "OUTFILE",
        "CUSTOMRUN",
        "DONUMSORT",
        "NUMNUMARRAYS",
        "NUMARRAYSIZE",
        "NUMMINSECONDS",
        "DOSTRINGSORT",
        "STRARRAYSIZE",
        "NUMSTRARRAYS",
        "STRMINSECONDS",
        "DOBITFIELD",
        "NUMBITOPS",
        "BITFIELDSIZE",
        "BITMINSECONDS",
        "DOEMF",
        "EMFARRAYSIZE",
        "EMFLOOPS",
        "EMFMINSECONDS",
        "DOFOUR",
        "FOURSIZE",
        "FOURMINSECONDS",
        "DOASSIGN",
        "ASSIGNARRAYS",
        "ASSIGNMINSECONDS",
        "DOIDEA",
        "IDEARRAYSIZE",
        "IDEALOOPS",
        "IDEAMINSECONDS",
        "DOHUFF",
        "HUFARRAYSIZE",
        "HUFFLOOPS",
        "HUFFMINSECONDS",
        "DONNET",
        "NNETLOOPS",
        "NNETMINSECONDS",
        "DOLU",
        "LUNUMARRAYS",
        "LUMINSECONDS",
	"ALIGN" };

/*
** Following array is a collection of flags indicating which
** tests to perform.
*/
int tests_to_do[NUMTESTS];

/*
** Buffer for holding output text.
*/
char buffer[BUF_SIZ];

/*
** Global parameters.
*/
ulong global_min_ticks;         /* Minimum ticks */
ulong global_min_seconds;       /* Minimum seconds tests run */
int global_allstats;            /* Statistics dump flag */
char global_ofile_name[BUF_SIZ];/* Output file name */
FILE *global_ofile;             /* Output file */
int global_custrun;             /* Custom run flag */
int write_to_file;              /* Write output to file */
int global_align;		/* Memory alignment */

/*
** Following global is the memory array.  This is used to store
** original and aligned (modified) memory addresses.
*/
ulong mem_array[2][MEM_ARRAY_SIZE];
int mem_array_ents;		/* # of active entries */

/*
** Following are global structures, one built for
** each of the tests.
*/
SortStruct global_numsortstruct;        /* For numeric sort */
SortStruct global_strsortstruct;        /* For string sort */
BitOpStruct global_bitopstruct;         /* For bitfield operations */
EmFloatStruct global_emfloatstruct;     /* For emul. float. point */
FourierStruct global_fourierstruct;     /* For fourier test */
AssignStruct global_assignstruct;       /* For assignment algorithm */
IDEAStruct global_ideastruct;           /* For IDEA encryption */
HuffStruct global_huffstruct;           /* For Huffman compression */
NNetStruct global_nnetstruct;           /* For Neural Net */
LUStruct global_lustruct;               /* For LU decomposition */

/*
** The following array of function struct pointers lets
** us very rapidly map a function to its controlling
** data structure. NOTE: These must match the "TF_xxx"
** constants above.
*/
void *global_fstruct[] =
{       (void *)&global_numsortstruct,
        (void *)&global_strsortstruct,
        (void *)&global_bitopstruct,
        (void *)&global_emfloatstruct,
        (void *)&global_fourierstruct,
        (void *)&global_assignstruct,
        (void *)&global_ideastruct,
        (void *)&global_huffstruct,
        (void *)&global_nnetstruct,
        (void *)&global_lustruct };

/*
** Following globals added to support command line emulation on
** the Macintosh....which doesn't have command lines.
*/
#ifdef MAC
int argc;                       /* Argument count */
char *argv[20];                 /* Argument vectors */

unsigned char Uargbuff[129];    /* Buffer holding arguments string */
unsigned char Udummy[2];        /* Dummy buffer for first arg */

#endif

#ifdef MACTIMEMGR
#include <Types.h>
#include <Timer.h>
/*
** Timer globals for Mac
*/
struct TMTask myTMTask;
long MacHSTdelay,MacHSTohead;

#endif

/*
** Following globals used by Win 31 timing routines.
** NOTE: This requires the includes of the w31timer.asm
** file in your project!!
*/
#ifdef WIN31TIMER
#include <windows.h>
#include <toolhelp.h>
extern TIMERINFO win31tinfo;
extern HANDLE hThlp;
extern FARPROC lpfn;
#endif

/*
** PROTOTYPES
*/
static int parse_arg(char *argptr);
static void display_help(char *progname);
static void read_comfile(FILE *cfile);
static int getflag(char *cptr);
static void strtoupper(char *s);
static void set_request_secs(void);
static int bench_with_confidence(int fid,
        double *mean, double *stdev, ulong *numtries);
/*
static int seek_confidence(double scores[5],
        double *newscore, double *c_half_interval,
        double *smean,double *sdev);
*/
static int calc_confidence(double scores[],
        int num_scores,
        double *c_half_interval,double *smean,
        double *sdev);
static double getscore(int fid);
static void output_string(char *buffer);
static void show_stats(int bid);

#ifdef MAC
void UCommandLine(void);
void UParse(void);
unsigned char *UField(unsigned char *ptr);
#endif

/*
** EXTERNAL PROTOTYPES
*/
extern void DoNumSort(void);    /* From NBENCH1 */
extern void DoStringSort(void);
extern void DoBitops(void);
extern void DoEmFloat(void);
extern void DoFourier(void);
extern void DoAssign(void);
extern void DoIDEA(void);
extern void DoHuffman(void);
extern void DoNNET(void);
extern void DoLU(void);

extern void ErrorExit(void);    /* From SYSSPEC */

/*
** Array of pointers to the benchmark functions.
*/
void (*funcpointer[])(void) =
{       DoNumSort,
        DoStringSort,
        DoBitops,
        DoEmFloat,
        DoFourier,
        DoAssign,
        DoIDEA,
        DoHuffman,
        DoNNET,
        DoLU };


