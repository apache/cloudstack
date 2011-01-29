
/*
** nbench1.c
*/

/********************************
**       BYTEmark (tm)         **
** BYTE NATIVE MODE BENCHMARKS **
**       VERSION 2             **
**                             **
** Included in this source     **
** file:                       **
**  Numeric Heapsort           **
**  String Heapsort            **
**  Bitfield test              **
**  Floating point emulation   **
**  Fourier coefficients       **
**  Assignment algorithm       **
**  IDEA Encyption             **
**  Huffman compression        **
**  Back prop. neural net      **
**  LU Decomposition           **
**    (linear equations)       **
** ----------                  **
** Rick Grehan, BYTE Magazine  **
*********************************
**
** BYTEmark (tm)
** BYTE's Native Mode Benchmarks
** Rick Grehan, BYTE Magazine
**
** Creation:
** Revision: 3/95;10/95
**  10/95 - Removed allocation that was taking place inside
**   the LU Decomposition benchmark. Though it didn't seem to
**   make a difference on systems we ran it on, it nonetheless
**   removes an operating system dependency that probably should
**   not have been there.
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
** INCLUDES
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <math.h>
#include "nmglobal.h"
#include "nbench1.h"
#include "wordcat.h"

#ifdef DEBUG
static int numsort_status=0;
static int stringsort_status=0;
#endif

/*********************
** NUMERIC HEAPSORT **
**********************
** This test implements a heapsort algorithm, performed on an
** array of longs.
*/

/**************
** DoNumSort **
***************
** This routine performs the CPU numeric sort test.
** NOTE: Last version incorrectly stated that the routine
**  returned result in # of longword sorted per second.
**  Not so; the routine returns # of iterations per sec.
*/

void DoNumSort(void)
{
SortStruct *numsortstruct;      /* Local pointer to global struct */
farlong *arraybase;     /* Base pointers of array */
long accumtime;         /* Accumulated time */
double iterations;      /* Iteration counter */
char *errorcontext;     /* Error context string pointer */
int systemerror;        /* For holding error codes */

/*
** Link to global structure
*/
numsortstruct=&global_numsortstruct;

/*
** Set the error context string.
*/
errorcontext="CPU:Numeric Sort";

/*
** See if we need to do self adjustment code.
*/
if(numsortstruct->adjust==0)
{
	/*
	** Self-adjustment code.  The system begins by sorting 1
	** array.  If it does that in no time, then two arrays
	** are built and sorted.  This process continues until
	** enough arrays are built to handle the tolerance.
	*/
	numsortstruct->numarrays=1;
	while(1)
	{
		/*
		** Allocate space for arrays
		*/
		arraybase=(farlong *)AllocateMemory(sizeof(long) *
			numsortstruct->numarrays * numsortstruct->arraysize,
			&systemerror);
		if(systemerror)
		{       ReportError(errorcontext,systemerror);
			FreeMemory((farvoid *)arraybase,
				  &systemerror);
			ErrorExit();
		}

		/*
		** Do an iteration of the numeric sort.  If the
		** elapsed time is less than or equal to the permitted
		** minimum, then allocate for more arrays and
		** try again.
		*/
		if(DoNumSortIteration(arraybase,
			numsortstruct->arraysize,
			numsortstruct->numarrays)>global_min_ticks)
			break;          /* We're ok...exit */

		FreeMemory((farvoid *)arraybase,&systemerror);
		if(numsortstruct->numarrays++>NUMNUMARRAYS)
		{       printf("CPU:NSORT -- NUMNUMARRAYS hit.\n");
			ErrorExit();
		}
	}
}
else
{       /*
	** Allocate space for arrays
	*/
	arraybase=(farlong *)AllocateMemory(sizeof(long) *
		numsortstruct->numarrays * numsortstruct->arraysize,
		&systemerror);
	if(systemerror)
	{       ReportError(errorcontext,systemerror);
		FreeMemory((farvoid *)arraybase,
			  &systemerror);
		ErrorExit();
	}

}
/*
** All's well if we get here.  Repeatedly perform sorts until the
** accumulated elapsed time is greater than # of seconds requested.
*/
accumtime=0L;
iterations=(double)0.0;

do {
	accumtime+=DoNumSortIteration(arraybase,
		numsortstruct->arraysize,
		numsortstruct->numarrays);
	iterations+=(double)1.0;
} while(TicksToSecs(accumtime)<numsortstruct->request_secs);

/*
** Clean up, calculate results, and go home.  Be sure to
** show that we don't have to rerun adjustment code.
*/
FreeMemory((farvoid *)arraybase,&systemerror);

numsortstruct->sortspersec=iterations *
	(double)numsortstruct->numarrays / TicksToFracSecs(accumtime);

if(numsortstruct->adjust==0)
	numsortstruct->adjust=1;

#ifdef DEBUG
if (numsort_status==0) printf("Numeric sort: OK\n");
numsort_status=0;
#endif
return;
}

/***********************
** DoNumSortIteration **
************************
** This routine executes one iteration of the numeric
** sort benchmark.  It returns the number of ticks
** elapsed for the iteration.
*/
static ulong DoNumSortIteration(farlong *arraybase,
		ulong arraysize,
		uint numarrays)
{
ulong elapsed;          /* Elapsed ticks */
ulong i;
/*
** Load up the array with random numbers
*/
LoadNumArrayWithRand(arraybase,arraysize,numarrays);

/*
** Start the stopwatch
*/
elapsed=StartStopwatch();

/*
** Execute a heap of heapsorts
*/
for(i=0;i<numarrays;i++)
	NumHeapSort(arraybase+i*arraysize,0L,arraysize-1L);

/*
** Get elapsed time
*/
elapsed=StopStopwatch(elapsed);
#ifdef DEBUG
{
	for(i=0;i<arraysize-1;i++)
	{       /*
		** Compare to check for proper
		** sort.
		*/
		if(arraybase[i+1]<arraybase[i])
		{       printf("Sort Error\n");
			numsort_status=1;
                        break;
		}
	}
}
#endif

return(elapsed);
}

/*************************
** LoadNumArrayWithRand **
**************************
** Load up an array with random longs.
*/
static void LoadNumArrayWithRand(farlong *array,     /* Pointer to arrays */
		ulong arraysize,
		uint numarrays)         /* # of elements in array */
{
long i;                 /* Used for index */
farlong *darray;        /* Destination array pointer */
/*
** Initialize the random number generator
*/
/* randnum(13L); */
randnum((int32)13);

/*
** Load up first array with randoms
*/
for(i=0L;i<arraysize;i++)
        /* array[i]=randnum(0L); */
	array[i]=randnum((int32)0);

/*
** Now, if there's more than one array to load, copy the
** first into each of the others.
*/
darray=array;
while(--numarrays)
{       darray+=arraysize;
	for(i=0L;i<arraysize;i++)
		darray[i]=array[i];
}

return;
}

/****************
** NumHeapSort **
*****************
** Pass this routine a pointer to an array of long
** integers.  Also pass in minimum and maximum offsets.
** This routine performs a heap sort on that array.
*/
static void NumHeapSort(farlong *array,
	ulong bottom,           /* Lower bound */
	ulong top)              /* Upper bound */
{
ulong temp;                     /* Used to exchange elements */
ulong i;                        /* Loop index */

/*
** First, build a heap in the array
*/
for(i=(top/2L); i>0; --i)
	NumSift(array,i,top);

/*
** Repeatedly extract maximum from heap and place it at the
** end of the array.  When we get done, we'll have a sorted
** array.
*/
for(i=top; i>0; --i)
{       NumSift(array,bottom,i);
	temp=*array;                    /* Perform exchange */
	*array=*(array+i);
	*(array+i)=temp;
}
return;
}

/************
** NumSift **
*************
** Peforms the sift operation on a numeric array,
** constructing a heap in the array.
*/
static void NumSift(farlong *array,     /* Array of numbers */
	ulong i,                /* Minimum of array */
	ulong j)                /* Maximum of array */
{
unsigned long k;
long temp;                              /* Used for exchange */

while((i+i)<=j)
{
	k=i+i;
	if(k<j)
		if(array[k]<array[k+1L])
			++k;
	if(array[i]<array[k])
	{
		temp=array[k];
		array[k]=array[i];
		array[i]=temp;
		i=k;
	}
	else
		i=j+1;
}
return;
}

/********************
** STRING HEAPSORT **
********************/

/*****************
** DoStringSort **
******************
** This routine performs the CPU string sort test.
** Arguments:
**      requested_secs = # of seconds to execute test
**      stringspersec = # of strings per second sorted (RETURNED)
*/
void DoStringSort(void)
{

SortStruct *strsortstruct;      /* Local for sort structure */
faruchar *arraybase;            /* Base pointer of char array */
long accumtime;                 /* Accumulated time */
double iterations;              /* # of iterations */
char *errorcontext;             /* Error context string pointer */
int systemerror;                /* For holding error code */

/*
** Link to global structure
*/
strsortstruct=&global_strsortstruct;

/*
** Set the error context
*/
errorcontext="CPU:String Sort";

/*
** See if we have to perform self-adjustment code
*/
if(strsortstruct->adjust==0)
{
	/*
	** Initialize the number of arrays.
	*/
	strsortstruct->numarrays=1;
	while(1)
	{
		/*
		** Allocate space for array.  We'll add an extra 100
		** bytes to protect memory as strings move around
		** (this can happen during string adjustment)
		*/
		arraybase=(faruchar *)AllocateMemory((strsortstruct->arraysize+100L) *
			(long)strsortstruct->numarrays,&systemerror);
		if(systemerror)
		{       ReportError(errorcontext,systemerror);
			ErrorExit();
		}

		/*
		** Do an iteration of the string sort.  If the
		** elapsed time is less than or equal to the permitted
		** minimum, then de-allocate the array, reallocate a
		** an additional array, and try again.
		*/
		if(DoStringSortIteration(arraybase,
			strsortstruct->numarrays,
			strsortstruct->arraysize)>global_min_ticks)
			break;          /* We're ok...exit */

		FreeMemory((farvoid *)arraybase,&systemerror);
		strsortstruct->numarrays+=1;
	}
}
else
{
	/*
	** We don't have to perform self adjustment code.
	** Simply allocate the space for the array.
	*/
	arraybase=(faruchar *)AllocateMemory((strsortstruct->arraysize+100L) *
		(long)strsortstruct->numarrays,&systemerror);
	if(systemerror)
	{       ReportError(errorcontext,systemerror);
		ErrorExit();
	}
}
/*
** All's well if we get here.  Repeatedly perform sorts until the
** accumulated elapsed time is greater than # of seconds requested.
*/
accumtime=0L;
iterations=(double)0.0;

do {
	accumtime+=DoStringSortIteration(arraybase,
				strsortstruct->numarrays,
				strsortstruct->arraysize);
	iterations+=(double)strsortstruct->numarrays;
} while(TicksToSecs(accumtime)<strsortstruct->request_secs);

/*
** Clean up, calculate results, and go home.
** Set flag to show we don't need to rerun adjustment code.
*/
FreeMemory((farvoid *)arraybase,&systemerror);
strsortstruct->sortspersec=iterations / (double)TicksToFracSecs(accumtime);
if(strsortstruct->adjust==0)
	strsortstruct->adjust=1;
#ifdef DEBUG
if (stringsort_status==0) printf("String sort: OK\n");
stringsort_status=0;
#endif
return;
}

/**************************
** DoStringSortIteration **
***************************
** This routine executes one iteration of the string
** sort benchmark.  It returns the number of ticks
** Note that this routine also builds the offset pointer
** array.
*/
static ulong DoStringSortIteration(faruchar *arraybase,
		uint numarrays,ulong arraysize)
{
farulong *optrarray;            /* Offset pointer array */
unsigned long elapsed;          /* Elapsed ticks */
unsigned long nstrings;         /* # of strings in array */
int syserror;                   /* System error code */
unsigned int i;                 /* Index */
farulong *tempobase;            /* Temporary offset pointer base */
faruchar *tempsbase;            /* Temporary string base pointer */

/*
** Load up the array(s) with random numbers
*/
optrarray=LoadStringArray(arraybase,numarrays,&nstrings,arraysize);

/*
** Set temp base pointers...they will be modified as the
** benchmark proceeds.
*/
tempobase=optrarray;
tempsbase=arraybase;

/*
** Start the stopwatch
*/
elapsed=StartStopwatch();

/*
** Execute heapsorts
*/
for(i=0;i<numarrays;i++)
{       StrHeapSort(tempobase,tempsbase,nstrings,0L,nstrings-1);
	tempobase+=nstrings;    /* Advance base pointers */
	tempsbase+=arraysize+100;
}

/*
** Record elapsed time
*/
elapsed=StopStopwatch(elapsed);

#ifdef DEBUG
{
	unsigned long i;
	for(i=0;i<nstrings-1;i++)
	{       /*
		** Compare strings to check for proper
		** sort.
		*/
		if(str_is_less(optrarray,arraybase,nstrings,i+1,i))
		{       printf("Sort Error\n");
			stringsort_status=1;
                        break;
		}
	}
}
#endif

/*
** Release the offset pointer array built by
** LoadStringArray()
*/
FreeMemory((farvoid *)optrarray,&syserror);

/*
** Return elapsed ticks.
*/
return(elapsed);
}

