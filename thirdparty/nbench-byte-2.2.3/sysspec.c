
/*
** sysspec.c
** System-specific routines.
**
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

/***********************************
**    SYSTEM-SPECIFIC ROUTINES    **
************************************
**
** These are the routines that provide functions that are
** system-specific.  If the benchmarks are to be ported
** to new hardware/new O.S., this is the first place to
** start.
*/
#include "sysspec.h"

#ifdef DOS16
#include <io.h>
#include <fcntl.h>
#include <sys\stat.h>
#endif
/*********************************
**  MEMORY MANAGEMENT ROUTINES  **
*********************************/


/****************************
** AllocateMemory
** This routine returns a void pointer to a memory
** block.  The size of the memory block is given in bytes
** as the first argument.  This routine also returns an
** error code in the second argument.
** 10/95 Update:
**  Added an associative array for memory alignment reasons.
**  mem_array[2][MEM_ARRAY_SIZE]
**   mem_array[0][n] = Actual address (from malloc)
**   mem_array[1][n] = Aligned address
** Currently, mem_array[][] is only used if you use malloc;
**  it is not used for the 16-bit DOS and MAC versions.
*/
farvoid *AllocateMemory(unsigned long nbytes,   /* # of bytes to alloc */
		int *errorcode)                 /* Returned error code */
{
#ifdef DOS16MEM
union REGS registers;
unsigned short nparas;            /* # of paragraphs */

/*
** Set # of paragraphs to nbytes/16 +1.  The +1 is a
** slop factor.
*/
nparas=(unsigned short)(nbytes/16L) + 1;

/*
** Set incoming registers.
*/
registers.h.ah=0x48;            /* Allocate memory */
registers.x.bx=nparas;          /* # of paragraphs */


intdos(&registers,&registers);  /* Call DOS */

/*
** See if things succeeded.
*/
if(registers.x.cflag)
{       printf("error: %d Lgst: %d\n",registers.x.ax,registers.x.bx);
	    *errorcode=ERROR_MEMORY;
	return((farvoid *)NULL);
}

/*
** Create a farvoid pointer to return.
*/
*errorcode=0;
return((farvoid *)MK_FP(registers.x.ax,0));

#endif

#ifdef MACMEM
/*
** For MAC CodeWarrior, we'll use the MacOS NewPtr call
*/
farvoid *returnval;
returnval=(farvoid *)NewPtr((Size)nbytes);
if(returnval==(farvoid *)NULL)
	*errorcode=ERROR_MEMORY;
else
	*errorcode=0;
return(returnval);
#endif

#ifdef MALLOCMEM
/*
** Everyone else, its pretty straightforward, given
** that you use a 32-bit compiler which treats size_t as
** a 4-byte entity.
*/
farvoid *returnval;             /* Return value */
ulong true_addr;		/* True address */
ulong adj_addr;			/* Adjusted address */

returnval=(farvoid *)malloc((size_t)(nbytes+2L*(long)global_align));
if(returnval==(farvoid *)NULL)
	*errorcode=ERROR_MEMORY;
else
	*errorcode=0;

/*
** Check for alignment
*/
adj_addr=true_addr=(ulong)returnval;
if(global_align==0)
{	
	if(AddMemArray(true_addr, adj_addr))
		*errorcode=ERROR_MEMARRAY_FULL;
	return(returnval);
}

if(global_align==1)
{	
        if(true_addr%2==0) adj_addr++;
}
else
{	
	while(adj_addr%global_align!=0) ++adj_addr;
	if(adj_addr%(global_align*2)==0) adj_addr+=global_align;
}
returnval=(void *)adj_addr;
if(AddMemArray(true_addr,adj_addr))
	*errorcode=ERROR_MEMARRAY_FULL;
return(returnval);
#endif

}


