// CRYPTO LIBRARY FOR EXCHANGING KEYS
// USING THE DIFFIE-HELLMAN KEY EXCHANGE PROTOCOL

// The diffie-hellman can be used to securely exchange keys
// between parties, where a third party eavesdropper given
// the values being transmitted cannot determine the key.

// Implemented by Lee Griffiths, Jan 2004.
// This software is freeware, you may use it to your discretion,
// however by doing so you take full responsibility for any damage
// it may cause.

// Hope you find it useful, even if you just use some of the functions
// out of it like the prime number generator and the XtoYmodN function.

// It would be great if you could send me emails to: lee.griffiths@first4internet.co.uk
// with any suggestions, comments, or questions!

// Enjoy.

// Adopted to ms-logon for ultravnc by marscha, 2006.

#ifndef __RFB_DH_H__
#define __RFB_DH_H__

#include <windows.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <time.h>

#define DH_MAX_BITS 31
#define DH_RANGE 100

#define DH_CLEAN_ALL_MEMORY				1
#define DH_CLEAN_ALL_MEMORY_EXCEPT_KEY		2

#define DH_MOD	1
#define DH_GEN	2
#define DH_PRIV	3
#define DH_PUB	4
#define DH_KEY	5

class DH
{
public:
	DH();
	DH(unsigned __int64 generator, unsigned __int64 modulus);
	~DH();

	void createKeys();
	unsigned __int64 createInterKey();
	unsigned __int64 createEncryptionKey(unsigned __int64 interKey);
	
	unsigned __int64 getValue(DWORD flags = DH_KEY);

private:
	unsigned __int64 XpowYmodN(unsigned __int64 x, unsigned __int64 y, unsigned __int64 N);
	unsigned __int64 generatePrime();
	unsigned __int64 tryToGeneratePrime(unsigned __int64 start);
	bool millerRabin (unsigned __int64 n, unsigned int trials);
	void cleanMem(DWORD flags=DH_CLEAN_ALL_MEMORY);


	unsigned __int64 gen;
	unsigned __int64 mod;
	unsigned __int64 priv;
	unsigned __int64 pub;
	unsigned __int64 key;
	unsigned __int64 maxNum;

};

int bits(__int64 number);
bool int64ToBytes(const unsigned __int64 integer, char* const bytes);
unsigned __int64 bytesToInt64(const char* const bytes);
bool vncWc2Mb(char* multibyte, WCHAR* widechar, int length);

#endif // __RFB_DH_H__