/********************
** LoadStringArray **
*********************
** Initialize the string array with random strings of
** varying sizes.
** Returns the pointer to the offset pointer array.
** Note that since we're creating a number of arrays, this
** routine builds one array, then copies it into the others.
*/
static farulong *LoadStringArray(faruchar *strarray, /* String array */
	uint numarrays,                 /* # of arrays */
	ulong *nstrings,                /* # of strings */
	ulong arraysize)                /* Size of array */
{
faruchar *tempsbase;            /* Temporary string base pointer */
farulong *optrarray;            /* Local for pointer */
farulong *tempobase;            /* Temporary offset pointer base pointer */
unsigned long curroffset;       /* Current offset */
int fullflag;                   /* Indicates full array */
unsigned char stringlength;     /* Length of string */
unsigned char i;                /* Index */
unsigned long j;                /* Another index */
unsigned int k;                 /* Yet another index */
unsigned int l;                 /* Ans still one more index */
int systemerror;                /* For holding error code */

/*
** Initialize random number generator.
*/
/* randnum(13L); */
randnum((int32)13);

/*
** Start with no strings.  Initialize our current offset pointer
** to 0.
*/
*nstrings=0L;
curroffset=0L;
fullflag=0;

do
{
	/*
	** Allocate a string with a random length no
	** shorter than 4 bytes and no longer than
	** 80 bytes.  Note we have to also make sure
	** there's room in the array.
	*/
        /* stringlength=(unsigned char)((1+abs_randwc(76L)) & 0xFFL);*/
	stringlength=(unsigned char)((1+abs_randwc((int32)76)) & 0xFFL);
	if((unsigned long)stringlength+curroffset+1L>=arraysize)
	{       stringlength=(unsigned char)((arraysize-curroffset-1L) &
				0xFF);
		fullflag=1;     /* Indicates a full */
	}

	/*
	** Store length at curroffset and advance current offset.
	*/
	*(strarray+curroffset)=stringlength;
	curroffset++;

	/*
	** Fill up the rest of the string with random bytes.
	*/
	for(i=0;i<stringlength;i++)
	{       *(strarray+curroffset)=
		        /* (unsigned char)(abs_randwc((long)0xFE)); */
			(unsigned char)(abs_randwc((int32)0xFE));
		curroffset++;
	}

	/*
	** Increment the # of strings counter.
	*/
	*nstrings+=1L;

} while(fullflag==0);

/*
** We now have initialized a single full array.  If there
** is more than one array, copy the original into the
** others.
*/
k=1;
tempsbase=strarray;
while(k<numarrays)
{       tempsbase+=arraysize+100;         /* Set base */
	for(l=0;l<arraysize;l++)
		tempsbase[l]=strarray[l];
	k++;
}

/*
** Now the array is full, allocate enough space for an
** offset pointer array.
*/
optrarray=(farulong *)AllocateMemory(*nstrings * sizeof(unsigned long) *
		numarrays,
		&systemerror);
if(systemerror)
{       ReportError("CPU:Stringsort",systemerror);
	FreeMemory((void *)strarray,&systemerror);
	ErrorExit();
}

/*
** Go through the newly-built string array, building
** offsets and putting them into the offset pointer
** array.
*/
curroffset=0;
for(j=0;j<*nstrings;j++)
{       *(optrarray+j)=curroffset;
	curroffset+=(unsigned long)(*(strarray+curroffset))+1L;
}

/*
** As above, we've made one copy of the offset pointers,
** so duplicate this array in the remaining ones.
*/
k=1;
tempobase=optrarray;
while(k<numarrays)
{       tempobase+=*nstrings;
	for(l=0;l<*nstrings;l++)
		tempobase[l]=optrarray[l];
	k++;
}

/*
** All done...go home.  Pass local pointer back.
*/
return(optrarray);
}

/**************
** stradjust **
***************
** Used by the string heap sort.  Call this routine to adjust the
** string at offset i to length l.  The members of the string array
** are moved accordingly and the length of the string at offset i
** is set to l.
*/
static void stradjust(farulong *optrarray,      /* Offset pointer array */
	faruchar *strarray,                     /* String array */
	ulong nstrings,                         /* # of strings */
	ulong i,                                /* Offset to adjust */
	uchar l)                                /* New length */
{
unsigned long nbytes;           /* # of bytes to move */
unsigned long j;                /* Index */
int direction;                  /* Direction indicator */
unsigned char adjamount;        /* Adjustment amount */

/*
** If new length is less than old length, the direction is
** down.  If new length is greater than old length, the
** direction is up.
*/
direction=(int)l - (int)*(strarray+*(optrarray+i));
adjamount=(unsigned char)abs(direction);

/*
** See if the adjustment is being made to the last
** string in the string array.  If so, we don't have to
** do anything more than adjust the length field.
*/
if(i==(nstrings-1L))
{       *(strarray+*(optrarray+i))=l;
	return;
}

/*
** Calculate the total # of bytes in string array from
** location i+1 to end of array.  Whether we're moving "up" or
** down, this is how many bytes we'll have to move.
*/
nbytes=*(optrarray+nstrings-1L) +
	(unsigned long)*(strarray+*(optrarray+nstrings-1L)) + 1L -
	*(optrarray+i+1L);

/*
** Calculate the source and the destination.  Source is
** string position i+1.  Destination is string position i+l
** (i+"ell"...don't confuse 1 and l).
** Hand this straight to memmove and let it handle the
** "overlap" problem.
*/
MoveMemory((farvoid *)(strarray+*(optrarray+i)+l+1),
	(farvoid *)(strarray+*(optrarray+i+1)),
	(unsigned long)nbytes);

/*
** We have to adjust the offset pointer array.
** This covers string i+1 to numstrings-1.
*/
for(j=i+1;j<nstrings;j++)
	if(direction<0)
		*(optrarray+j)=*(optrarray+j)-adjamount;
	else
		*(optrarray+j)=*(optrarray+j)+adjamount;

/*
** Store the new length and go home.
*/
*(strarray+*(optrarray+i))=l;
return;
}

/****************
** strheapsort **
*****************
** Pass this routine a pointer to an array of unsigned char.
** The array is presumed to hold strings occupying at most
** 80 bytes (counts a byte count).
** This routine also needs a pointer to an array of offsets
** which represent string locations in the array, and
** an unsigned long indicating the number of strings
** in the array.
*/
static void StrHeapSort(farulong *optrarray, /* Offset pointers */
	faruchar *strarray,             /* Strings array */
	ulong numstrings,               /* # of strings in array */
	ulong bottom,                   /* Region to sort...bottom */
	ulong top)                      /* Region to sort...top */
{
unsigned char temp[80];                 /* Used to exchange elements */
unsigned char tlen;                     /* Temp to hold length */
unsigned long i;                        /* Loop index */


/*
** Build a heap in the array
*/
for(i=(top/2L); i>0; --i)
	strsift(optrarray,strarray,numstrings,i,top);

/*
** Repeatedly extract maximum from heap and place it at the
** end of the array.  When we get done, we'll have a sorted
** array.
*/
for(i=top; i>0; --i)
{
	strsift(optrarray,strarray,numstrings,0,i);

	/* temp = string[0] */
	tlen=*strarray;
	MoveMemory((farvoid *)&temp[0], /* Perform exchange */
		(farvoid *)strarray,
		(unsigned long)(tlen+1));


	/* string[0]=string[i] */
	tlen=*(strarray+*(optrarray+i));
	stradjust(optrarray,strarray,numstrings,0,tlen);
	MoveMemory((farvoid *)strarray,
		(farvoid *)(strarray+*(optrarray+i)),
		(unsigned long)(tlen+1));

	/* string[i]=temp */
	tlen=temp[0];
	stradjust(optrarray,strarray,numstrings,i,tlen);
	MoveMemory((farvoid *)(strarray+*(optrarray+i)),
		(farvoid *)&temp[0],
		(unsigned long)(tlen+1));

}
return;
}

/****************
** str_is_less **
*****************
** Pass this function:
**      1) A pointer to an array of offset pointers
**      2) A pointer to a string array
**      3) The number of elements in the string array
**      4) Offsets to two strings (a & b)
** This function returns TRUE if string a is < string b.
*/
static int str_is_less(farulong *optrarray, /* Offset pointers */
	faruchar *strarray,                     /* String array */
	ulong numstrings,                       /* # of strings */
	ulong a, ulong b)                       /* Offsets */
{
int slen;               /* String length */

/*
** Determine which string has the minimum length.  Use that
** to call strncmp().  If they match up to that point, the
** string with the longer length wins.
*/
slen=(int)*(strarray+*(optrarray+a));
if(slen > (int)*(strarray+*(optrarray+b)))
	slen=(int)*(strarray+*(optrarray+b));

slen=strncmp((char *)(strarray+*(optrarray+a)),
		(char *)(strarray+*(optrarray+b)),slen);

if(slen==0)
{
	/*
	** They match.  Return true if the length of a
	** is greater than the length of b.
	*/
	if(*(strarray+*(optrarray+a)) >
		*(strarray+*(optrarray+b)))
		return(TRUE);
	return(FALSE);
}

if(slen<0) return(TRUE);        /* a is strictly less than b */

return(FALSE);                  /* Only other possibility */
}

/************
** strsift **
*************
** Pass this function:
**      1) A pointer to an array of offset pointers
**      2) A pointer to a string array
**      3) The number of elements in the string array
**      4) Offset within which to sort.
** Sift the array within the bounds of those offsets (thus
** building a heap).
*/
static void strsift(farulong *optrarray,        /* Offset pointers */
	faruchar *strarray,                     /* String array */
	ulong numstrings,                       /* # of strings */
	ulong i, ulong j)                       /* Offsets */
{
unsigned long k;                /* Temporaries */
unsigned char temp[80];
unsigned char tlen;             /* For string lengths */


while((i+i)<=j)
{
	k=i+i;
	if(k<j)
		if(str_is_less(optrarray,strarray,numstrings,k,k+1L))
			++k;
	if(str_is_less(optrarray,strarray,numstrings,i,k))
	{
		/* temp=string[k] */
		tlen=*(strarray+*(optrarray+k));
		MoveMemory((farvoid *)&temp[0],
			(farvoid *)(strarray+*(optrarray+k)),
			(unsigned long)(tlen+1));

		/* string[k]=string[i] */
		tlen=*(strarray+*(optrarray+i));
		stradjust(optrarray,strarray,numstrings,k,tlen);
		MoveMemory((farvoid *)(strarray+*(optrarray+k)),
			(farvoid *)(strarray+*(optrarray+i)),
			(unsigned long)(tlen+1));

		/* string[i]=temp */
		tlen=temp[0];
		stradjust(optrarray,strarray,numstrings,i,tlen);
		MoveMemory((farvoid *)(strarray+*(optrarray+i)),
			(farvoid *)&temp[0],
			(unsigned long)(tlen+1));
		i=k;
	}
	else
		i=j+1;
}
return;
}

/************************
** BITFIELD OPERATIONS **
*************************/

/*************
** DoBitops **
**************
** Perform the bit operations test portion of the CPU
** benchmark.  Returns the iterations per second.
*/
void DoBitops(void)
{
BitOpStruct *locbitopstruct;    /* Local bitop structure */
farulong *bitarraybase;         /* Base of bitmap array */
farulong *bitoparraybase;       /* Base of bitmap operations array */
ulong nbitops;                  /* # of bitfield operations */
ulong accumtime;                /* Accumulated time in ticks */
double iterations;              /* # of iterations */
char *errorcontext;             /* Error context string */
int systemerror;                /* For holding error codes */
int ticks;

/*
** Link to global structure.
*/
locbitopstruct=&global_bitopstruct;

/*
** Set the error context.
*/
errorcontext="CPU:Bitfields";

/*
** See if we need to run adjustment code.
*/
if(locbitopstruct->adjust==0)
{
	bitarraybase=(farulong *)AllocateMemory(locbitopstruct->bitfieldarraysize *
		sizeof(ulong),&systemerror);
	if(systemerror)
	{       ReportError(errorcontext,systemerror);
		ErrorExit();
	}

	/*
	** Initialize bitfield operations array to [2,30] elements
	*/
	locbitopstruct->bitoparraysize=30L;

	while(1)
	{
		/*
		** Allocate space for operations array
		*/
		bitoparraybase=(farulong *)AllocateMemory(locbitopstruct->bitoparraysize*2L*
			sizeof(ulong),
			&systemerror);
		if(systemerror)
		{       ReportError(errorcontext,systemerror);
			FreeMemory((farvoid *)bitarraybase,&systemerror);
			ErrorExit();
		}
		/*
		** Do an iteration of the bitmap test.  If the
		** elapsed time is less than or equal to the permitted
		** minimum, then de-allocate the array, reallocate a
		** larger version, and try again.
		*/
		ticks=DoBitfieldIteration(bitarraybase,
					   bitoparraybase,
					   locbitopstruct->bitoparraysize,
					   &nbitops);
#ifdef DEBUG
#ifdef LINUX
	        if (locbitopstruct->bitoparraysize==30L){
		  /* this is the first loop, write a debug file */
		  FILE *file;
		  unsigned long *running_base; /* same as farulong */
		  long counter;
		  file=fopen("debugbit.dat","w");
		  running_base=bitarraybase;
		  for (counter=0;counter<(long)(locbitopstruct->bitfieldarraysize);counter++){
#ifdef LONG64
		    fprintf(file,"%08X",(unsigned int)(*running_base&0xFFFFFFFFL));
		    fprintf(file,"%08X",(unsigned int)((*running_base>>32)&0xFFFFFFFFL));
		    if ((counter+1)%4==0) fprintf(file,"\n");
#else
		    fprintf(file,"%08lX",*running_base);
		    if ((counter+1)%8==0) fprintf(file,"\n");
#endif
		    running_base=running_base+1;
		  }
		  fclose(file);
		  printf("\nWrote the file debugbit.dat, you may want to compare it to debugbit.good\n");
		}
#endif
#endif

		if (ticks>global_min_ticks) break;      /* We're ok...exit */

		FreeMemory((farvoid *)bitoparraybase,&systemerror);
		locbitopstruct->bitoparraysize+=100L;
	}
}
else
{
	/*
	** Don't need to do self adjustment, just allocate
	** the array space.
	*/
	bitarraybase=(farulong *)AllocateMemory(locbitopstruct->bitfieldarraysize *
		sizeof(ulong),&systemerror);
	if(systemerror)
	{       ReportError(errorcontext,systemerror);
		ErrorExit();
	}
	bitoparraybase=(farulong *)AllocateMemory(locbitopstruct->bitoparraysize*2L*
		sizeof(ulong),
		&systemerror);
	if(systemerror)
	{       ReportError(errorcontext,systemerror);
		FreeMemory((farvoid *)bitarraybase,&systemerror);
		ErrorExit();
	}
}

/*
** All's well if we get here.  Repeatedly perform bitops until the
** accumulated elapsed time is greater than # of seconds requested.
*/
accumtime=0L;
iterations=(double)0.0;
do {
	accumtime+=DoBitfieldIteration(bitarraybase,
			bitoparraybase,
			locbitopstruct->bitoparraysize,&nbitops);
	iterations+=(double)nbitops;
} while(TicksToSecs(accumtime)<locbitopstruct->request_secs);

/*
** Clean up, calculate results, and go home.
** Also, set adjustment flag to show that we don't have
** to do self adjusting in the future.
*/
FreeMemory((farvoid *)bitarraybase,&systemerror);
FreeMemory((farvoid *)bitoparraybase,&systemerror);
locbitopstruct->bitopspersec=iterations /TicksToFracSecs(accumtime);
if(locbitopstruct->adjust==0)
	locbitopstruct->adjust=1;

return;
}