/****************************
** FreeMemory
** This is the reverse of AllocateMemory.  The memory
** block passed in is freed.  Should an error occur,
** that error is returned in errorcode.
*/
void FreeMemory(farvoid *mempointer,    /* Pointer to memory block */
		int *errorcode)
{
#ifdef DOS16MEM
/*
** 16-bit DOS VERSION!!
*/
unsigned int segment;
unsigned int offset;
union REGS registers;
struct SREGS sregisters;

/*
** First get the segment/offset of the farvoid pointer.
*/
segment=FP_SEG(mempointer);
offset=FP_OFF(mempointer);

/*
** Align the segment properly.  For as long as offset > 16,
** subtract 16 from offset and add 1 to segment.
*/
while(offset>=16)
{       offset-=16;
	segment++;
}

/*
** Build the call to DOS
*/
registers.h.ah=0x49;            /* Free memory */
sregisters.es=segment;

intdosx(&registers,&registers,&sregisters);

/*
** Check for error
*/
if(registers.x.cflag)
{       *errorcode=ERROR_MEMORY;
	return;
}

*errorcode=0;
return;
#endif

#ifdef MACMEM
DisposPtr((Ptr)mempointer);
*errorcode=0;
return;
#endif

#ifdef MALLOCMEM
ulong adj_addr, true_addr;

/* Locate item in memory array */
adj_addr=(ulong)mempointer;
if(RemoveMemArray(adj_addr, &true_addr))
{	*errorcode=ERROR_MEMARRAY_NFOUND;
	return;
}
mempointer=(void *)true_addr;
free(mempointer);
*errorcode=0;
return;
#endif
}

/****************************
** MoveMemory
** Moves n bytes from a to b.  Handles overlap.
** In most cases, this is just a memmove operation.
** But, not in DOS....noooo....
*/
void MoveMemory( farvoid *destination,  /* Destination address */
		farvoid *source,        /* Source address */
		unsigned long nbytes)
{

/* +++16-bit DOS VERSION+++ */
#ifdef DOS16MEM

	FarDOSmemmove( destination, source, nbytes);

#else

memmove(destination, source, nbytes);

#endif
}

#ifdef DOS16MEM

/****************************
** FarDOSmemmove
** Performs the same function as memmove for DOS when
** the arrays are defined with far pointers.
*/
void FarDOSmemmove(farvoid *destination,        /* Destination pointer */
		farvoid *source,        /* Source pointer */
		unsigned long nbytes)   /* # of bytes to move */
{
unsigned char huge *uchsource;  /* Temp source */
unsigned char huge *uchdest;    /* Temp destination */
unsigned long saddr;            /* Source "true" address */
unsigned long daddr;            /* Destination "true" address */


/*
** Get unsigned char pointer equivalents
*/
uchsource=(unsigned char huge *)source;
uchdest=(unsigned char huge *)destination;

/*
** Calculate true address of source and destination and
** compare.
*/
saddr=(unsigned long)(FP_SEG(source)*16 + FP_OFF(source));
daddr=(unsigned long)(FP_SEG(destination)*16 + FP_OFF(destination));

if(saddr > daddr)
{
	/*
	** Source is greater than destination.
	** Use a series of standard move operations.
	** We'll move 65535 bytes at a time.
	*/
	while(nbytes>=65535L)
	{       _fmemmove((farvoid *)uchdest,
			(farvoid *)uchsource,
			(size_t) 65535);
		uchsource+=65535;       /* Advance pointers */
		uchdest+=65535;
		nbytes-=65535;
	}

	/*
	** Move remaining bytes
	*/
	if(nbytes!=0L)
		_fmemmove((farvoid *)uchdest,
			(farvoid *)uchsource,
			(size_t)(nbytes & 0xFFFF));

}
else
{
	/*
	** Destination is greater than source.
	** Advance pointers to the end of their
	** respective blocks.
	*/
	uchsource+=nbytes;
	uchdest+=nbytes;

	/*
	** Again, move 65535 bytes at a time.  However,
	** "back" the pointers up before doing the
	** move.
	*/
	while(nbytes>=65535L)
	{
		uchsource-=65535;
		uchdest-=65535;
		_fmemmove((farvoid *)uchdest,
			(farvoid *)uchsource,
			(size_t) 65535);
		nbytes-=65535;
	}

	/*
	** Move remaining bytes.
	*/
	if(nbytes!=0L)
	{       uchsource-=nbytes;
		uchdest-=nbytes;
		_fmemmove((farvoid *)uchdest,
			(farvoid *)uchsource,
			(size_t)(nbytes & 0xFFFF));
	}
}
return;
}
#endif

