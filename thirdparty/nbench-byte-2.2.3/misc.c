
/*
** misc.c
** BYTEmark (tm)
** BYTE's Native Mode Benchmarks
** Rick Grehan, BYTE Magazine
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
#include "misc.h"

/***********************************************************
**     MISCELLANEOUS BUT OTHERWISE NECESSARY ROUTINES     **
***********************************************************/

/****************************
** RANDOM NUMBER GENERATOR **
*****************************
** This is a second-order linear congruential random number
** generator.  Its advantage is (of course) that it can be
** seeded and will thus produce repeatable sequences of
** random numbers.
*/

/****************************
*         randwc()          *
*****************************
** Returns signed long random modulo num.
*/
/*
long randwc(long num)
{
	return(randnum(0L)%num);
}
*/
/*
** Returns signed 32-bit random modulo num.
*/
int32 randwc(int32 num)
{
	return(randnum((int32)0)%num);
}

/***************************
**      abs_randwc()      **
****************************
** Same as randwc(), only this routine returns only
** positive numbers.
*/
/*
unsigned long abs_randwc(unsigned long num)
{
long temp;

temp=randwc(num);
if(temp<0) temp=0L-temp;

return((unsigned long)temp);
}
*/
u32 abs_randwc(u32 num)
{
int32 temp;		/* Temporary storage */ 

temp=randwc(num);
if(temp<0) temp=(int32)0-temp;

return((u32)temp);
}

/****************************
*        randnum()          *
*****************************
** Second order linear congruential generator.
** Constants suggested by J. G. Skellam.
** If val==0, returns next member of sequence.
**    val!=0, restart generator.
*/
/*
long randnum(long lngval)
{
	register long interm;
	static long randw[2] = { 13L , 117L };

	if (lngval!=0L)
	{	randw[0]=13L; randw[1]=117L; }

	interm=(randw[0]*254754L+randw[1]*529562L)%999563L;
	randw[1]=randw[0];
	randw[0]=interm;
	return(interm);
}
*/
int32 randnum(int32 lngval)
{
	register int32 interm;
	static int32 randw[2] = { (int32)13 , (int32)117 };

	if (lngval!=(int32)0)
	{	randw[0]=(int32)13; randw[1]=(int32)117; }

	interm=(randw[0]*(int32)254754+randw[1]*(int32)529562)%(int32)999563;
	randw[1]=randw[0];
	randw[0]=interm;
	return(interm);
}