/************************
** DoBitfieldIteration **
*************************
** Perform a single iteration of the bitfield benchmark.
** Return the # of ticks accumulated by the operation.
*/
static ulong DoBitfieldIteration(farulong *bitarraybase,
		farulong *bitoparraybase,
		long bitoparraysize,
		ulong *nbitops)
{
long i;                         /* Index */
ulong bitoffset;                /* Offset into bitmap */
ulong elapsed;                  /* Time to execute */
/*
** Clear # bitops counter
*/
*nbitops=0L;

/*
** Construct a set of bitmap offsets and run lengths.
** The offset can be any random number from 0 to the
** size of the bitmap (in bits).  The run length can
** be any random number from 1 to the number of bits
** between the offset and the end of the bitmap.
** Note that the bitmap has 8192 * 32 bits in it.
** (262,144 bits)
*/
/*
** Reset random number generator so things repeat.
** Also reset the bit array we work on.
** added by Uwe F. Mayer
*/
randnum((int32)13);
for (i=0;i<global_bitopstruct.bitfieldarraysize;i++)
{
#ifdef LONG64
	*(bitarraybase+i)=(ulong)0x5555555555555555;
#else
	*(bitarraybase+i)=(ulong)0x55555555;
#endif
}
randnum((int32)13);
/* end of addition of code */

for (i=0;i<bitoparraysize;i++)
{
	/* First item is offset */
        /* *(bitoparraybase+i+i)=bitoffset=abs_randwc(262140L); */
	*(bitoparraybase+i+i)=bitoffset=abs_randwc((int32)262140);

	/* Next item is run length */
	/* *nbitops+=*(bitoparraybase+i+i+1L)=abs_randwc(262140L-bitoffset);*/
	*nbitops+=*(bitoparraybase+i+i+1L)=abs_randwc((int32)262140-bitoffset);
}

/*
** Array of offset and lengths built...do an iteration of
** the test.
** Start the stopwatch.
*/
elapsed=StartStopwatch();

/*
** Loop through array off offset/run length pairs.
** Execute operation based on modulus of index.
*/
for(i=0;i<bitoparraysize;i++)
{
	switch(i % 3)
	{

		case 0: /* Set run of bits */
			ToggleBitRun(bitarraybase,
				*(bitoparraybase+i+i),
				*(bitoparraybase+i+i+1),
				1);
			break;

		case 1: /* Clear run of bits */
			ToggleBitRun(bitarraybase,
				*(bitoparraybase+i+i),
				*(bitoparraybase+i+i+1),
				0);
			break;

		case 2: /* Complement run of bits */
			FlipBitRun(bitarraybase,
				*(bitoparraybase+i+i),
				*(bitoparraybase+i+i+1));
			break;
	}
}

/*
** Return elapsed time
*/
return(StopStopwatch(elapsed));
}


/*****************************
**     ToggleBitRun          *
******************************
** Set or clear a run of nbits starting at
** bit_addr in bitmap.
*/
static void ToggleBitRun(farulong *bitmap, /* Bitmap */
		ulong bit_addr,         /* Address of bits to set */
		ulong nbits,            /* # of bits to set/clr */
		uint val)               /* 1 or 0 */
{
unsigned long bindex;   /* Index into array */
unsigned long bitnumb;  /* Bit number */

while(nbits--)
{
#ifdef LONG64
	bindex=bit_addr>>6;     /* Index is number /64 */
	bitnumb=bit_addr % 64;   /* Bit number in word */
#else
	bindex=bit_addr>>5;     /* Index is number /32 */
	bitnumb=bit_addr % 32;  /* bit number in word */
#endif
	if(val)
		bitmap[bindex]|=(1L<<bitnumb);
	else
		bitmap[bindex]&=~(1L<<bitnumb);
	bit_addr++;
}
return;
}

/***************
** FlipBitRun **
****************
** Complements a run of bits.
*/
static void FlipBitRun(farulong *bitmap,        /* Bit map */
		ulong bit_addr,                 /* Bit address */
		ulong nbits)                    /* # of bits to flip */
{
unsigned long bindex;   /* Index into array */
unsigned long bitnumb;  /* Bit number */

while(nbits--)
{
#ifdef LONG64
	bindex=bit_addr>>6;     /* Index is number /64 */
	bitnumb=bit_addr % 64;  /* Bit number in longword */
#else
	bindex=bit_addr>>5;     /* Index is number /32 */
	bitnumb=bit_addr % 32;  /* Bit number in longword */
#endif
	bitmap[bindex]^=(1L<<bitnumb);
	bit_addr++;
}

return;
}

/*****************************
** FLOATING-POINT EMULATION **
*****************************/

/**************
** DoEmFloat **
***************
** Perform the floating-point emulation routines portion of the
** CPU benchmark.  Returns the operations per second.
*/
void DoEmFloat(void)
{
EmFloatStruct *locemfloatstruct;        /* Local structure */
InternalFPF *abase;             /* Base of A array */
InternalFPF *bbase;             /* Base of B array */
InternalFPF *cbase;             /* Base of C array */
ulong accumtime;                /* Accumulated time in ticks */
double iterations;              /* # of iterations */
ulong tickcount;                /* # of ticks */
char *errorcontext;             /* Error context string pointer */
int systemerror;                /* For holding error code */
ulong loops;                    /* # of loops */

/*
** Link to global structure
*/
locemfloatstruct=&global_emfloatstruct;

/*
** Set the error context
*/
errorcontext="CPU:Floating Emulation";


/*
** Test the emulation routines.
*/
#ifdef DEBUG
#endif

abase=(InternalFPF *)AllocateMemory(locemfloatstruct->arraysize*sizeof(InternalFPF),
		&systemerror);
if(systemerror)
{       ReportError(errorcontext,systemerror);
	ErrorExit();
}

bbase=(InternalFPF *)AllocateMemory(locemfloatstruct->arraysize*sizeof(InternalFPF),
		&systemerror);
if(systemerror)
{       ReportError(errorcontext,systemerror);
	FreeMemory((farvoid *)abase,&systemerror);
	ErrorExit();
}

cbase=(InternalFPF *)AllocateMemory(locemfloatstruct->arraysize*sizeof(InternalFPF),
		&systemerror);
if(systemerror)
{       ReportError(errorcontext,systemerror);
	FreeMemory((farvoid *)abase,&systemerror);
	FreeMemory((farvoid *)bbase,&systemerror);
	ErrorExit();
}

/*
** Set up the arrays
*/
SetupCPUEmFloatArrays(abase,bbase,cbase,locemfloatstruct->arraysize);

/*
** See if we need to do self-adjusting code.
*/
if(locemfloatstruct->adjust==0)
{
	locemfloatstruct->loops=0;

	/*
	** Do an iteration of the tests.  If the elapsed time is
	** less than minimum, increase the loop count and try
	** again.
	*/
	for(loops=1;loops<CPUEMFLOATLOOPMAX;loops+=loops)
	{       tickcount=DoEmFloatIteration(abase,bbase,cbase,
			locemfloatstruct->arraysize,
			loops);
		if(tickcount>global_min_ticks)
		{       locemfloatstruct->loops=loops;
			break;
		}
	}
}

/*
** Verify that selft adjustment code worked.
*/
if(locemfloatstruct->loops==0)
{       printf("CPU:EMFPU -- CMPUEMFLOATLOOPMAX limit hit\n");
	FreeMemory((farvoid *)abase,&systemerror);
	FreeMemory((farvoid *)bbase,&systemerror);
	FreeMemory((farvoid *)cbase,&systemerror);
	ErrorExit();
}

/*
** All's well if we get here.  Repeatedly perform floating
** tests until the accumulated time is greater than the
** # of seconds requested.
** Each iteration performs arraysize * 3 operations.
*/
accumtime=0L;
iterations=(double)0.0;
do {
	accumtime+=DoEmFloatIteration(abase,bbase,cbase,
			locemfloatstruct->arraysize,
			locemfloatstruct->loops);
	iterations+=(double)1.0;
} while(TicksToSecs(accumtime)<locemfloatstruct->request_secs);


/*
** Clean up, calculate results, and go home.
** Also, indicate that adjustment is done.
*/
FreeMemory((farvoid *)abase,&systemerror);
FreeMemory((farvoid *)bbase,&systemerror);
FreeMemory((farvoid *)cbase,&systemerror);

locemfloatstruct->emflops=(iterations*(double)locemfloatstruct->loops)/
		(double)TicksToFracSecs(accumtime);
if(locemfloatstruct->adjust==0)
	locemfloatstruct->adjust=1;

#ifdef DEBUG
printf("----------------------------------------------------------------------------\n");
#endif
return;
}

/*************************
** FOURIER COEFFICIENTS **
*************************/

/**************
** DoFourier **
***************
** Perform the transcendental/trigonometric portion of the
** benchmark.  This benchmark calculates the first n
** fourier coefficients of the function (x+1)^x defined
** on the interval 0,2.
*/
void DoFourier(void)
{
FourierStruct *locfourierstruct;        /* Local fourier struct */
fardouble *abase;               /* Base of A[] coefficients array */
fardouble *bbase;               /* Base of B[] coefficients array */
unsigned long accumtime;        /* Accumulated time in ticks */
double iterations;              /* # of iterations */
char *errorcontext;             /* Error context string pointer */
int systemerror;                /* For error code */

/*
** Link to global structure
*/
locfourierstruct=&global_fourierstruct;

/*
** Set error context string
*/
errorcontext="FPU:Transcendental";

/*
** See if we need to do self-adjustment code.
*/
if(locfourierstruct->adjust==0)
{
	locfourierstruct->arraysize=100L;       /* Start at 100 elements */
	while(1)
	{

		abase=(fardouble *)AllocateMemory(locfourierstruct->arraysize*sizeof(double),
				&systemerror);
		if(systemerror)
		{       ReportError(errorcontext,systemerror);
			ErrorExit();
		}

		bbase=(fardouble *)AllocateMemory(locfourierstruct->arraysize*sizeof(double),
				&systemerror);
		if(systemerror)
		{       ReportError(errorcontext,systemerror);
			FreeMemory((void *)abase,&systemerror);
			ErrorExit();
		}
		/*
		** Do an iteration of the tests.  If the elapsed time is
		** less than or equal to the permitted minimum, re-allocate
		** larger arrays and try again.
		*/
		if(DoFPUTransIteration(abase,bbase,
			locfourierstruct->arraysize)>global_min_ticks)
			break;          /* We're ok...exit */

		/*
		** Make bigger arrays and try again.
		*/
		FreeMemory((farvoid *)abase,&systemerror);
		FreeMemory((farvoid *)bbase,&systemerror);
		locfourierstruct->arraysize+=50L;
	}
}
else
{       /*
	** Don't need self-adjustment.  Just allocate the
	** arrays, and go.
	*/
	abase=(fardouble *)AllocateMemory(locfourierstruct->arraysize*sizeof(double),
			&systemerror);
	if(systemerror)
	{       ReportError(errorcontext,systemerror);
		ErrorExit();
	}

	bbase=(fardouble *)AllocateMemory(locfourierstruct->arraysize*sizeof(double),
			&systemerror);
	if(systemerror)
	{       ReportError(errorcontext,systemerror);
		FreeMemory((void *)abase,&systemerror);
		ErrorExit();
	}
}
/*
** All's well if we get here.  Repeatedly perform integration
** tests until the accumulated time is greater than the
** # of seconds requested.
*/
accumtime=0L;
iterations=(double)0.0;
do {
	accumtime+=DoFPUTransIteration(abase,bbase,locfourierstruct->arraysize);
	iterations+=(double)locfourierstruct->arraysize*(double)2.0-(double)1.0;
} while(TicksToSecs(accumtime)<locfourierstruct->request_secs);


/*
** Clean up, calculate results, and go home.
** Also set adjustment flag to indicate no adjust code needed.
*/
FreeMemory((farvoid *)abase,&systemerror);
FreeMemory((farvoid *)bbase,&systemerror);

locfourierstruct->fflops=iterations/(double)TicksToFracSecs(accumtime);

if(locfourierstruct->adjust==0)
	locfourierstruct->adjust=1;

return;
}

/************************
** DoFPUTransIteration **
*************************
** Perform an iteration of the FPU Transcendental/trigonometric
** benchmark.  Here, an iteration consists of calculating the
** first n fourier coefficients of the function (x+1)^x on
** the interval 0,2.  n is given by arraysize.
** NOTE: The # of integration steps is fixed at
** 200.
*/
static ulong DoFPUTransIteration(fardouble *abase,      /* A coeffs. */
			fardouble *bbase,               /* B coeffs. */
			ulong arraysize)                /* # of coeffs */
{
double omega;           /* Fundamental frequency */
unsigned long i;        /* Index */
unsigned long elapsed;  /* Elapsed time */

/*
** Start the stopwatch
*/
elapsed=StartStopwatch();

/*
** Calculate the fourier series.  Begin by
** calculating A[0].
*/

*abase=TrapezoidIntegrate((double)0.0,
			(double)2.0,
			200,
			(double)0.0,    /* No omega * n needed */
			0 )/(double)2.0;

/*
** Calculate the fundamental frequency.
** ( 2 * pi ) / period...and since the period
** is 2, omega is simply pi.
*/
omega=(double)3.1415926535897932;

for(i=1;i<arraysize;i++)
{

	/*
	** Calculate A[i] terms.  Note, once again, that we
	** can ignore the 2/period term outside the integral
	** since the period is 2 and the term cancels itself
	** out.
	*/
	*(abase+i)=TrapezoidIntegrate((double)0.0,
			(double)2.0,
			200,
			omega * (double)i,
			1);

	/*
	** Calculate the B[i] terms.
	*/
	*(bbase+i)=TrapezoidIntegrate((double)0.0,
			(double)2.0,
			200,
			omega * (double)i,
			2);

}
#ifdef DEBUG
{
  int i;
  printf("\nA[i]=\n");
  for (i=0;i<arraysize;i++) printf("%7.3g ",abase[i]);
  printf("\nB[i]=\n(undefined) ");
  for (i=1;i<arraysize;i++) printf("%7.3g ",bbase[i]);
}
#endif
/*
** All done, stop the stopwatch
*/
return(StopStopwatch(elapsed));
}

/***********************
** TrapezoidIntegrate **
************************
** Perform a simple trapezoid integration on the
** function (x+1)**x.
** x0,x1 set the lower and upper bounds of the
** integration.
** nsteps indicates # of trapezoidal sections
** omegan is the fundamental frequency times
**  the series member #
** select = 0 for the A[0] term, 1 for cosine terms, and
**   2 for sine terms.
** Returns the value.
*/
static double TrapezoidIntegrate( double x0,            /* Lower bound */
			double x1,              /* Upper bound */
			int nsteps,             /* # of steps */
			double omegan,          /* omega * n */
			int select)
{
double x;               /* Independent variable */
double dx;              /* Stepsize */
double rvalue;          /* Return value */


/*
** Initialize independent variable
*/
x=x0;

/*
** Calculate stepsize
*/
dx=(x1 - x0) / (double)nsteps;

/*
** Initialize the return value.
*/
rvalue=thefunction(x0,omegan,select)/(double)2.0;

/*
** Compute the other terms of the integral.
*/
if(nsteps!=1)
{       --nsteps;               /* Already done 1 step */
	while(--nsteps )
	{
		x+=dx;
		rvalue+=thefunction(x,omegan,select);
	}
}
/*
** Finish computation
*/
rvalue=(rvalue+thefunction(x1,omegan,select)/(double)2.0)*dx;

return(rvalue);
}

/****************
** thefunction **
*****************
** This routine selects the function to be used
** in the Trapezoid integration.
** x is the independent variable
** omegan is omega * n
** select chooses which of the sine/cosine functions
**  are used.  note the special case for select=0.
*/
static double thefunction(double x,             /* Independent variable */
		double omegan,          /* Omega * term */
		int select)             /* Choose term */
{

/*
** Use select to pick which function we call.
*/
switch(select)
{
	case 0: return(pow(x+(double)1.0,x));

	case 1: return(pow(x+(double)1.0,x) * cos(omegan * x));

	case 2: return(pow(x+(double)1.0,x) * sin(omegan * x));
}

/*
** We should never reach this point, but the following
** keeps compilers from issuing a warning message.
*/
return(0.0);
}