/***********************************
** MEMORY ARRAY HANDLING ROUTINES **
***********************************/
/****************************
** InitMemArray
** Initialize the memory array.  This simply amounts to
** setting mem_array_ents to zero, indicating that there
** isn't anything in the memory array.
*/
void InitMemArray(void)
{
mem_array_ents=0;
return;
}

/***************************
** AddMemArray
** Add a pair of items to the memory array.
**  true_addr is the true address (mem_array[0][n])
**  adj_addr is the adjusted address (mem_array[0][n])
** Returns 0 if ok
** -1 if not enough room
*/
int AddMemArray(ulong true_addr,
		ulong adj_addr)
{
if(mem_array_ents>=MEM_ARRAY_SIZE)
	return(-1);

mem_array[0][mem_array_ents]=true_addr;
mem_array[1][mem_array_ents]=adj_addr;
mem_array_ents++;
return(0);
}

/*************************
** RemoveMemArray
** Given an adjusted address value (mem_array[1][n]), locate
** the entry and remove it from the mem_array.
** Also returns the associated true address.
** Returns 0 if ok
** -1 if not found.
*/
int RemoveMemArray(ulong adj_addr,ulong *true_addr)
{
int i,j;

/* Locate the item in the array. */
for(i=0;i<mem_array_ents;i++)
	if(mem_array[1][i]==adj_addr)
	{       /* Found it..bubble stuff down */
		*true_addr=mem_array[0][i];
		j=i;
		while(j+1<mem_array_ents)
		{       mem_array[0][j]=mem_array[0][j+1];
			mem_array[1][j]=mem_array[1][j+1];
			j++;
		}
		mem_array_ents--;
		return(0);      /* Return if found */
	}

/* If we made it here...something's wrong...show error */
return(-1);
}

/**********************************
**    FILE HANDLING ROUTINES     **
**********************************/

/****************************
** CreateFile
** This routine accepts a filename for an argument and
** creates that file in the current directory (unless the
** name contains a path that overrides the current directory).
** Note that the routine does not OPEN the file.
** If the file exists, it is truncated to length 0.
*/
void CreateFile(char *filename,
		int *errorcode)
{

#ifdef DOS16
/*
** DOS VERSION!!
*/
int fhandle;            /* File handle used internally */

fhandle=open(filename,O_CREAT | O_TRUNC, S_IREAD | S_IWRITE);

if(fhandle==-1)
	*errorcode=ERROR_FILECREATE;
else
	*errorcode=0;

/*
** Since all we're doing here is creating the file,
** go ahead and close it.
*/
close(fhandle);

return;
#endif

#ifdef LINUX
FILE *fhandle;            /* File handle used internally */

fhandle=fopen(filename,"w");

if(fhandle==NULL)
	*errorcode=ERROR_FILECREATE;
else
	*errorcode=0;

/*
** Since all we're doing here is creating the file,
** go ahead and close it.
*/
fclose(fhandle);

return;
#endif
}

/****************************
** bmOpenFile
** Opens the file given by fname, returning its handle.
** If an error occurs, returns its code in errorcode.
** The file is opened in read-write exclusive mode.
*/
#ifdef DOS16
/*
** DOS VERSION!!
*/

int bmOpenFile(char *fname,       /* File name */
	int *errorcode)         /* Error code returned */
{

int fhandle;            /* Returned file handle */

fhandle=open(fname,O_BINARY | O_RDWR, S_IREAD | S_IWRITE);

if(fhandle==-1)
	*errorcode=ERROR_FILEOPEN;
else
	*errorcode=0;

return(fhandle);
}
#endif


#ifdef LINUX

FILE *bmOpenFile(char *fname,       /* File name */
	    int *errorcode)         /* Error code returned */
{

FILE *fhandle;            /* Returned file handle */

fhandle=fopen(fname,"w+");

if(fhandle==NULL)
	*errorcode=ERROR_FILEOPEN;
else
	*errorcode=0;

return(fhandle);
}
#endif


