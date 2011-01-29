/*
** sysspec.h
** Header file for sysspec.c
** BYTEmark (tm)
** BYTE's Native Mode Benchmarks
** Rick Grehan, BYTE Magazine
**
** Creation:
** Revision: 3/95
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
** Standard includes
*/
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <string.h>

#include "nmglobal.h"

#if !defined(MAC) && !defined(OSX)
#include <malloc.h>
#endif


/*
** System-specific includes
*/

#ifdef DOS16MEM
#include "dos.h"
#endif

/* #include "time.h"
#include "io.h"
#include "fcntl.h"
#include "sys\stat.h" */
/* Removed for MSVC++
#include "alloc.h"
*/

/*
** MAC Time Manager routines (from Code Warrior)
*/
#ifdef MACTIMEMGR
#include <memory.h>
#include <lowmem.h>
#include <Types.h>
#include <Timer.h>
extern struct TMTask myTMTask;
extern long MacHSTdelay,MacHSTohead;
#endif

/*
** Windows 3.1 timer defines
*/
#ifdef WIN31TIMER
#include <windows.h>
#include <toolhelp.h>
TIMERINFO win31tinfo;
HANDLE hThlp;
FARPROC lpfn;
#endif

/**************
** EXTERNALS **
**************/
extern ulong mem_array[2][MEM_ARRAY_SIZE];
extern int mem_array_ents;
extern int global_align;

/****************************
**   FUNCTION PROTOTYPES   **
****************************/

farvoid *AllocateMemory(unsigned long nbytes,
                int *errorcode);

void FreeMemory(farvoid *mempointer,
                int *errorcode);

void MoveMemory( farvoid *destination,
                farvoid *source,
                unsigned long nbytes);

#ifdef DOS16MEM
void FarDOSmemmove(farvoid *destination,
                farvoid *source,
                unsigned long nbytes);
#endif

void InitMemArray(void);

int AddMemArray(ulong true_addr, ulong adj_addr);

int RemoveMemArray(ulong adj_addr,ulong *true_addr);

void ReportError(char *context, int errorcode);

void ErrorExit();

void CreateFile(char *filename,
                int *errorcode);

#ifdef DOS16
int bmOpenFile(char *fname,
                int *errorcode);

void CloseFile(int fhandle,
                int *errorcode);

void readfile(int fhandle,
                unsigned long offset,
                unsigned long nbytes,
                void *buffer,
                int *errorcode);

void writefile(int fhandle,
                unsigned long offset,
                unsigned long nbytes,
                void *buffer,
                int *errorcode);
#endif

#ifdef LINUX
FILE *bmOpenFile(char *fname,
                int *errorcode);

void CloseFile(FILE *fhandle,
                int *errorcode);

void readfile(FILE *fhandle,
                unsigned long offset,
                unsigned long nbytes,
                void *buffer,
                int *errorcode);

void writefile(FILE *fhandle,
                unsigned long offset,
                unsigned long nbytes,
                void *buffer,
                int *errorcode);

#endif

unsigned long StartStopwatch();

unsigned long StopStopwatch(unsigned long startticks);

unsigned long TicksToSecs(unsigned long tickamount);

double TicksToFracSecs(unsigned long tickamount);