/*************************
** ASSIGNMENT ALGORITHM **
*************************/

/*************
** DoAssign **
**************
** Perform an assignment algorithm.
** The algorithm was adapted from the step by step guide found
** in "Quantitative Decision Making for Business" (Gordon,
**  Pressman, and Cohn; Prentice-Hall)
**
**
** NOTES:
** 1. Even though the algorithm distinguishes between
**    ASSIGNROWS and ASSIGNCOLS, as though the two might
**    be different, it does presume a square matrix.
**    I.E., ASSIGNROWS and ASSIGNCOLS must be the same.
**    This makes for some algorithmically-correct but
**    probably non-optimal constructs.
**
*/
void DoAssign(void)
{
AssignStruct *locassignstruct;  /* Local structure ptr */
farlong *arraybase;
char *errorcontext;
int systemerror;
ulong accumtime;
double iterations;

/*
** Link to global structure
*/
locassignstruct=&global_assignstruct;

/*
** Set the error context string.
*/
errorcontext="CPU:Assignment";

/*
** See if we need to do self adjustment code.
*/
if(locassignstruct->adjust==0)
{
	/*
	** Self-adjustment code.  The system begins by working on 1
	** array.  If it does that in no time, then two arrays
	** are built.  This process continues until
	** enough arrays are built to handle the tolerance.
	*/
	locassignstruct->numarrays=1;
	while(1)
	{
		/*
		** Allocate space for arrays
		*/
		arraybase=(farlong *) AllocateMemory(sizeof(long)*
			ASSIGNROWS*ASSIGNCOLS*locassignstruct->numarrays,
			 &systemerror);
		if(systemerror)
		{       ReportError(errorcontext,systemerror);
			FreeMemory((farvoid *)arraybase,
			  &systemerror);
			ErrorExit();
		}

		/*
		** Do an iteration of the assignment alg.  If the
		** elapsed time is less than or equal to the permitted
		** minimum, then allocate for more arrays and
		** try again.
		*/
		if(DoAssignIteration(arraybase,
			locassignstruct->numarrays)>global_min_ticks)
			break;          /* We're ok...exit */

		FreeMemory((farvoid *)arraybase, &systemerror);
		locassignstruct->numarrays++;
	}
}
else
{       /*
	** Allocate space for arrays
	*/
	arraybase=(farlong *)AllocateMemory(sizeof(long)*
		ASSIGNROWS*ASSIGNCOLS*locassignstruct->numarrays,
		 &systemerror);
	if(systemerror)
	{       ReportError(errorcontext,systemerror);
		FreeMemory((farvoid *)arraybase,
		  &systemerror);
		ErrorExit();
	}
}

/*
** All's well if we get here.  Do the tests.
*/
accumtime=0L;
iterations=(double)0.0;

do {
	accumtime+=DoAssignIteration(arraybase,
		locassignstruct->numarrays);
	iterations+=(double)1.0;
} while(TicksToSecs(accumtime)<locassignstruct->request_secs);

/*
** Clean up, calculate results, and go home.  Be sure to
** show that we don't have to rerun adjustment code.
*/
FreeMemory((farvoid *)arraybase,&systemerror);

locassignstruct->iterspersec=iterations *
	(double)locassignstruct->numarrays / TicksToFracSecs(accumtime);

if(locassignstruct->adjust==0)
	locassignstruct->adjust=1;

return;

}

/**********************
** DoAssignIteration **
***********************
** This routine executes one iteration of the assignment test.
** It returns the number of ticks elapsed in the iteration.
*/
static ulong DoAssignIteration(farlong *arraybase,
	ulong numarrays)
{
longptr abase;                  /* local pointer */
ulong elapsed;          /* Elapsed ticks */
ulong i;

/*
** Set up local pointer
*/
abase.ptrs.p=arraybase;

/*
** Load up the arrays with a random table.
*/
LoadAssignArrayWithRand(arraybase,numarrays);

/*
** Start the stopwatch
*/
elapsed=StartStopwatch();

/*
** Execute assignment algorithms
*/
for(i=0;i<numarrays;i++)
{       /* abase.ptrs.p+=i*ASSIGNROWS*ASSIGNCOLS; */
        /* Fixed  by Eike Dierks */
	Assignment(*abase.ptrs.ap);
	abase.ptrs.p+=ASSIGNROWS*ASSIGNCOLS;
}

/*
** Get elapsed time
*/
return(StopStopwatch(elapsed));
}

/****************************
** LoadAssignArrayWithRand **
*****************************
** Load the assignment arrays with random numbers.  All positive.
** These numbers represent costs.
*/
static void LoadAssignArrayWithRand(farlong *arraybase,
	ulong numarrays)
{
longptr abase,abase1;   /* Local for array pointer */
ulong i;

/*
** Set local array pointer
*/
abase.ptrs.p=arraybase;
abase1.ptrs.p=arraybase;

/*
** Set up the first array.  Then just copy it into the
** others.
*/
LoadAssign(*(abase.ptrs.ap));
if(numarrays>1)
	for(i=1;i<numarrays;i++)
	  {     /* abase1.ptrs.p+=i*ASSIGNROWS*ASSIGNCOLS; */
	        /* Fixed  by Eike Dierks */
	        abase1.ptrs.p+=ASSIGNROWS*ASSIGNCOLS;
		CopyToAssign(*(abase.ptrs.ap),*(abase1.ptrs.ap));
	}

return;
}

/***************
** LoadAssign **
****************
** The array given by arraybase is loaded with positive random
** numbers.  Elements in the array are capped at 5,000,000.
*/
static void LoadAssign(farlong arraybase[][ASSIGNCOLS])
{
ushort i,j;

/*
** Reset random number generator so things repeat.
*/
/* randnum(13L); */
randnum((int32)13);

for(i=0;i<ASSIGNROWS;i++)
  for(j=0;j<ASSIGNROWS;j++){
    /* arraybase[i][j]=abs_randwc(5000000L);*/
    arraybase[i][j]=abs_randwc((int32)5000000);
  }

return;
}

/*****************
** CopyToAssign **
******************
** Copy the contents of one array to another.  This is called by
** the routine that builds the initial array, and is used to copy
** the contents of the intial array into all following arrays.
*/
static void CopyToAssign(farlong arrayfrom[ASSIGNROWS][ASSIGNCOLS],
		farlong arrayto[ASSIGNROWS][ASSIGNCOLS])
{
ushort i,j;

for(i=0;i<ASSIGNROWS;i++)
	for(j=0;j<ASSIGNCOLS;j++)
		arrayto[i][j]=arrayfrom[i][j];

return;
}

/***************
** Assignment **
***************/
static void Assignment(farlong arraybase[][ASSIGNCOLS])
{
short assignedtableau[ASSIGNROWS][ASSIGNCOLS];

/*
** First, calculate minimum costs
*/
calc_minimum_costs(arraybase);

/*
** Repeat following until the number of rows selected
** equals the number of rows in the tableau.
*/
while(first_assignments(arraybase,assignedtableau)!=ASSIGNROWS)
{         second_assignments(arraybase,assignedtableau);
}

#ifdef DEBUG
{
	int i,j;
	printf("\nColumn choices for each row\n");
	for(i=0;i<ASSIGNROWS;i++)
	{
	        printf("R%03d: ",i);
		for(j=0;j<ASSIGNCOLS;j++)
			if(assignedtableau[i][j]==1)
				printf("%03d ",j);
	}
}
#endif

return;
}

/***********************
** calc_minimum_costs **
************************
** Revise the tableau by calculating the minimum costs on a
** row and column basis.  These minima are subtracted from
** their rows and columns, creating a new tableau.
*/
static void calc_minimum_costs(long tableau[][ASSIGNCOLS])
{
ushort i,j;              /* Index variables */
long currentmin;        /* Current minimum */
/*
** Determine minimum costs on row basis.  This is done by
** subtracting -- on a row-per-row basis -- the minum value
** for that row.
*/
for(i=0;i<ASSIGNROWS;i++)
{
	currentmin=MAXPOSLONG;  /* Initialize minimum */
	for(j=0;j<ASSIGNCOLS;j++)
		if(tableau[i][j]<currentmin)
			currentmin=tableau[i][j];

	for(j=0;j<ASSIGNCOLS;j++)
		tableau[i][j]-=currentmin;
}

/*
** Determine minimum cost on a column basis.  This works
** just as above, only now we step through the array
** column-wise
*/
for(j=0;j<ASSIGNCOLS;j++)
{
	currentmin=MAXPOSLONG;  /* Initialize minimum */
	for(i=0;i<ASSIGNROWS;i++)
		if(tableau[i][j]<currentmin)
			currentmin=tableau[i][j];

	/*
	** Here, we'll take the trouble to see if the current
	** minimum is zero.  This is likely worth it, since the
	** preceding loop will have created at least one zero in
	** each row.  We can save ourselves a few iterations.
	*/
	if(currentmin!=0)
		for(i=0;i<ASSIGNROWS;i++)
			tableau[i][j]-=currentmin;
}

return;
}

/**********************
** first_assignments **
***********************
** Do first assignments.
** The assignedtableau[] array holds a set of values that
** indicate the assignment of a value, or its elimination.
** The values are:
**      0 = Item is neither assigned nor eliminated.
**      1 = Item is assigned
**      2 = Item is eliminated
** Returns the number of selections made.  If this equals
** the number of rows, then an optimum has been determined.
*/
static int first_assignments(long tableau[][ASSIGNCOLS],
		short assignedtableau[][ASSIGNCOLS])
{
ushort i,j,k;                   /* Index variables */
ushort numassigns;              /* # of assignments */
ushort totnumassigns;           /* Total # of assignments */
ushort numzeros;                /* # of zeros in row */
int selected=0;                 /* Flag used to indicate selection */

/*
** Clear the assignedtableau, setting all members to show that
** no one is yet assigned, eliminated, or anything.
*/
for(i=0;i<ASSIGNROWS;i++)
	for(j=0;j<ASSIGNCOLS;j++)
		assignedtableau[i][j]=0;

totnumassigns=0;
do {
	numassigns=0;
	/*
	** Step through rows.  For each one that is not currently
	** assigned, see if the row has only one zero in it.  If so,
	** mark that as an assigned row/col.  Eliminate other zeros
	** in the same column.
	*/
	for(i=0;i<ASSIGNROWS;i++)
	{       numzeros=0;
		for(j=0;j<ASSIGNCOLS;j++)
			if(tableau[i][j]==0L)
				if(assignedtableau[i][j]==0)
				{       numzeros++;
					selected=j;
				}
		if(numzeros==1)
		{       numassigns++;
			totnumassigns++;
			assignedtableau[i][selected]=1;
			for(k=0;k<ASSIGNROWS;k++)
				if((k!=i) &&
				   (tableau[k][selected]==0))
					assignedtableau[k][selected]=2;
		}
	}
	/*
	** Step through columns, doing same as above.  Now, be careful
	** of items in the other rows of a selected column.
	*/
	for(j=0;j<ASSIGNCOLS;j++)
	{       numzeros=0;
		for(i=0;i<ASSIGNROWS;i++)
			if(tableau[i][j]==0L)
				if(assignedtableau[i][j]==0)
				{       numzeros++;
					selected=i;
				}
		if(numzeros==1)
		{       numassigns++;
			totnumassigns++;
			assignedtableau[selected][j]=1;
			for(k=0;k<ASSIGNCOLS;k++)
				if((k!=j) &&
				   (tableau[selected][k]==0))
					assignedtableau[selected][k]=2;
		}
	}
	/*
	** Repeat until no more assignments to be made.
	*/
} while(numassigns!=0);

/*
** See if we can leave at this point.
*/
if(totnumassigns==ASSIGNROWS) return(totnumassigns);

/*
** Now step through the array by row.  If you find any unassigned
** zeros, pick the first in the row.  Eliminate all zeros from
** that same row & column.  This occurs if there are multiple optima...
** possibly.
*/
for(i=0;i<ASSIGNROWS;i++)
{       selected=-1;
	for(j=0;j<ASSIGNCOLS;j++)
		if((tableau[i][j]==0L) &&
		   (assignedtableau[i][j]==0))
		{       selected=j;
			break;
		}
	if(selected!=-1)
	{       assignedtableau[i][selected]=1;
		totnumassigns++;
		for(k=0;k<ASSIGNCOLS;k++)
			if((k!=selected) &&
			   (tableau[i][k]==0L))
				assignedtableau[i][k]=2;
		for(k=0;k<ASSIGNROWS;k++)
			if((k!=i) &&
			   (tableau[k][selected]==0L))
				assignedtableau[k][selected]=2;
	}
}

return(totnumassigns);
}

/***********************
** second_assignments **
************************
** This section of the algorithm creates the revised
** tableau, and is difficult to explain.  I suggest you
** refer to the algorithm's source, mentioned in comments
** toward the beginning of the program.
*/
static void second_assignments(long tableau[][ASSIGNCOLS],
		short assignedtableau[][ASSIGNCOLS])
{
int i,j;                                /* Indexes */
short linesrow[ASSIGNROWS];
short linescol[ASSIGNCOLS];
long smallest;                          /* Holds smallest value */
ushort numassigns;                      /* Number of assignments */
ushort newrows;                         /* New rows to be considered */
/*
** Clear the linesrow and linescol arrays.
*/
for(i=0;i<ASSIGNROWS;i++)
	linesrow[i]=0;
for(i=0;i<ASSIGNCOLS;i++)
	linescol[i]=0;

/*
** Scan rows, flag each row that has no assignment in it.
*/
for(i=0;i<ASSIGNROWS;i++)
{       numassigns=0;
	for(j=0;j<ASSIGNCOLS;j++)
		if(assignedtableau[i][j]==1)
		{       numassigns++;
			break;
		}
	if(numassigns==0) linesrow[i]=1;
}

do {

	newrows=0;
	/*
	** For each row checked above, scan for any zeros.  If found,
	** check the associated column.
	*/
	for(i=0;i<ASSIGNROWS;i++)
	{       if(linesrow[i]==1)
			for(j=0;j<ASSIGNCOLS;j++)
				if(tableau[i][j]==0)
					linescol[j]=1;
	}

	/*
	** Now scan checked columns.  If any contain assigned zeros, check
	** the associated row.
	*/
	for(j=0;j<ASSIGNCOLS;j++)
		if(linescol[j]==1)
			for(i=0;i<ASSIGNROWS;i++)
				if((assignedtableau[i][j]==1) &&
					(linesrow[i]!=1))
				{
					linesrow[i]=1;
					newrows++;
				}
} while(newrows!=0);

/*
** linesrow[n]==0 indicate rows covered by imaginary line
** linescol[n]==1 indicate cols covered by imaginary line
** For all cells not covered by imaginary lines, determine smallest
** value.
*/
smallest=MAXPOSLONG;
for(i=0;i<ASSIGNROWS;i++)
	if(linesrow[i]!=0)
		for(j=0;j<ASSIGNCOLS;j++)
			if(linescol[j]!=1)
				if(tableau[i][j]<smallest)
					smallest=tableau[i][j];

/*
** Subtract smallest from all cells in the above set.
*/
for(i=0;i<ASSIGNROWS;i++)
	if(linesrow[i]!=0)
		for(j=0;j<ASSIGNCOLS;j++)
			if(linescol[j]!=1)
				tableau[i][j]-=smallest;

/*
** Add smallest to all cells covered by two lines.
*/
for(i=0;i<ASSIGNROWS;i++)
	if(linesrow[i]==0)
		for(j=0;j<ASSIGNCOLS;j++)
			if(linescol[j]==1)
				tableau[i][j]+=smallest;

return;
}