/****************************
** CloseFile
** Closes the file identified by fhandle.
** A more inocuous routine there never was.
*/
#ifdef DOS16
/*
** DOS VERSION!!!
*/
void CloseFile(int fhandle,             /* File handle */
		int *errorcode)         /* Returned error code */
{

close(fhandle);
*errorcode=0;
return;
}
#endif
#ifdef LINUX
void CloseFile(FILE *fhandle,             /* File handle */
		int *errorcode)         /* Returned error code */
{
fclose(fhandle);
*errorcode=0;
return;
}
#endif

/****************************
** readfile
** Read bytes from an opened file.  This routine
** is a combination seek-and-read.
** Note that this routine expects the offset to be from
** the beginning of the file.
*/
#ifdef DOS16
/*
** DOS VERSION!!
*/

void readfile(int fhandle,              /* File handle */
	unsigned long offset,           /* Offset into file */
	unsigned long nbytes,           /* # of bytes to read */
	void *buffer,                   /* Buffer to read into */
	int *errorcode)                 /* Returned error code */
{

long newoffset;                         /* New offset by lseek */
int readcode;                           /* Return code from read */

/*
** Presume success.
*/
*errorcode=0;

/*
** Seek to the proper offset.
*/
newoffset=lseek(fhandle,(long)offset,SEEK_SET);
if(newoffset==-1L)
{       *errorcode=ERROR_FILESEEK;
	return;
}

/*
** Do the read.
*/
readcode=read(fhandle,buffer,(unsigned)(nbytes & 0xFFFF));
if(readcode==-1)
	*errorcode=ERROR_FILEREAD;

return;
}
#endif
#ifdef LINUX
void readfile(FILE *fhandle,            /* File handle */
	unsigned long offset,           /* Offset into file */
	unsigned long nbytes,           /* # of bytes to read */
	void *buffer,                   /* Buffer to read into */
	int *errorcode)                 /* Returned error code */
{

long newoffset;                         /* New offset by fseek */
size_t nelems;                          /* Expected return code from read */
size_t readcode;                        /* Actual return code from read */

/*
** Presume success.
*/
*errorcode=0;

/*
** Seek to the proper offset.
*/
newoffset=fseek(fhandle,(long)offset,SEEK_SET);
if(newoffset==-1L)
{       *errorcode=ERROR_FILESEEK;
	return;
}

/*
** Do the read.
*/
nelems=(size_t)(nbytes & 0xFFFF);
readcode=fread(buffer,(size_t)1,nelems,fhandle);
if(readcode!=nelems)
	*errorcode=ERROR_FILEREAD;

return;
}
#endif

/****************************
** writefile
** writes bytes to an opened file.  This routine is
** a combination seek-and-write.
** Note that this routine expects the offset to be from
** the beinning of the file.
*/
#ifdef DOS16
/*
** DOS VERSION!!
*/

void writefile(int fhandle,             /* File handle */
	unsigned long offset,           /* Offset into file */
	unsigned long nbytes,           /* # of bytes to read */
	void *buffer,                   /* Buffer to read into */
	int *errorcode)                 /* Returned error code */
{

long newoffset;                         /* New offset by lseek */
int writecode;                          /* Return code from write */

/*
** Presume success.
*/
*errorcode=0;

/*
** Seek to the proper offset.
*/
newoffset=lseek(fhandle,(long)offset,SEEK_SET);
if(newoffset==-1L)
{       *errorcode=ERROR_FILESEEK;
	return;
}

/*
** Do the write.
*/
writecode=write(fhandle,buffer,(unsigned)(nbytes & 0xFFFF));
if(writecode==-1)
	*errorcode=ERROR_FILEWRITE;

return;
}
#endif

#ifdef LINUX

void writefile(FILE *fhandle,           /* File handle */
	unsigned long offset,           /* Offset into file */
	unsigned long nbytes,           /* # of bytes to read */
	void *buffer,                   /* Buffer to read into */
	int *errorcode)                 /* Returned error code */
{

long newoffset;                         /* New offset by lseek */
size_t nelems;                          /* Expected return code from write */
size_t writecode;                       /* Actual return code from write */

/*
** Presume success.
*/
*errorcode=0;

/*
** Seek to the proper offset.
*/
newoffset=fseek(fhandle,(long)offset,SEEK_SET);
if(newoffset==-1L)
{       *errorcode=ERROR_FILESEEK;
	return;
}

/*
** Do the write.
*/
nelems=(size_t)(nbytes & 0xFFFF);
writecode=fwrite(buffer,(size_t)1,nelems,fhandle);
if(writecode==nelems)
	*errorcode=ERROR_FILEWRITE;

return;
}
#endif


