/*
** nbench1.h
** Header for nbench1.c
** BYTEmark (tm)
** BYTE's Native Mode Benchmarks
** Rick Grehan, BYTE Magazine
**
** Creation:
** Revision: 3/95;10/95
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
** DEFINES
*/
/* #define DEBUG */

/*
** EXTERNALS
*/
extern ulong global_min_ticks;

extern SortStruct global_numsortstruct;
extern SortStruct global_strsortstruct;
extern BitOpStruct global_bitopstruct;
extern EmFloatStruct global_emfloatstruct;
extern FourierStruct global_fourierstruct;
extern AssignStruct global_assignstruct;
extern IDEAStruct global_ideastruct;
extern HuffStruct global_huffstruct;
extern NNetStruct global_nnetstruct;
extern LUStruct global_lustruct;

/* External PROTOTYPES */
/*extern unsigned long abs_randwc(unsigned long num);*/     /* From MISC */
/*extern long randnum(long lngval);*/
extern int32 randwc(int32 num);
extern u32 abs_randwc(u32 num);
extern int32 randnum(int32 lngval);

extern farvoid *AllocateMemory(unsigned long nbytes,    /* From SYSSPEC */
	int *errorcode);
extern void FreeMemory(farvoid *mempointer,
	int *errorcode);
extern void MoveMemory(farvoid *destination,
		farvoid *source, unsigned long nbytes);
extern void ReportError(char *context, int errorcode);
extern void ErrorExit();
extern unsigned long StartStopwatch();
extern unsigned long StopStopwatch(unsigned long startticks);
extern unsigned long TicksToSecs(unsigned long tickamount);
extern double TicksToFracSecs(unsigned long tickamount);

/*****************
** NUMERIC SORT **
*****************/

/*
** PROTOTYPES
*/
void DoNumSort(void);
static ulong DoNumSortIteration(farlong *arraybase,
		ulong arraysize,
		uint numarrays);
static void LoadNumArrayWithRand(farlong *array,
		ulong arraysize,
		uint numarrays);
static void NumHeapSort(farlong *array,
		ulong bottom,
		ulong top);
static void NumSift(farlong *array,
		ulong i,
		ulong j);


/****************
** STRING SORT **
*****************
*/


/*
** PROTOTYPES
*/
void DoStringSort(void);
static ulong DoStringSortIteration(faruchar *arraybase,
		uint numarrays,
		ulong arraysize);
static farulong *LoadStringArray(faruchar *strarray,
		uint numarrays,
		ulong *strings,
		ulong arraysize);
static void stradjust(farulong *optrarray,
		faruchar *strarray,
		ulong nstrings,
		ulong i,
		uchar l);
static void StrHeapSort(farulong *optrarray,
		faruchar *strarray,
		ulong numstrings,
		ulong bottom,
		ulong top);
static int str_is_less(farulong *optrarray,
		faruchar *strarray,
		ulong numstrings,
		ulong a,
		ulong b);
static void strsift(farulong *optrarray,
		faruchar *strarray,
		ulong numstrings,
		ulong i,
		ulong j);

/************************
** BITFIELD OPERATIONS **
*************************
*/

/*
** PROTOTYPES
*/
void DoBitops(void);
static ulong DoBitfieldIteration(farulong *bitarraybase,
		farulong *bitoparraybase,
		long bitoparraysize,
		ulong *nbitops);
static void ToggleBitRun(farulong *bitmap,
		ulong bit_addr,
		ulong nbits,
		uint val);
static void FlipBitRun(farulong *bitmap,
		ulong bit_addr,
		ulong nbits);

/****************************
** EMULATED FLOATING POINT **
****************************/
typedef struct
{
	u8 type;        /* Indicates, NORMAL, SUBNORMAL, etc. */
	u8 sign;        /* Mantissa sign */
	short exp;      /* Signed exponent...no bias */
	u16 mantissa[INTERNAL_FPF_PRECISION];
} InternalFPF;

/*
** PROTOTYPES
*/
void DoEmFloat(void);

/*
** EXTERNALS
*/
extern void SetupCPUEmFloatArrays(InternalFPF *abase,
	InternalFPF *bbase, InternalFPF *cbase,
	ulong arraysize);