/********************
** IDEA Encryption **
*********************
** IDEA - International Data Encryption Algorithm.
** Based on code presented in Applied Cryptography by Bruce Schneier.
** Which was based on code developed by Xuejia Lai and James L. Massey.
** Other modifications made by Colin Plumb.
**
*/

/***********
** DoIDEA **
************
** Perform IDEA encryption.  Note that we time encryption & decryption
** time as being a single loop.
*/
void DoIDEA(void)
{
IDEAStruct *locideastruct;      /* Loc pointer to global structure */
int i;
IDEAkey Z,DK;
u16 userkey[8];
ulong accumtime;
double iterations;
char *errorcontext;
int systemerror;
faruchar *plain1;               /* First plaintext buffer */
faruchar *crypt1;               /* Encryption buffer */
faruchar *plain2;               /* Second plaintext buffer */

/*
** Link to global data
*/
locideastruct=&global_ideastruct;

/*
** Set error context
*/
errorcontext="CPU:IDEA";

/*
** Re-init random-number generator.
*/
/* randnum(3L); */
randnum((int32)3);

/*
** Build an encryption/decryption key
*/
for (i=0;i<8;i++)
        /* userkey[i]=(u16)(abs_randwc(60000L) & 0xFFFF); */
	userkey[i]=(u16)(abs_randwc((int32)60000) & 0xFFFF);
for(i=0;i<KEYLEN;i++)
	Z[i]=0;

/*
** Compute encryption/decryption subkeys
*/
en_key_idea(userkey,Z);
de_key_idea(Z,DK);

/*
** Allocate memory for buffers.  We'll make 3, called plain1,
** crypt1, and plain2.  It works like this:
**   plain1 >>encrypt>> crypt1 >>decrypt>> plain2.
** So, plain1 and plain2 should match.
** Also, fill up plain1 with sample text.
*/
plain1=(faruchar *)AllocateMemory(locideastruct->arraysize,&systemerror);
if(systemerror)
{
	ReportError(errorcontext,systemerror);
	ErrorExit();
}

crypt1=(faruchar *)AllocateMemory(locideastruct->arraysize,&systemerror);
if(systemerror)
{
	ReportError(errorcontext,systemerror);
	FreeMemory((farvoid *)plain1,&systemerror);
	ErrorExit();
}

plain2=(faruchar *)AllocateMemory(locideastruct->arraysize,&systemerror);
if(systemerror)
{
	ReportError(errorcontext,systemerror);
	FreeMemory((farvoid *)plain1,&systemerror);
	FreeMemory((farvoid *)crypt1,&systemerror);
	ErrorExit();
}
/*
** Note that we build the "plaintext" by simply loading
** the array up with random numbers.
*/
for(i=0;i<locideastruct->arraysize;i++)
	plain1[i]=(uchar)(abs_randwc(255) & 0xFF);

/*
** See if we need to perform self adjustment loop.
*/
if(locideastruct->adjust==0)
{
	/*
	** Do self-adjustment.  This involves initializing the
	** # of loops and increasing the loop count until we
	** get a number of loops that we can use.
	*/
	for(locideastruct->loops=100L;
	  locideastruct->loops<MAXIDEALOOPS;
	  locideastruct->loops+=10L)
		if(DoIDEAIteration(plain1,crypt1,plain2,
		  locideastruct->arraysize,
		  locideastruct->loops,
		  Z,DK)>global_min_ticks) break;
}

/*
** All's well if we get here.  Do the test.
*/
accumtime=0L;
iterations=(double)0.0;

do {
	accumtime+=DoIDEAIteration(plain1,crypt1,plain2,
		locideastruct->arraysize,
		locideastruct->loops,Z,DK);
	iterations+=(double)locideastruct->loops;
} while(TicksToSecs(accumtime)<locideastruct->request_secs);

/*
** Clean up, calculate results, and go home.  Be sure to
** show that we don't have to rerun adjustment code.
*/
FreeMemory((farvoid *)plain1,&systemerror);
FreeMemory((farvoid *)crypt1,&systemerror);
FreeMemory((farvoid *)plain2,&systemerror);
locideastruct->iterspersec=iterations / TicksToFracSecs(accumtime);

if(locideastruct->adjust==0)
	locideastruct->adjust=1;

return;

}

/********************
** DoIDEAIteration **
*********************
** Execute a single iteration of the IDEA encryption algorithm.
** Actually, a single iteration is one encryption and one
** decryption.
*/
static ulong DoIDEAIteration(faruchar *plain1,
			faruchar *crypt1,
			faruchar *plain2,
			ulong arraysize,
			ulong nloops,
			IDEAkey Z,
			IDEAkey DK)
{
register ulong i;
register ulong j;
ulong elapsed;
#ifdef DEBUG
int status=0;
#endif

/*
** Start the stopwatch.
*/
elapsed=StartStopwatch();

/*
** Do everything for nloops.
*/
for(i=0;i<nloops;i++)
{
	for(j=0;j<arraysize;j+=(sizeof(u16)*4))
		cipher_idea((u16 *)(plain1+j),(u16 *)(crypt1+j),Z);       /* Encrypt */

	for(j=0;j<arraysize;j+=(sizeof(u16)*4))
		cipher_idea((u16 *)(crypt1+j),(u16 *)(plain2+j),DK);      /* Decrypt */
}

#ifdef DEBUG
for(j=0;j<arraysize;j++)
	if(*(plain1+j)!=*(plain2+j)){
		printf("IDEA Error! \n");
                status=1;
                }
if (status==0) printf("IDEA: OK\n");
#endif

/*
** Get elapsed time.
*/
return(StopStopwatch(elapsed));
}

/********
** mul **
*********
** Performs multiplication, modulo (2**16)+1.  This code is structured
** on the assumption that untaken branches are cheaper than taken
** branches, and that the compiler doesn't schedule branches.
*/
static u16 mul(register u16 a, register u16 b)
{
register u32 p;
if(a)
{       if(b)
	{       p=(u32)(a*b);
		b=low16(p);
		a=(u16)(p>>16);
		return(b-a+(b<a));
	}
	else
		return(1-a);
}
else
	return(1-b);
}

/********
** inv **
*********
** Compute multiplicative inverse of x, modulo (2**16)+1
** using Euclid's GCD algorithm.  It is unrolled twice
** to avoid swapping the meaning of the registers.  And
** some subtracts are changed to adds.
*/
static u16 inv(u16 x)
{
u16 t0, t1;
u16 q, y;

if(x<=1)
	return(x);      /* 0 and 1 are self-inverse */
t1=0x10001 / x;
y=0x10001 % x;
if(y==1)
	return(low16(1-t1));
t0=1;
do {
	q=x/y;
	x=x%y;
	t0+=q*t1;
	if(x==1) return(t0);
	q=y/x;
	y=y%x;
	t1+=q*t0;
} while(y!=1);
return(low16(1-t1));
}

/****************
** en_key_idea **
*****************
** Compute IDEA encryption subkeys Z
*/
static void en_key_idea(u16 *userkey, u16 *Z)
{
int i,j;

/*
** shifts
*/
for(j=0;j<8;j++)
	Z[j]=*userkey++;
for(i=0;j<KEYLEN;j++)
{       i++;
	Z[i+7]=(Z[i&7]<<9)| (Z[(i+1) & 7] >> 7);
	Z+=i&8;
	i&=7;
}
return;
}

/****************
** de_key_idea **
*****************
** Compute IDEA decryption subkeys DK from encryption
** subkeys Z.
*/
static void de_key_idea(IDEAkey Z, IDEAkey DK)
{
IDEAkey TT;
int j;
u16 t1, t2, t3;
u16 *p;
p=(u16 *)(TT+KEYLEN);

t1=inv(*Z++);
t2=-*Z++;
t3=-*Z++;
*--p=inv(*Z++);
*--p=t3;
*--p=t2;
*--p=t1;

for(j=1;j<ROUNDS;j++)
{       t1=*Z++;
	*--p=*Z++;
	*--p=t1;
	t1=inv(*Z++);
	t2=-*Z++;
	t3=-*Z++;
	*--p=inv(*Z++);
	*--p=t2;
	*--p=t3;
	*--p=t1;
}
t1=*Z++;
*--p=*Z++;
*--p=t1;
t1=inv(*Z++);
t2=-*Z++;
t3=-*Z++;
*--p=inv(*Z++);
*--p=t3;
*--p=t2;
*--p=t1;
/*
** Copy and destroy temp copy
*/
for(j=0,p=TT;j<KEYLEN;j++)
{       *DK++=*p;
	*p++=0;
}

return;
}

/*
** MUL(x,y)
** This #define creates a macro that computes x=x*y modulo 0x10001.
** Requires temps t16 and t32.  Also requires y to be strictly 16
** bits.  Here, I am using the simplest form.  May not be the
** fastest. -- RG
*/
/* #define MUL(x,y) (x=mul(low16(x),y)) */

/****************
** cipher_idea **
*****************
** IDEA encryption/decryption algorithm.
*/
static void cipher_idea(u16 in[4],
		u16 out[4],
		register IDEAkey Z)
{
register u16 x1, x2, x3, x4, t1, t2;
/* register u16 t16;
register u16 t32; */
int r=ROUNDS;

x1=*in++;
x2=*in++;
x3=*in++;
x4=*in;

do {
	MUL(x1,*Z++);
	x2+=*Z++;
	x3+=*Z++;
	MUL(x4,*Z++);

	t2=x1^x3;
	MUL(t2,*Z++);
	t1=t2+(x2^x4);
	MUL(t1,*Z++);
	t2=t1+t2;

	x1^=t1;
	x4^=t2;

	t2^=x2;
	x2=x3^t1;
	x3=t2;
} while(--r);
MUL(x1,*Z++);
*out++=x1;
*out++=x3+*Z++;
*out++=x2+*Z++;
MUL(x4,*Z);
*out=x4;
return;
}

/************************
** HUFFMAN COMPRESSION **
************************/

/**************
** DoHuffman **
***************
** Execute a huffman compression on a block of plaintext.
** Note that (as with IDEA encryption) an iteration of the
** Huffman test includes a compression AND a decompression.
** Also, the compression cycle includes building the
** Huffman tree.
*/
void DoHuffman(void)
{
HuffStruct *lochuffstruct;      /* Loc pointer to global data */
char *errorcontext;
int systemerror;
ulong accumtime;
double iterations;
farchar *comparray;
farchar *decomparray;
farchar *plaintext;

/*
** Link to global data
*/
lochuffstruct=&global_huffstruct;

/*
** Set error context.
*/
errorcontext="CPU:Huffman";

/*
** Allocate memory for the plaintext and the compressed text.
** We'll be really pessimistic here, and allocate equal amounts
** for both (though we know...well, we PRESUME) the compressed
** stuff will take less than the plain stuff.
** Also note that we'll build a 3rd buffer to decompress
** into, and we preallocate space for the huffman tree.
** (We presume that the Huffman tree will grow no larger
** than 512 bytes.  This is actually a super-conservative
** estimate...but, who cares?)
*/
plaintext=(farchar *)AllocateMemory(lochuffstruct->arraysize,&systemerror);
if(systemerror)
{       ReportError(errorcontext,systemerror);
	ErrorExit();
}
comparray=(farchar *)AllocateMemory(lochuffstruct->arraysize,&systemerror);
if(systemerror)
{       ReportError(errorcontext,systemerror);
	FreeMemory(plaintext,&systemerror);
	ErrorExit();
}
decomparray=(farchar *)AllocateMemory(lochuffstruct->arraysize,&systemerror);
if(systemerror)
{       ReportError(errorcontext,systemerror);
	FreeMemory(plaintext,&systemerror);
	FreeMemory(comparray,&systemerror);
	ErrorExit();
}

hufftree=(huff_node *)AllocateMemory(sizeof(huff_node) * 512,
	&systemerror);
if(systemerror)
{       ReportError(errorcontext,systemerror);
	FreeMemory(plaintext,&systemerror);
	FreeMemory(comparray,&systemerror);
	FreeMemory(decomparray,&systemerror);
	ErrorExit();
}

/*
** Build the plaintext buffer.  Since we want this to
** actually be able to compress, we'll use the
** wordcatalog to build the plaintext stuff.
*/
/*
** Reset random number generator so things repeat.
** added by Uwe F. Mayer
*/
randnum((int32)13);
create_text_block(plaintext,lochuffstruct->arraysize-1,(ushort)500);
plaintext[lochuffstruct->arraysize-1L]='\0';
plaintextlen=lochuffstruct->arraysize;

/*
** See if we need to perform self adjustment loop.
*/
if(lochuffstruct->adjust==0)
{
	/*
	** Do self-adjustment.  This involves initializing the
	** # of loops and increasing the loop count until we
	** get a number of loops that we can use.
	*/
	for(lochuffstruct->loops=100L;
	  lochuffstruct->loops<MAXHUFFLOOPS;
	  lochuffstruct->loops+=10L)
		if(DoHuffIteration(plaintext,
			comparray,
			decomparray,
		  lochuffstruct->arraysize,
		  lochuffstruct->loops,
		  hufftree)>global_min_ticks) break;
}

/*
** All's well if we get here.  Do the test.
*/
accumtime=0L;
iterations=(double)0.0;

do {
	accumtime+=DoHuffIteration(plaintext,
		comparray,
		decomparray,
		lochuffstruct->arraysize,
		lochuffstruct->loops,
		hufftree);
	iterations+=(double)lochuffstruct->loops;
} while(TicksToSecs(accumtime)<lochuffstruct->request_secs);

/*
** Clean up, calculate results, and go home.  Be sure to
** show that we don't have to rerun adjustment code.
*/
FreeMemory((farvoid *)plaintext,&systemerror);
FreeMemory((farvoid *)comparray,&systemerror);
FreeMemory((farvoid *)decomparray,&systemerror);
FreeMemory((farvoid *)hufftree,&systemerror);
lochuffstruct->iterspersec=iterations / TicksToFracSecs(accumtime);

if(lochuffstruct->adjust==0)
	lochuffstruct->adjust=1;

}

