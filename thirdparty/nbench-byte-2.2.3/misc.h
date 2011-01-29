/*
** misc.h
** Header for misc.c
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

/************************
** FUNCTION PROTOTYPES **
************************/

/*
long randwc(long num);
unsigned long abs_randwc(unsigned long num);
long randnum(long lngval);
*/

#include "nmglobal.h"
int32 randwc(int32 num);
u32 abs_randwc(u32 num);
int32 randnum(int32 lngval);