extern ulong DoEmFloatIteration(InternalFPF *abase,
	InternalFPF *bbase, InternalFPF *cbase,
	ulong arraysize, ulong loops);

/*************************
** FOURIER COEFFICIENTS **
*************************/

/*
** PROTOTYPES
*/
void DoFourier(void);
static ulong DoFPUTransIteration(fardouble *abase,
		fardouble *bbase,
		ulong arraysize);
static double TrapezoidIntegrate(double x0,
		double x1,
		int nsteps,
		double omegan,
		int select);
static double thefunction(double x,
		double omegan,
		int select);

/*************************
** ASSIGNMENT ALGORITHM **
*************************/

/*
** DEFINES
*/

#define ASSIGNROWS 101L
#define ASSIGNCOLS 101L

/*
** TYPEDEFS
*/
typedef struct {
	union {
		long *p;
		long (*ap)[ASSIGNROWS][ASSIGNCOLS];
	} ptrs;
} longptr;

/*
** PROTOTYPES
*/
void DoAssign(void);
static ulong DoAssignIteration(farlong *arraybase,
		ulong numarrays);
static void LoadAssignArrayWithRand(farlong *arraybase,
		ulong numarrays);
static void LoadAssign(farlong arraybase[][ASSIGNCOLS]);
static void CopyToAssign(farlong arrayfrom[][ASSIGNCOLS],
		long arrayto[][ASSIGNCOLS]);
static void Assignment(farlong arraybase[][ASSIGNCOLS]);
static void calc_minimum_costs(long tableau[][ASSIGNCOLS]);
static int first_assignments(long tableau[][ASSIGNCOLS],
		short assignedtableau[][ASSIGNCOLS]);
static void second_assignments(long tableau[][ASSIGNCOLS],
		short assignedtableau[][ASSIGNCOLS]);

/********************
** IDEA ENCRYPTION **
********************/

/*
** DEFINES
*/
#define IDEAKEYSIZE 16
#define IDEABLOCKSIZE 8
#define ROUNDS 8
#define KEYLEN (6*ROUNDS+4)

/*
** MACROS
*/
#define low16(x) ((x) & 0x0FFFF)
#define MUL(x,y) (x=mul(low16(x),y))


typedef u16 IDEAkey[KEYLEN];

/*
** PROTOTYPES
*/
void DoIDEA(void);
static ulong DoIDEAIteration(faruchar *plain1,
	faruchar *crypt1, faruchar *plain2,
	ulong arraysize, ulong nloops,
	IDEAkey Z, IDEAkey DK);
static u16 mul(register u16 a, register u16 b);
static u16 inv(u16 x);
static void en_key_idea(u16 userkey[8], IDEAkey Z);
static void de_key_idea(IDEAkey Z, IDEAkey DK);
static void cipher_idea(u16 in[4], u16 out[4], IDEAkey Z);

/************************
** HUFFMAN COMPRESSION **
************************/

/*
** DEFINES
*/
#define EXCLUDED 32000L          /* Big positive value */

/*
** TYPEDEFS
*/
typedef struct {
	uchar c;                /* Byte value */
	float freq;             /* Frequency */
	int parent;             /* Parent node */
	int left;               /* Left pointer = 0 */
	int right;              /* Right pointer = 1 */
} huff_node;

/*
** GLOBALS
*/
static huff_node *hufftree;             /* The huffman tree */
static long plaintextlen;               /* Length of plaintext */

/*
** PROTOTYPES
*/
void DoHuffman();
static void create_text_line(farchar *dt,long nchars);
static void create_text_block(farchar *tb, ulong tblen,
		ushort maxlinlen);
static ulong DoHuffIteration(farchar *plaintext,
	farchar *comparray, farchar *decomparray,
	ulong arraysize, ulong nloops, huff_node *hufftree);
static void SetCompBit(u8 *comparray, u32 bitoffset, char bitchar);
static int GetCompBit(u8 *comparray, u32 bitoffset);

/********************************
** BACK PROPAGATION NEURAL NET **
********************************/