/*********************
** create_text_line **
**********************
** Create a random line of text, stored at *dt.  The line may be
** no more than nchars long.
*/
static void create_text_line(farchar *dt,
			long nchars)
{
long charssofar;        /* # of characters so far */
long tomove;            /* # of characters to move */
char myword[40];        /* Local buffer for words */
farchar *wordptr;       /* Pointer to word from catalog */

charssofar=0;

do {
/*
** Grab a random word from the wordcatalog
*/
/* wordptr=wordcatarray[abs_randwc((long)WORDCATSIZE)];*/
wordptr=wordcatarray[abs_randwc((int32)WORDCATSIZE)];
MoveMemory((farvoid *)myword,
	(farvoid *)wordptr,
	(unsigned long)strlen(wordptr)+1);

/*
** Append a blank.
*/
tomove=strlen(myword)+1;
myword[tomove-1]=' ';

/*
** See how long it is.  If its length+charssofar > nchars, we have
** to trim it.
*/
if((tomove+charssofar)>nchars)
	tomove=nchars-charssofar;
/*
** Attach the word to the current line.  Increment counter.
*/
MoveMemory((farvoid *)dt,(farvoid *)myword,(unsigned long)tomove);
charssofar+=tomove;
dt+=tomove;

/*
** If we're done, bail out.  Otherwise, go get another word.
*/
} while(charssofar<nchars);

return;
}

/**********************
** create_text_block **
***********************
** Build a block of text randomly loaded with words.  The words
** come from the wordcatalog (which must be loaded before you
** call this).
** *tb points to the memory where the text is to be built.
** tblen is the # of bytes to put into the text block
** maxlinlen is the maximum length of any line (line end indicated
**  by a carriage return).
*/
static void create_text_block(farchar *tb,
			ulong tblen,
			ushort maxlinlen)
{
ulong bytessofar;       /* # of bytes so far */
ulong linelen;          /* Line length */

bytessofar=0L;
do {

/*
** Pick a random length for a line and fill the line.
** Make sure the line can fit (haven't exceeded tablen) and also
** make sure you leave room to append a carriage return.
*/
linelen=abs_randwc(maxlinlen-6)+6;
if((linelen+bytessofar)>tblen)
	linelen=tblen-bytessofar;

if(linelen>1)
{
	create_text_line(tb,linelen);
}
tb+=linelen-1;          /* Add the carriage return */
*tb++='\n';

bytessofar+=linelen;

} while(bytessofar<tblen);

}

/********************
** DoHuffIteration **
*********************
** Perform the huffman benchmark.  This routine
**  (a) Builds the huffman tree
**  (b) Compresses the text
**  (c) Decompresses the text and verifies correct decompression
*/
static ulong DoHuffIteration(farchar *plaintext,
	farchar *comparray,
	farchar *decomparray,
	ulong arraysize,
	ulong nloops,
	huff_node *hufftree)
{
int i;                          /* Index */
long j;                         /* Bigger index */
int root;                       /* Pointer to huffman tree root */
float lowfreq1, lowfreq2;       /* Low frequency counters */
int lowidx1, lowidx2;           /* Indexes of low freq. elements */
long bitoffset;                 /* Bit offset into text */
long textoffset;                /* Char offset into text */
long maxbitoffset;              /* Holds limit of bit offset */
long bitstringlen;              /* Length of bitstring */
int c;                          /* Character from plaintext */
char bitstring[30];             /* Holds bitstring */
ulong elapsed;                  /* For stopwatch */
#ifdef DEBUG
int status=0;
#endif

/*
** Start the stopwatch
*/
elapsed=StartStopwatch();

/*
** Do everything for nloops
*/
while(nloops--)
{

/*
** Calculate the frequency of each byte value. Store the
** results in what will become the "leaves" of the
** Huffman tree.  Interior nodes will be built in those
** nodes greater than node #255.
*/
for(i=0;i<256;i++)
{
	hufftree[i].freq=(float)0.0;
	hufftree[i].c=(unsigned char)i;
}

for(j=0;j<arraysize;j++)
	hufftree[(int)plaintext[j]].freq+=(float)1.0;

for(i=0;i<256;i++)
	if(hufftree[i].freq != (float)0.0)
		hufftree[i].freq/=(float)arraysize;

/* Reset the second half of the tree. Otherwise the loop below that
** compares the frequencies up to index 512 makes no sense. Some
** systems automatically zero out memory upon allocation, others (like
** for example DEC Unix) do not. Depending on this the loop below gets
** different data and different run times. On our alpha the data that
** was arbitrarily assigned led to an underflow error at runtime. We
** use that zeroed-out bits are in fact 0 as a float.
** Uwe F. Mayer */
bzero((char *)&(hufftree[256]),sizeof(huff_node)*256);
/*
** Build the huffman tree.  First clear all the parent
** pointers and left/right pointers.  Also, discard all
** nodes that have a frequency of true 0.  */
for(i=0;i<512;i++)
{       if(hufftree[i].freq==(float)0.0)
		hufftree[i].parent=EXCLUDED;
	else
		hufftree[i].parent=hufftree[i].left=hufftree[i].right=-1;
}

/*
** Go through the tree. Finding nodes of really low
** frequency.
*/
root=255;                       /* Starting root node-1 */
while(1)
{
	lowfreq1=(float)2.0; lowfreq2=(float)2.0;
	lowidx1=-1; lowidx2=-1;
	/*
	** Find first lowest frequency.
	*/
	for(i=0;i<=root;i++)
		if(hufftree[i].parent<0)
			if(hufftree[i].freq<lowfreq1)
			{       lowfreq1=hufftree[i].freq;
				lowidx1=i;
			}

	/*
	** Did we find a lowest value?  If not, the
	** tree is done.
	*/
	if(lowidx1==-1) break;

	/*
	** Find next lowest frequency
	*/
	for(i=0;i<=root;i++)
		if((hufftree[i].parent<0) && (i!=lowidx1))
			if(hufftree[i].freq<lowfreq2)
			{       lowfreq2=hufftree[i].freq;
				lowidx2=i;
			}

	/*
	** If we could only find one item, then that
	** item is surely the root, and (as above) the
	** tree is done.
	*/
	if(lowidx2==-1) break;

	/*
	** Attach the two new nodes to the current root, and
	** advance the current root.
	*/
	root++;                 /* New root */
	hufftree[lowidx1].parent=root;
	hufftree[lowidx2].parent=root;
	hufftree[root].freq=lowfreq1+lowfreq2;
	hufftree[root].left=lowidx1;
	hufftree[root].right=lowidx2;
	hufftree[root].parent=-2;       /* Show root */
}

/*
** Huffman tree built...compress the plaintext
*/
bitoffset=0L;                           /* Initialize bit offset */
for(i=0;i<arraysize;i++)
{
	c=(int)plaintext[i];                 /* Fetch character */
	/*
	** Build a bit string for byte c
	*/
	bitstringlen=0;
	while(hufftree[c].parent!=-2)
	{       if(hufftree[hufftree[c].parent].left==c)
			bitstring[bitstringlen]='0';
		else
			bitstring[bitstringlen]='1';
		c=hufftree[c].parent;
		bitstringlen++;
	}

	/*
	** Step backwards through the bit string, setting
	** bits in the compressed array as you go.
	*/
	while(bitstringlen--)
	{       SetCompBit((u8 *)comparray,(u32)bitoffset,bitstring[bitstringlen]);
		bitoffset++;
	}
}

/*
** Compression done.  Perform de-compression.
*/
maxbitoffset=bitoffset;
bitoffset=0;
textoffset=0;
do {
	i=root;
	while(hufftree[i].left!=-1)
	{       if(GetCompBit((u8 *)comparray,(u32)bitoffset)==0)
			i=hufftree[i].left;
		else
			i=hufftree[i].right;
		bitoffset++;
	}
	decomparray[textoffset]=hufftree[i].c;

#ifdef DEBUG
	if(hufftree[i].c != plaintext[textoffset])
	{
		/* Show error */
		printf("Error at textoffset %ld\n",textoffset);
		status=1;
	}
#endif
	textoffset++;
} while(bitoffset<maxbitoffset);

}       /* End the big while(nloops--) from above */

/*
** All done
*/
#ifdef DEBUG
  if (status==0) printf("Huffman: OK\n");
#endif
return(StopStopwatch(elapsed));
}

/***************
** SetCompBit **
****************
** Set a bit in the compression array.  The value of the
** bit is set according to char bitchar.
*/
static void SetCompBit(u8 *comparray,
		u32 bitoffset,
		char bitchar)
{
u32 byteoffset;
int bitnumb;

/*
** First calculate which element in the comparray to
** alter. and the bitnumber.
*/
byteoffset=bitoffset>>3;
bitnumb=bitoffset % 8;

/*
** Set or clear
*/
if(bitchar=='1')
	comparray[byteoffset]|=(1<<bitnumb);
else
	comparray[byteoffset]&=~(1<<bitnumb);

return;
}

/***************
** GetCompBit **
****************
** Return the bit value of a bit in the comparession array.
** Returns 0 if the bit is clear, nonzero otherwise.
*/
static int GetCompBit(u8 *comparray,
		u32 bitoffset)
{
u32 byteoffset;
int bitnumb;

/*
** Calculate byte offset and bit number.
*/
byteoffset=bitoffset>>3;
bitnumb=bitoffset % 8;

/*
** Fetch
*/
return((1<<bitnumb) & comparray[byteoffset] );
}

/********************************
** BACK PROPAGATION NEURAL NET **
*********************************
** This code is a modified version of the code
** that was submitted to BYTE Magazine by
** Maureen Caudill.  It accomanied an article
** that I CANNOT NOW RECALL.
** The author's original heading/comment was
** as follows:
**
**  Backpropagation Network
**  Written by Maureen Caudill
**  in Think C 4.0 on a Macintosh
**
**  (c) Maureen Caudill 1988-1991
**  This network will accept 5x7 input patterns
**  and produce 8 bit output patterns.
**  The source code may be copied or modified without restriction,
**  but no fee may be charged for its use.
**
** ++++++++++++++
** I have modified the code so that it will work
** on systems other than a Macintosh -- RG
*/

/***********
** DoNNet **
************
** Perform the neural net benchmark.
** Note that this benchmark is one of the few that
** requires an input file.  That file is "NNET.DAT" and
** should be on the local directory (from which the
** benchmark program in launched).
*/
void DoNNET(void)
{
NNetStruct *locnnetstruct;      /* Local ptr to global data */
char *errorcontext;
ulong accumtime;
double iterations;

/*
** Link to global data
*/
locnnetstruct=&global_nnetstruct;

/*
** Set error context
*/
errorcontext="CPU:NNET";

/*
** Init random number generator.
** NOTE: It is important that the random number generator
**  be re-initialized for every pass through this test.
**  The NNET algorithm uses the random number generator
**  to initialize the net.  Results are sensitive to
**  the initial neural net state.
*/
/* randnum(3L); */
randnum((int32)3);

/*
** Read in the input and output patterns.  We'll do this
** only once here at the beginning.  These values don't
** change once loaded.
*/
if(read_data_file()!=0)
   ErrorExit();


/*
** See if we need to perform self adjustment loop.
*/
if(locnnetstruct->adjust==0)
{
	/*
	** Do self-adjustment.  This involves initializing the
	** # of loops and increasing the loop count until we
	** get a number of loops that we can use.
	*/
	for(locnnetstruct->loops=1L;
	  locnnetstruct->loops<MAXNNETLOOPS;
	  locnnetstruct->loops++)
	  {     /*randnum(3L); */
		randnum((int32)3);
		if(DoNNetIteration(locnnetstruct->loops)
			>global_min_ticks) break;
	  }
}

/*
** All's well if we get here.  Do the test.
*/
accumtime=0L;
iterations=(double)0.0;

do {
	/* randnum(3L); */    /* Gotta do this for Neural Net */
	randnum((int32)3);    /* Gotta do this for Neural Net */
	accumtime+=DoNNetIteration(locnnetstruct->loops);
	iterations+=(double)locnnetstruct->loops;
} while(TicksToSecs(accumtime)<locnnetstruct->request_secs);

/*
** Clean up, calculate results, and go home.  Be sure to
** show that we don't have to rerun adjustment code.
*/
locnnetstruct->iterspersec=iterations / TicksToFracSecs(accumtime);

if(locnnetstruct->adjust==0)
	locnnetstruct->adjust=1;


return;
}

/********************
** DoNNetIteration **
*********************
** Do a single iteration of the neural net benchmark.
** By iteration, we mean a "learning" pass.
*/
static ulong DoNNetIteration(ulong nloops)
{
ulong elapsed;          /* Elapsed time */
int patt;

/*
** Run nloops learning cycles.  Notice that, counted with
** the learning cycle is the weight randomization and
** zeroing of changes.  This should reduce clock jitter,
** since we don't have to stop and start the clock for
** each iteration.
*/
elapsed=StartStopwatch();
while(nloops--)
{
	randomize_wts();
	zero_changes();
	iteration_count=1;
	learned = F;
	numpasses = 0;
	while (learned == F)
	{
		for (patt=0; patt<numpats; patt++)
		{
			worst_error = 0.0;      /* reset this every pass through data */
			move_wt_changes();      /* move last pass's wt changes to momentum array */
			do_forward_pass(patt);
			do_back_pass(patt);
			iteration_count++;
		}
		numpasses ++;
		learned = check_out_error();
	}
#ifdef DEBUG
printf("Learned in %d passes\n",numpasses);
#endif
}
return(StopStopwatch(elapsed));
}

/*************************
** do_mid_forward(patt) **
**************************
** Process the middle layer's forward pass
** The activation of middle layer's neurode is the weighted
** sum of the inputs from the input pattern, with sigmoid
** function applied to the inputs.
**/
static void  do_mid_forward(int patt)
{
double  sum;
int     neurode, i;

for (neurode=0;neurode<MID_SIZE; neurode++)
{
	sum = 0.0;
	for (i=0; i<IN_SIZE; i++)
	{       /* compute weighted sum of input signals */
		sum += mid_wts[neurode][i]*in_pats[patt][i];
	}
	/*
	** apply sigmoid function f(x) = 1/(1+exp(-x)) to weighted sum
	*/
	sum = 1.0/(1.0+exp(-sum));
	mid_out[neurode] = sum;
}
return;
}

/*********************
** do_out_forward() **
**********************
** process the forward pass through the output layer
** The activation of the output layer is the weighted sum of
** the inputs (outputs from middle layer), modified by the
** sigmoid function.
**/
static void  do_out_forward()
{
double sum;
int neurode, i;

for (neurode=0; neurode<OUT_SIZE; neurode++)
{
	sum = 0.0;
	for (i=0; i<MID_SIZE; i++)
	{       /*
		** compute weighted sum of input signals
		** from middle layer
		*/
		sum += out_wts[neurode][i]*mid_out[i];
	}
	/*
	** Apply f(x) = 1/(1+exp(-x)) to weighted input
	*/
	sum = 1.0/(1.0+exp(-sum));
	out_out[neurode] = sum;
}
return;
}