/********************************
**   ERROR HANDLING ROUTINES   **
********************************/

/****************************
** ReportError
** Report error message condition.
*/
void ReportError(char *errorcontext,    /* Error context string */
		int errorcode)          /* Error code number */
{

/*
** Display error context
*/
printf("ERROR CONDITION\nContext: %s\n",errorcontext);

/*
** Display code
*/
printf("Code: %d",errorcode);

return;
}

/****************************
** ErrorExit
** Peforms an exit from an error condition.
*/
void ErrorExit()
{

/*
** For profiling on the Mac with MetroWerks -- 11/17/94 RG
** Have to do this to turn off profiler.
*/
#ifdef MACCWPROF
#if __profile__
ProfilerTerm();
#endif
#endif

/*
** FOR NOW...SIMPLE EXIT
*/
exit(1);
}

/*****************************
**    STOPWATCH ROUTINES    **
*****************************/

/****************************
** StartStopwatch
** Starts a software stopwatch.  Returns the first value of
** the stopwatch in ticks.
*/
unsigned long StartStopwatch()
{
#ifdef MACTIMEMGR
/*
** For Mac code warrior, use timer. In this case, what we return is really
** a dummy value.
*/
InsTime((QElemPtr)&myTMTask);
PrimeTime((QElemPtr)&myTMTask,-MacHSTdelay);
return((unsigned long)1);
#else
#ifdef WIN31TIMER
/*
** Win 3.x timer returns a DWORD, which we coax into a long.
*/
_Call16(lpfn,"p",&win31tinfo);
return((unsigned long)win31tinfo.dwmsSinceStart);
#else
return((unsigned long)clock());
#endif
#endif
}

/****************************
** StopStopwatch
** Stops the software stopwatch.  Expects as an input argument
** the stopwatch start time.
*/
unsigned long StopStopwatch(unsigned long startticks)
{
	
#ifdef MACTIMEMGR
/*
** For Mac code warrior...ignore startticks.  Return val. in microseconds
*/
RmvTime((QElemPtr)&myTMTask);
return((unsigned long)(MacHSTdelay+myTMTask.tmCount-MacHSTohead));
#else
#ifdef WIN31TIMER
_Call16(lpfn,"p",&win31tinfo);
return((unsigned long)win31tinfo.dwmsSinceStart-startticks);
#else
return((unsigned long)clock()-startticks);
#endif
#endif
}

/****************************
** TicksToSecs
** Converts ticks to seconds.  Converts ticks to integer
** seconds, discarding any fractional amount.
*/
unsigned long TicksToSecs(unsigned long tickamount)
{
#ifdef CLOCKWCT
return((unsigned long)(tickamount/CLK_TCK));
#endif

#ifdef MACTIMEMGR
/* +++ MAC time manager version (using timer in microseconds) +++ */
return((unsigned long)(tickamount/1000000));
#endif

#ifdef CLOCKWCPS
/* Everybody else */
return((unsigned long)(tickamount/CLOCKS_PER_SEC));
#endif

#ifdef WIN31TIMER
/* Each tick is 840 nanoseconds */
return((unsigned long)(tickamount/1000L));
#endif

}

/****************************
** TicksToFracSecs
** Converts ticks to fractional seconds.  In other words,
** this returns the exact conversion from ticks to
** seconds.
*/
double TicksToFracSecs(unsigned long tickamount)
{
#ifdef CLOCKWCT
return((double)tickamount/(double)CLK_TCK);
#endif

#ifdef MACTIMEMGR
/* +++ MAC time manager version +++ */
return((double)tickamount/(double)1000000);
#endif

#ifdef CLOCKWCPS
/* Everybody else */
return((double)tickamount/(double)CLOCKS_PER_SEC);
#endif

#ifdef WIN31TIMER
/* Using 840 nanosecond ticks */
return((double)tickamount/(double)1000);
#endif
}

