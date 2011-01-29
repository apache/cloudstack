//
// CCritSec.h
//

#pragma once

//
// struct definition.
//

class CCriticalSection
{
public:

	// Construction.

	CCriticalSection ()
	{
		::InitializeCriticalSection( & m_cs );
	}

	virtual ~ CCriticalSection ()
	{
		::DeleteCriticalSection( & m_cs );
	}

	// Methods.

	VOID Enter ()
	{
		::EnterCriticalSection( & m_cs );
	}
	VOID Leave ()
	{
		::LeaveCriticalSection( & m_cs );
	}

	// Data.

	class scope
	{
	public:
		scope( CCriticalSection& cs )
		{
			m_pcs = & cs.m_cs;
			::EnterCriticalSection( m_pcs );
		}
		virtual ~ scope ()
		{
			::LeaveCriticalSection( m_pcs );
		}
	protected:
		CRITICAL_SECTION*		m_pcs;
	};

	//

	typedef /*WINBASEAPI*/ BOOL ( WINAPI * TRYENTERCRITICALSECTION )( LPCRITICAL_SECTION lpCriticalSection );

	class try_scope
	{
	public:
		try_scope( CCriticalSection& cs )
		{
			m_pcs = & cs.m_cs;
			TRYENTERCRITICALSECTION		pfnTryEnterCriticalSection = (TRYENTERCRITICALSECTION) ::GetProcAddress( ::GetModuleHandle( "kernel32.dll" ), "TryEnterCriticalSection" );
			if ( pfnTryEnterCriticalSection )
				entered = pfnTryEnterCriticalSection( m_pcs );
			else
			{
				entered = TRUE;
				::EnterCriticalSection( m_pcs );
			}
		}
		virtual ~ try_scope ()
		{
			if ( entered )
				::LeaveCriticalSection( m_pcs );
		}
		BOOL			entered;
	protected:
		CRITICAL_SECTION*		m_pcs;
	};

protected:

	// Data.

	CRITICAL_SECTION		m_cs;

	// Friends.

	friend class scope;
	friend class try_scope;
};