/*************************
** display_output(patt) **
**************************
** Display the actual output vs. the desired output of the
** network.
** Once the training is complete, and the "learned" flag set
** to TRUE, then display_output sends its output to both
** the screen and to a text output file.
**
** NOTE: This routine has been disabled in the benchmark
** version. -- RG
**/
/*
void  display_output(int patt)
{
int             i;

	fprintf(outfile,"\n Iteration # %d",iteration_count);
	fprintf(outfile,"\n Desired Output:  ");

	for (i=0; i<OUT_SIZE; i++)
	{
		fprintf(outfile,"%6.3f  ",out_pats[patt][i]);
	}
	fprintf(outfile,"\n Actual Output:   ");

	for (i=0; i<OUT_SIZE; i++)
	{
		fprintf(outfile,"%6.3f  ",out_out[i]);
	}
	fprintf(outfile,"\n");
	return;
}
*/

/**********************
** do_forward_pass() **
***********************
** control function for the forward pass through the network
** NOTE: I have disabled the call to display_output() in
**  the benchmark version -- RG.
**/
static void  do_forward_pass(int patt)
{
do_mid_forward(patt);   /* process forward pass, middle layer */
do_out_forward();       /* process forward pass, output layer */
/* display_output(patt);        ** display results of forward pass */
return;
}

/***********************
** do_out_error(patt) **
************************
** Compute the error for the output layer neurodes.
** This is simply Desired - Actual.
**/
static void do_out_error(int patt)
{
int neurode;
double error,tot_error, sum;

tot_error = 0.0;
sum = 0.0;
for (neurode=0; neurode<OUT_SIZE; neurode++)
{
	out_error[neurode] = out_pats[patt][neurode] - out_out[neurode];
	/*
	** while we're here, also compute magnitude
	** of total error and worst error in this pass.
	** We use these to decide if we are done yet.
	*/
	error = out_error[neurode];
	if (error <0.0)
	{
		sum += -error;
		if (-error > tot_error)
			tot_error = -error; /* worst error this pattern */
	}
	else
	{
		sum += error;
		if (error > tot_error)
			tot_error = error; /* worst error this pattern */
	}
}
avg_out_error[patt] = sum/OUT_SIZE;
tot_out_error[patt] = tot_error;
return;
}

/***********************
** worst_pass_error() **
************************
** Find the worst and average error in the pass and save it
**/
static void  worst_pass_error()
{
double error,sum;

int i;

error = 0.0;
sum = 0.0;
for (i=0; i<numpats; i++)
{
	if (tot_out_error[i] > error) error = tot_out_error[i];
	sum += avg_out_error[i];
}
worst_error = error;
average_error = sum/numpats;
return;
}

/*******************
** do_mid_error() **
********************
** Compute the error for the middle layer neurodes
** This is based on the output errors computed above.
** Note that the derivative of the sigmoid f(x) is
**        f'(x) = f(x)(1 - f(x))
** Recall that f(x) is merely the output of the middle
** layer neurode on the forward pass.
**/
static void do_mid_error()
{
double sum;
int neurode, i;

for (neurode=0; neurode<MID_SIZE; neurode++)
{
	sum = 0.0;
	for (i=0; i<OUT_SIZE; i++)
		sum += out_wts[i][neurode]*out_error[i];

	/*
	** apply the derivative of the sigmoid here
	** Because of the choice of sigmoid f(I), the derivative
	** of the sigmoid is f'(I) = f(I)(1 - f(I))
	*/
	mid_error[neurode] = mid_out[neurode]*(1-mid_out[neurode])*sum;
}
return;
}

/*********************
** adjust_out_wts() **
**********************
** Adjust the weights of the output layer.  The error for
** the output layer has been previously propagated back to
** the middle layer.
** Use the Delta Rule with momentum term to adjust the weights.
**/
static void adjust_out_wts()
{
int weight, neurode;
double learn,delta,alph;

learn = BETA;
alph  = ALPHA;
for (neurode=0; neurode<OUT_SIZE; neurode++)
{
	for (weight=0; weight<MID_SIZE; weight++)
	{
		/* standard delta rule */
		delta = learn * out_error[neurode] * mid_out[weight];

		/* now the momentum term */
		delta += alph * out_wt_change[neurode][weight];
		out_wts[neurode][weight] += delta;

		/* keep track of this pass's cum wt changes for next pass's momentum */
		out_wt_cum_change[neurode][weight] += delta;
	}
}
return;
}

/*************************
** adjust_mid_wts(patt) **
**************************
** Adjust the middle layer weights using the previously computed
** errors.
** We use the Generalized Delta Rule with momentum term
**/
static void adjust_mid_wts(int patt)
{
int weight, neurode;
double learn,alph,delta;

learn = BETA;
alph  = ALPHA;
for (neurode=0; neurode<MID_SIZE; neurode++)
{
	for (weight=0; weight<IN_SIZE; weight++)
	{
		/* first the basic delta rule */
		delta = learn * mid_error[neurode] * in_pats[patt][weight];

		/* with the momentum term */
		delta += alph * mid_wt_change[neurode][weight];
		mid_wts[neurode][weight] += delta;

		/* keep track of this pass's cum wt changes for next pass's momentum */
		mid_wt_cum_change[neurode][weight] += delta;
	}
}
return;
}

/*******************
** do_back_pass() **
********************
** Process the backward propagation of error through network.
**/
void  do_back_pass(int patt)
{

do_out_error(patt);
do_mid_error();
adjust_out_wts();
adjust_mid_wts(patt);

return;
}


/**********************
** move_wt_changes() **
***********************
** Move the weight changes accumulated last pass into the wt-change
** array for use by the momentum term in this pass. Also zero out
** the accumulating arrays after the move.
**/
static void move_wt_changes()
{
int i,j;

for (i = 0; i<MID_SIZE; i++)
	for (j = 0; j<IN_SIZE; j++)
	{
		mid_wt_change[i][j] = mid_wt_cum_change[i][j];
		/*
		** Zero it out for next pass accumulation.
		*/
		mid_wt_cum_change[i][j] = 0.0;
	}

for (i = 0; i<OUT_SIZE; i++)
	for (j=0; j<MID_SIZE; j++)
	{
		out_wt_change[i][j] = out_wt_cum_change[i][j];
		out_wt_cum_change[i][j] = 0.0;
	}

return;
}

/**********************
** check_out_error() **
***********************
** Check to see if the error in the output layer is below
** MARGIN*OUT_SIZE for all output patterns.  If so, then
** assume the network has learned acceptably well.  This
** is simply an arbitrary measure of how well the network
** has learned -- many other standards are possible.
**/
static int check_out_error()
{
int result,i,error;

result  = T;
error   = F;
worst_pass_error();     /* identify the worst error in this pass */

/*
#ifdef DEBUG
printf("\n Iteration # %d",iteration_count);
#endif
*/
for (i=0; i<numpats; i++)
{
/*      printf("\n Error pattern %d:   Worst: %8.3f; Average: %8.3f",
	  i+1,tot_out_error[i], avg_out_error[i]);
	fprintf(outfile,
	 "\n Error pattern %d:   Worst: %8.3f; Average: %8.3f",
	 i+1,tot_out_error[i]);
*/

	if (worst_error >= STOP) result = F;
	if (tot_out_error[i] >= 16.0) error = T;
}

if (error == T) result = ERR;


#ifdef DEBUG
/* printf("\n Error this pass thru data:   Worst: %8.3f; Average: %8.3f",
 worst_error,average_error);
*/
/* fprintf(outfile,
 "\n Error this pass thru data:   Worst: %8.3f; Average: %8.3f",
  worst_error, average_error); */
#endif

return(result);
}


/*******************
** zero_changes() **
********************
** Zero out all the wt change arrays
**/
static void zero_changes()
{
int i,j;

for (i = 0; i<MID_SIZE; i++)
{
	for (j=0; j<IN_SIZE; j++)
	{
		mid_wt_change[i][j] = 0.0;
		mid_wt_cum_change[i][j] = 0.0;
	}
}

for (i = 0; i< OUT_SIZE; i++)
{
	for (j=0; j<MID_SIZE; j++)
	{
		out_wt_change[i][j] = 0.0;
		out_wt_cum_change[i][j] = 0.0;
	}
}
return;
}


/********************
** randomize_wts() **
*********************
** Intialize the weights in the middle and output layers to
** random values between -0.25..+0.25
** Function rand() returns a value between 0 and 32767.
**
** NOTE: Had to make alterations to how the random numbers were
** created.  -- RG.
**/
static void randomize_wts()
{
int neurode,i;
double value;

/*
** Following not used int benchmark version -- RG
**
**        printf("\n Please enter a random number seed (1..32767):  ");
**        scanf("%d", &i);
**        srand(i);
*/

for (neurode = 0; neurode<MID_SIZE; neurode++)
{
	for(i=0; i<IN_SIZE; i++)
	{
	        /* value=(double)abs_randwc(100000L); */
		value=(double)abs_randwc((int32)100000);
		value=value/(double)100000.0 - (double) 0.5;
		mid_wts[neurode][i] = value/2;
	}
}
for (neurode=0; neurode<OUT_SIZE; neurode++)
{
	for(i=0; i<MID_SIZE; i++)
	{
	        /* value=(double)abs_randwc(100000L); */
		value=(double)abs_randwc((int32)100000);
		value=value/(double)10000.0 - (double) 0.5;
		out_wts[neurode][i] = value/2;
	}
}

return;
}


/*********************
** read_data_file() **
**********************
** Read in the input data file and store the patterns in
** in_pats and out_pats.
** The format for the data file is as follows:
**
** line#   data expected
** -----   ------------------------------
** 1               In-X-size,in-y-size,out-size
** 2               number of patterns in file
** 3               1st X row of 1st input pattern
** 4..             following rows of 1st input pattern pattern
**                 in-x+2  y-out pattern
**                                 1st X row of 2nd pattern
**                 etc.
**
** Each row of data is separated by commas or spaces.
** The data is expected to be ascii text corresponding to
** either a +1 or a 0.
**
** Sample input for a 1-pattern file (The comments to the
** right may NOT be in the file unless more sophisticated
** parsing of the input is done.):
**
** 5,7,8                      input is 5x7 grid, output is 8 bits
** 1                          one pattern in file
** 0,1,1,1,0                  beginning of pattern for "O"
** 1,0,0,0,1
** 1,0,0,0,1
** 1,0,0,0,1
** 1,0,0,0,1
** 1,0,0,0,0
** 0,1,1,1,0
** 0,1,0,0,1,1,1,1            ASCII code for "O" -- 0100 1111
**
** Clearly, this simple scheme can be expanded or enhanced
** any way you like.
**
** Returns -1 if any file error occurred, otherwise 0.
**/
static int read_data_file()
{
FILE *infile;

int xinsize,yinsize,youtsize;
int patt, element, i, row;
int vals_read;
int val1,val2,val3,val4,val5,val6,val7,val8;

/* printf("\n Opening and retrieving data from file."); */

infile = fopen(inpath, "r");
if (infile == NULL)
{
	printf("\n CPU:NNET--error in opening file!");
	return -1 ;
}
vals_read =fscanf(infile,"%d  %d  %d",&xinsize,&yinsize,&youtsize);
if (vals_read != 3)
{
	printf("\n CPU:NNET -- Should read 3 items in line one; did read %d",vals_read);
	return -1;
}
vals_read=fscanf(infile,"%d",&numpats);
if (vals_read !=1)
{
	printf("\n CPU:NNET -- Should read 1 item in line 2; did read %d",vals_read);
	return -1;
}
if (numpats > MAXPATS)
	numpats = MAXPATS;

for (patt=0; patt<numpats; patt++)
{
	element = 0;
	for (row = 0; row<yinsize; row++)
	{
		vals_read = fscanf(infile,"%d  %d  %d  %d  %d",
			&val1, &val2, &val3, &val4, &val5);
		if (vals_read != 5)
		{
			printf ("\n CPU:NNET -- failure in reading input!");
			return -1;
		}
		element=row*xinsize;

		in_pats[patt][element] = (double) val1; element++;
		in_pats[patt][element] = (double) val2; element++;
		in_pats[patt][element] = (double) val3; element++;
		in_pats[patt][element] = (double) val4; element++;
		in_pats[patt][element] = (double) val5; element++;
	}
	for (i=0;i<IN_SIZE; i++)
	{
		if (in_pats[patt][i] >= 0.9)
			in_pats[patt][i] = 0.9;
		if (in_pats[patt][i] <= 0.1)
			in_pats[patt][i] = 0.1;
	}
	element = 0;
	vals_read = fscanf(infile,"%d  %d  %d  %d  %d  %d  %d  %d",
		&val1, &val2, &val3, &val4, &val5, &val6, &val7, &val8);

	out_pats[patt][element] = (double) val1; element++;
	out_pats[patt][element] = (double) val2; element++;
	out_pats[patt][element] = (double) val3; element++;
	out_pats[patt][element] = (double) val4; element++;
	out_pats[patt][element] = (double) val5; element++;
	out_pats[patt][element] = (double) val6; element++;
	out_pats[patt][element] = (double) val7; element++;
	out_pats[patt][element] = (double) val8; element++;
}

/* printf("\n Closing the input file now. "); */

fclose(infile);
return(0);
}

/*********************
** initialize_net() **
**********************
** Do all the initialization stuff before beginning
*/
/*
static int initialize_net()
{
int err_code;

randomize_wts();
zero_changes();
err_code = read_data_file();
iteration_count = 1;
return(err_code);
}
*/

/**********************
** display_mid_wts() **
***********************
** Display the weights on the middle layer neurodes
** NOTE: This routine is not used in the benchmark
**  test -- RG
**/
/* static void display_mid_wts()
{
int             neurode, weight, row, col;

fprintf(outfile,"\n Weights of Middle Layer neurodes:");

for (neurode=0; neurode<MID_SIZE; neurode++)
{
	fprintf(outfile,"\n  Mid Neurode # %d",neurode);
	for (row=0; row<IN_Y_SIZE; row++)
	{
		fprintf(outfile,"\n ");
		for (col=0; col<IN_X_SIZE; col++)
		{
			weight = IN_X_SIZE * row + col;
			fprintf(outfile," %8.3f ", mid_wts[neurode][weight]);
		}
	}
}
return;
}
*/
/**********************
** display_out_wts() **
***********************
** Display the weights on the output layer neurodes
** NOTE: This code is not used in the benchmark
**  test -- RG
*/
/* void  display_out_wts()
{
int             neurode, weight;

	fprintf(outfile,"\n Weights of Output Layer neurodes:");

	for (neurode=0; neurode<OUT_SIZE; neurode++)
	{
		fprintf(outfile,"\n  Out Neurode # %d \n",neurode);
		for (weight=0; weight<MID_SIZE; weight++)
		{
			fprintf(outfile," %8.3f ", out_wts[neurode][weight]);
		}
	}
	return;
}
*/

/***********************
**  LU DECOMPOSITION  **
** (Linear Equations) **
************************
** These routines come from "Numerical Recipes in Pascal".
** Note that, as in the assignment algorithm, though we
** separately define LUARRAYROWS and LUARRAYCOLS, the two
** must be the same value (this routine depends on a square
** matrix).
*/

