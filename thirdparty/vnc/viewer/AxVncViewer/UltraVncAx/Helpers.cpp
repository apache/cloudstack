//
// Helpers.cpp
//

#include "Helpers.h"

//
// BstrToCharstring function.
//

charstring BstrToCharstring( BSTR bstrString )
{
	size_t		sBstrSize = ::SysStringLen( bstrString );
	size_t		sOutputDim = sBstrSize + 1;
	char*		pszOutput = (char*) ::malloc( sOutputDim );
	::WideCharToMultiByte( CP_ACP, 0, bstrString, sBstrSize, pszOutput, sOutputDim, NULL, NULL );
	pszOutput[ sBstrSize ] = '\0';
	charstring	csOutput = pszOutput;
	free( pszOutput );
	return csOutput;
}

//
// CharStringToWideString function.
//

widestring CharStringToWideString( charstring& csString )
{
	size_t		nStringDim = ( csString.size() + 1 ) * sizeof( WCHAR );
	WCHAR*		pszString = (WCHAR*) ::malloc( nStringDim );

	::MultiByteToWideChar( CP_ACP, MB_PRECOMPOSED,
		csString.c_str(), -1,
		pszString, nStringDim / sizeof( WCHAR ) );

	widestring	wsStringToRet = pszString;
	::free( pszString );

	return wsStringToRet;
}

//
// TrimAtBothSides function.
//

void TrimAtBothSides( charstring& csString )
{
	int		i, iStart = 0, iEnd = 0;

	for ( i=0; i<csString.size(); i++ )
		if ( csString[ i ] == ' ' || csString[ i ] == '\r' || csString[ i ] == '\n' || csString[ i ] == '\t' )
			iStart ++;
		else
			break;

	for ( i=csString.size()-1; i>=0; i-- )
		if ( csString[ i ] == ' ' || csString[ i ] == '\r' || csString[ i ] == '\n' || csString[ i ] == '\t' )
			iEnd ++;
		else
			break;

	if ( iStart || iEnd )
		csString = csString.substr( iStart, csString.size() - iEnd - iStart );

	return;
}