/*
** DEFINES
*/
#define T 1                     /* TRUE */
#define F 0                     /* FALSE */
#define ERR -1
#define MAXPATS 10              /* max number of patterns in data file */
#define IN_X_SIZE 5             /* number of neurodes/row of input layer */
#define IN_Y_SIZE 7             /* number of neurodes/col of input layer */
#define IN_SIZE 35              /* equals IN_X_SIZE*IN_Y_SIZE */
#define MID_SIZE 8              /* number of neurodes in middle layer */
#define OUT_SIZE 8              /* number of neurodes in output layer */
#define MARGIN 0.1              /* how near to 1,0 do we have to come to stop? */
#define BETA 0.09               /* beta learning constant */
#define ALPHA 0.09              /* momentum term constant */
#define STOP 0.1                /* when worst_error less than STOP, training is done */

/*
** GLOBALS
*/
double  mid_wts[MID_SIZE][IN_SIZE];     /* middle layer weights */
double  out_wts[OUT_SIZE][MID_SIZE];    /* output layer weights */
double  mid_out[MID_SIZE];              /* middle layer output */
double  out_out[OUT_SIZE];              /* output layer output */
double  mid_error[MID_SIZE];            /* middle layer errors */
double  out_error[OUT_SIZE];            /* output layer errors */
double  mid_wt_change[MID_SIZE][IN_SIZE]; /* storage for last wt change */
double  out_wt_change[OUT_SIZE][MID_SIZE]; /* storage for last wt change */
double  in_pats[MAXPATS][IN_SIZE];      /* input patterns */
double  out_pats[MAXPATS][OUT_SIZE];    /* desired output patterns */
double  tot_out_error[MAXPATS];         /* measure of whether net is done */
double  out_wt_cum_change[OUT_SIZE][MID_SIZE]; /* accumulated wt changes */
double  mid_wt_cum_change[MID_SIZE][IN_SIZE];  /* accumulated wt changes */

double  worst_error; /* worst error each pass through the data */
double  average_error; /* average error each pass through the data */
double  avg_out_error[MAXPATS]; /* average error each pattern */

int iteration_count;    /* number of passes thru network so far */
int numpats;            /* number of patterns in data file */
int numpasses;          /* number of training passes through data file */
int learned;            /* flag--if TRUE, network has learned all patterns */

/*
** The Neural Net test requires an input data file.
** The name is specified here.
*/
char *inpath="NNET.DAT";

/*
** PROTOTYPES
*/
void DoNNET(void);
static ulong DoNNetIteration(ulong nloops);
static void do_mid_forward(int patt);
static void do_out_forward();
void display_output(int patt);
static void do_forward_pass(int patt);
static void do_out_error(int patt);
static void worst_pass_error();
static void do_mid_error();
static void adjust_out_wts();
static void adjust_mid_wts();
static void do_back_pass(int patt);
static void move_wt_changes();
static int check_out_error();
static void zero_changes();
static void randomize_wts();
static int read_data_file();
/* static int initialize_net(); */

/***********************
**  LU DECOMPOSITION  **
** (Linear Equations) **
***********************/

/*
** DEFINES
*/

#define LUARRAYROWS 101L
#define LUARRAYCOLS 101L

/*
** TYPEDEFS
*/
typedef struct
{       union
	{       fardouble *p;
		fardouble (*ap)[][LUARRAYCOLS];
	} ptrs;
} LUdblptr;

/*
** GLOBALS
*/
fardouble *LUtempvv;

/*
** PROTOTYPES
*/
void DoLU(void);
static void LUFreeMem(fardouble *a, fardouble *b,
	fardouble *abase, fardouble *bbase);
static ulong DoLUIteration(fardouble *a, fardouble *b,
	fardouble *abase, fardouble *bbase,
	ulong numarrays);
static void build_problem( double a[][LUARRAYCOLS],
	int n, double b[LUARRAYROWS]);
static int ludcmp(double a[][LUARRAYCOLS],
	int n, int indx[], int *d);
static void lubksb(double a[][LUARRAYCOLS],
	int n, int indx[LUARRAYROWS],
	double b[LUARRAYROWS]);
static int lusolve(double a[][LUARRAYCOLS],
	int n, double b[LUARRAYROWS]);