/*********
** DoLU **
**********
** Perform the LU decomposition benchmark.
*/
void DoLU(void)
{
LUStruct *loclustruct;  /* Local pointer to global data */
char *errorcontext;
int systemerror;
fardouble *a;
fardouble *b;
fardouble *abase;
fardouble *bbase;
LUdblptr ptra;
int n;
int i;
ulong accumtime;
double iterations;

/*
** Link to global data
*/
loclustruct=&global_lustruct;

/*
** Set error context.
*/
errorcontext="FPU:LU";

/*
** Our first step is to build a "solvable" problem.  This
** will become the "seed" set that all others will be
** derived from. (I.E., we'll simply copy these arrays
** into the others.
*/
a=(fardouble *)AllocateMemory(sizeof(double) * LUARRAYCOLS * LUARRAYROWS,
		&systemerror);
b=(fardouble *)AllocateMemory(sizeof(double) * LUARRAYROWS,
		&systemerror);
n=LUARRAYROWS;

/*
** We need to allocate a temp vector that is used by the LU
** algorithm.  This removes the allocation routine from the
** timing.
*/
LUtempvv=(fardouble *)AllocateMemory(sizeof(double)*LUARRAYROWS,
	&systemerror);

/*
** Build a problem to be solved.
*/
ptra.ptrs.p=a;                  /* Gotta coerce linear array to 2D array */
build_problem(*ptra.ptrs.ap,n,b);

/*
** Now that we have a problem built, see if we need to do
** auto-adjust.  If so, repeatedly call the DoLUIteration routine,
** increasing the number of solutions per iteration as you go.
*/
if(loclustruct->adjust==0)
{
	loclustruct->numarrays=0;
	for(i=1;i<=MAXLUARRAYS;i++)
	{
		abase=(fardouble *)AllocateMemory(sizeof(double) *
			LUARRAYCOLS*LUARRAYROWS*(i+1),&systemerror);
		if(systemerror)
		{       ReportError(errorcontext,systemerror);
			LUFreeMem(a,b,(fardouble *)NULL,(fardouble *)NULL);
			ErrorExit();
		}
		bbase=(fardouble *)AllocateMemory(sizeof(double) *
			LUARRAYROWS*(i+1),&systemerror);
		if(systemerror)
		{       ReportError(errorcontext,systemerror);
			LUFreeMem(a,b,abase,(fardouble *)NULL);
			ErrorExit();
		}
		if(DoLUIteration(a,b,abase,bbase,i)>global_min_ticks)
		{       loclustruct->numarrays=i;
			break;
		}
		/*
		** Not enough arrays...free them all and try again
		*/
		FreeMemory((farvoid *)abase,&systemerror);
		FreeMemory((farvoid *)bbase,&systemerror);
	}
	/*
	** Were we able to do it?
	*/
	if(loclustruct->numarrays==0)
	{       printf("FPU:LU -- Array limit reached\n");
		LUFreeMem(a,b,abase,bbase);
		ErrorExit();
	}
}
else
{       /*
	** Don't need to adjust -- just allocate the proper
	** number of arrays and proceed.
	*/
	abase=(fardouble *)AllocateMemory(sizeof(double) *
		LUARRAYCOLS*LUARRAYROWS*loclustruct->numarrays,
		&systemerror);
	if(systemerror)
	{       ReportError(errorcontext,systemerror);
		LUFreeMem(a,b,(fardouble *)NULL,(fardouble *)NULL);
		ErrorExit();
	}
	bbase=(fardouble *)AllocateMemory(sizeof(double) *
		LUARRAYROWS*loclustruct->numarrays,&systemerror);
	if(systemerror)
	{
		ReportError(errorcontext,systemerror);
		LUFreeMem(a,b,abase,(fardouble *)NULL);
		ErrorExit();
	}
}
/*
** All's well if we get here.  Do the test.
*/
accumtime=0L;
iterations=(double)0.0;

do {
	accumtime+=DoLUIteration(a,b,abase,bbase,
		loclustruct->numarrays);
	iterations+=(double)loclustruct->numarrays;
} while(TicksToSecs(accumtime)<loclustruct->request_secs);

/*
** Clean up, calculate results, and go home.  Be sure to
** show that we don't have to rerun adjustment code.
*/
loclustruct->iterspersec=iterations / TicksToFracSecs(accumtime);

if(loclustruct->adjust==0)
	loclustruct->adjust=1;

LUFreeMem(a,b,abase,bbase);
return;
}

/**************
** LUFreeMem **
***************
** Release memory associated with LU benchmark.
*/
static void LUFreeMem(fardouble *a, fardouble *b,
			fardouble *abase,fardouble *bbase)
{
int systemerror;

FreeMemory((farvoid *)a,&systemerror);
FreeMemory((farvoid *)b,&systemerror);
FreeMemory((farvoid *)LUtempvv,&systemerror);

if(abase!=(fardouble *)NULL) FreeMemory((farvoid *)abase,&systemerror);
if(bbase!=(fardouble *)NULL) FreeMemory((farvoid *)bbase,&systemerror);
return;
}

/******************
** DoLUIteration **
*******************
** Perform an iteration of the LU decomposition benchmark.
** An iteration refers to the repeated solution of several
** identical matrices.
*/
static ulong DoLUIteration(fardouble *a,fardouble *b,
		fardouble *abase, fardouble *bbase,
		ulong numarrays)
{
fardouble *locabase;
fardouble *locbbase;
LUdblptr ptra;  /* For converting ptr to 2D array */
ulong elapsed;
ulong j,i;              /* Indexes */


/*
** Move the seed arrays (a & b) into the destination
** arrays;
*/
for(j=0;j<numarrays;j++)
{       locabase=abase+j*LUARRAYROWS*LUARRAYCOLS;
	locbbase=bbase+j*LUARRAYROWS;
	for(i=0;i<LUARRAYROWS*LUARRAYCOLS;i++)
		*(locabase+i)=*(a+i);
	for(i=0;i<LUARRAYROWS;i++)
		*(locbbase+i)=*(b+i);
}

/*
** Do test...begin timing.
*/
elapsed=StartStopwatch();
for(i=0;i<numarrays;i++)
{       locabase=abase+i*LUARRAYROWS*LUARRAYCOLS;
	locbbase=bbase+i*LUARRAYROWS;
	ptra.ptrs.p=locabase;
	lusolve(*ptra.ptrs.ap,LUARRAYROWS,locbbase);
}

return(StopStopwatch(elapsed));
}

/******************
** build_problem **
*******************
** Constructs a solvable set of linear equations.  It does this by
** creating an identity matrix, then loading the solution vector
** with random numbers.  After that, the identity matrix and
** solution vector are randomly "scrambled".  Scrambling is
** done by (a) randomly selecting a row and multiplying that
** row by a random number and (b) adding one randomly-selected
** row to another.
*/
static void build_problem(double a[][LUARRAYCOLS],
		int n,
		double b[LUARRAYROWS])
{
long i,j,k,k1;  /* Indexes */
double rcon;     /* Random constant */

/*
** Reset random number generator
*/
/* randnum(13L); */
randnum((int32)13);

/*
** Build an identity matrix.
** We'll also use this as a chance to load the solution
** vector.
*/
for(i=0;i<n;i++)
{       /* b[i]=(double)(abs_randwc(100L)+1L); */
	b[i]=(double)(abs_randwc((int32)100)+(int32)1);
	for(j=0;j<n;j++)
		if(i==j)
		        /* a[i][j]=(double)(abs_randwc(1000L)+1L); */
			a[i][j]=(double)(abs_randwc((int32)1000)+(int32)1);
		else
			a[i][j]=(double)0.0;
}

#ifdef DEBUG
printf("Problem:\n");
for(i=0;i<n;i++)
{
/*
	for(j=0;j<n;j++)
		printf("%6.2f ",a[i][j]);
*/
	printf("%.0f/%.0f=%.2f\t",b[i],a[i][i],b[i]/a[i][i]);
/*
        printf("\n");
*/
}
#endif

/*
** Scramble.  Do this 8n times.  See comment above for
** a description of the scrambling process.
*/

for(i=0;i<8*n;i++)
{
	/*
	** Pick a row and a random constant.  Multiply
	** all elements in the row by the constant.
	*/
 /*       k=abs_randwc((long)n);
	rcon=(double)(abs_randwc(20L)+1L);
	for(j=0;j<n;j++)
		a[k][j]=a[k][j]*rcon;
	b[k]=b[k]*rcon;
*/
	/*
	** Pick two random rows and add second to
	** first.  Note that we also occasionally multiply
	** by minus 1 so that we get a subtraction operation.
	*/
        /* k=abs_randwc((long)n); */
        /* k1=abs_randwc((long)n); */
	k=abs_randwc((int32)n);
	k1=abs_randwc((int32)n);
	if(k!=k1)
	{
		if(k<k1) rcon=(double)1.0;
			else rcon=(double)-1.0;
		for(j=0;j<n;j++)
			a[k][j]+=a[k1][j]*rcon;;
		b[k]+=b[k1]*rcon;
	}
}

return;
}


/***********
** ludcmp **
************
** From the procedure of the same name in "Numerical Recipes in Pascal",
** by Press, Flannery, Tukolsky, and Vetterling.
** Given an nxn matrix a[], this routine replaces it by the LU
** decomposition of a rowwise permutation of itself.  a[] and n
** are input.  a[] is output, modified as follows:
**   --                       --
**  |  b(1,1) b(1,2) b(1,3)...  |
**  |  a(2,1) b(2,2) b(2,3)...  |
**  |  a(3,1) a(3,2) b(3,3)...  |
**  |  a(4,1) a(4,2) a(4,3)...  |
**  |  ...                      |
**   --                        --
**
** Where the b(i,j) elements form the upper triangular matrix of the
** LU decomposition, and the a(i,j) elements form the lower triangular
** elements.  The LU decomposition is calculated so that we don't
** need to store the a(i,i) elements (which would have laid along the
** diagonal and would have all been 1).
**
** indx[] is an output vector that records the row permutation
** effected by the partial pivoting; d is output as +/-1 depending
** on whether the number of row interchanges was even or odd,
** respectively.
** Returns 0 if matrix singular, else returns 1.
*/
static int ludcmp(double a[][LUARRAYCOLS],
		int n,
		int indx[],
		int *d)
{

double big;     /* Holds largest element value */
double sum;
double dum;     /* Holds dummy value */
int i,j,k;      /* Indexes */
int imax=0;     /* Holds max index value */
double tiny;    /* A really small number */

tiny=(double)1.0e-20;

*d=1;           /* No interchanges yet */

for(i=0;i<n;i++)
{       big=(double)0.0;
	for(j=0;j<n;j++)
		if((double)fabs(a[i][j]) > big)
			big=fabs(a[i][j]);
	/* Bail out on singular matrix */
	if(big==(double)0.0) return(0);
	LUtempvv[i]=1.0/big;
}

/*
** Crout's algorithm...loop over columns.
*/
for(j=0;j<n;j++)
{       if(j!=0)
		for(i=0;i<j;i++)
		{       sum=a[i][j];
			if(i!=0)
				for(k=0;k<i;k++)
					sum-=(a[i][k]*a[k][j]);
			a[i][j]=sum;
		}
	big=(double)0.0;
	for(i=j;i<n;i++)
	{       sum=a[i][j];
		if(j!=0)
			for(k=0;k<j;k++)
				sum-=a[i][k]*a[k][j];
		a[i][j]=sum;
		dum=LUtempvv[i]*fabs(sum);
		if(dum>=big)
		{       big=dum;
			imax=i;
		}
	}
	if(j!=imax)             /* Interchange rows if necessary */
	{       for(k=0;k<n;k++)
		{       dum=a[imax][k];
			a[imax][k]=a[j][k];
			a[j][k]=dum;
		}
		*d=-*d;         /* Change parity of d */
		dum=LUtempvv[imax];
		LUtempvv[imax]=LUtempvv[j]; /* Don't forget scale factor */
		LUtempvv[j]=dum;
	}
	indx[j]=imax;
	/*
	** If the pivot element is zero, the matrix is singular
	** (at least as far as the precision of the machine
	** is concerned.)  We'll take the original author's
	** recommendation and replace 0.0 with "tiny".
	*/
	if(a[j][j]==(double)0.0)
		a[j][j]=tiny;

	if(j!=(n-1))
	{       dum=1.0/a[j][j];
		for(i=j+1;i<n;i++)
			a[i][j]=a[i][j]*dum;
	}
}

return(1);
}

/***********
** lubksb **
************
** Also from "Numerical Recipes in Pascal".
** This routine solves the set of n linear equations A X = B.
** Here, a[][] is input, not as the matrix A, but as its
** LU decomposition, created by the routine ludcmp().
** Indx[] is input as the permutation vector returned by ludcmp().
**  b[] is input as the right-hand side an returns the
** solution vector X.
** a[], n, and indx are not modified by this routine and
** can be left in place for different values of b[].
** This routine takes into account the possibility that b will
** begin with many zero elements, so it is efficient for use in
** matrix inversion.
*/
static void lubksb( double a[][LUARRAYCOLS],
		int n,
		int indx[LUARRAYROWS],
		double b[LUARRAYROWS])
{

int i,j;        /* Indexes */
int ip;         /* "pointer" into indx */
int ii;
double sum;

/*
** When ii is set to a positive value, it will become
** the index of the first nonvanishing element of b[].
** We now do the forward substitution. The only wrinkle
** is to unscramble the permutation as we go.
*/
ii=-1;
for(i=0;i<n;i++)
{       ip=indx[i];
	sum=b[ip];
	b[ip]=b[i];
	if(ii!=-1)
		for(j=ii;j<i;j++)
			sum=sum-a[i][j]*b[j];
	else
		/*
		** If a nonzero element is encountered, we have
		** to do the sums in the loop above.
		*/
		if(sum!=(double)0.0)
			ii=i;
	b[i]=sum;
}
/*
** Do backsubstitution
*/
for(i=(n-1);i>=0;i--)
{
	sum=b[i];
	if(i!=(n-1))
		for(j=(i+1);j<n;j++)
			sum=sum-a[i][j]*b[j];
	b[i]=sum/a[i][i];
}
return;
}

/************
** lusolve **
*************
** Solve a linear set of equations: A x = b
** Original matrix A will be destroyed by this operation.
** Returns 0 if matrix is singular, 1 otherwise.
*/
static int lusolve(double a[][LUARRAYCOLS],
		int n,
		double b[LUARRAYROWS])
{
int indx[LUARRAYROWS];
int d;
#ifdef DEBUG
int i,j;
#endif

if(ludcmp(a,n,indx,&d)==0) return(0);

/* Matrix not singular -- proceed */
lubksb(a,n,indx,b);

#ifdef DEBUG
printf("Solution:\n");
for(i=0;i<n;i++)
{
  for(j=0;j<n;j++){
  /*
    printf("%6.2f ",a[i][j]);
  */
  }
  printf("%6.2f\t",b[i]);
  /*
    printf("\n");
  */
}
printf("\n");
#endif

return(1);
}
