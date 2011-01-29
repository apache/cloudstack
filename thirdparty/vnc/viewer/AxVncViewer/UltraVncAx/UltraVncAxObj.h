// UltraVncAxObj.h : Declaration of the CUltraVncAxObj

#ifndef __ULTRAVNCAXOBJ_H_
#define __ULTRAVNCAXOBJ_H_

#include "resource.h"       // main symbols
#include <atlctl.h>
#include "UltraVncAxCP.h"

/////////////////////////////////////////////////////////////////////////////

#include "Helpers.h"
#include "UltraVncAxGlobalConstructor.h"

/////////////////////////////////////////////////////////////////////////////
// CUltraVncAxObj
class ATL_NO_VTABLE CUltraVncAxObj : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public IDispatchImpl<IUltraVncAxObj, &IID_IUltraVncAxObj, &LIBID_ULTRAVNCAXLib>,
	public CComControl<CUltraVncAxObj>,
	public IPersistStreamInitImpl<CUltraVncAxObj>,
	public IOleControlImpl<CUltraVncAxObj>,
	public IOleObjectImpl<CUltraVncAxObj>,
	public IOleInPlaceActiveObjectImpl<CUltraVncAxObj>,
	public IViewObjectExImpl<CUltraVncAxObj>,
	public IOleInPlaceObjectWindowlessImpl<CUltraVncAxObj>,
	public IConnectionPointContainerImpl<CUltraVncAxObj>,
	public ISupportErrorInfo,
	public IPersistStorageImpl<CUltraVncAxObj>,
	public ISpecifyPropertyPagesImpl<CUltraVncAxObj>,
	public IQuickActivateImpl<CUltraVncAxObj>,
	public IDataObjectImpl<CUltraVncAxObj>,
	public IProvideClassInfo2Impl<&CLSID_UltraVncAxObj, &DIID__IUltraVncAxObjEvents, &LIBID_ULTRAVNCAXLib>,
	public IPropertyNotifySinkCP<CUltraVncAxObj>,
	public CComCoClass<CUltraVncAxObj, &CLSID_UltraVncAxObj>,
	public CProxy_IUltraVncAxObjEvents< CUltraVncAxObj >
{
public:
	CUltraVncAxObj();
	virtual ~ CUltraVncAxObj();

DECLARE_REGISTRY_RESOURCEID(IDR_ULTRAVNCAXOBJ)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CUltraVncAxObj)
	COM_INTERFACE_ENTRY(IUltraVncAxObj)
	COM_INTERFACE_ENTRY(IDispatch)
	COM_INTERFACE_ENTRY(IViewObjectEx)
	COM_INTERFACE_ENTRY(IViewObject2)
	COM_INTERFACE_ENTRY(IViewObject)
	COM_INTERFACE_ENTRY(IOleInPlaceObjectWindowless)
	COM_INTERFACE_ENTRY(IOleInPlaceObject)
	COM_INTERFACE_ENTRY2(IOleWindow, IOleInPlaceObjectWindowless)
	COM_INTERFACE_ENTRY(IOleInPlaceActiveObject)
	COM_INTERFACE_ENTRY(IOleControl)
	COM_INTERFACE_ENTRY(IOleObject)
	COM_INTERFACE_ENTRY(IPersistStreamInit)
	COM_INTERFACE_ENTRY2(IPersist, IPersistStreamInit)
	COM_INTERFACE_ENTRY(IConnectionPointContainer)
	COM_INTERFACE_ENTRY(ISupportErrorInfo)
	COM_INTERFACE_ENTRY(ISpecifyPropertyPages)
	COM_INTERFACE_ENTRY(IQuickActivate)
	COM_INTERFACE_ENTRY(IPersistStorage)
	COM_INTERFACE_ENTRY(IDataObject)
	COM_INTERFACE_ENTRY(IProvideClassInfo)
	COM_INTERFACE_ENTRY(IProvideClassInfo2)
	COM_INTERFACE_ENTRY_IMPL(IConnectionPointContainer)
END_COM_MAP()

BEGIN_PROP_MAP(CUltraVncAxObj)
	PROP_DATA_ENTRY("_cx", m_sizeExtent.cx, VT_UI4)
	PROP_DATA_ENTRY("_cy", m_sizeExtent.cy, VT_UI4)
	// Example entries
	// PROP_ENTRY("Property Description", dispid, clsid)
	// PROP_PAGE(CLSID_StockColorPage)
END_PROP_MAP()

BEGIN_CONNECTION_POINT_MAP(CUltraVncAxObj)
	CONNECTION_POINT_ENTRY(IID_IPropertyNotifySink)
	CONNECTION_POINT_ENTRY(DIID__IUltraVncAxObjEvents)
END_CONNECTION_POINT_MAP()

BEGIN_MSG_MAP(CUltraVncAxObj)
	CHAIN_MSG_MAP(CComControl<CUltraVncAxObj>)
	DEFAULT_REFLECTION_HANDLER()
	MESSAGE_HANDLER( WM_DESTROY, OnDestroy )
	MESSAGE_HANDLER( WM_KEYDOWN, OnRelayMessageHandler )
	MESSAGE_HANDLER( WM_KEYUP, OnRelayMessageHandler )
	MESSAGE_HANDLER( WM_SYSKEYDOWN, OnRelayMessageHandler )
	MESSAGE_HANDLER( WM_SYSKEYUP, OnRelayMessageHandler )
	MESSAGE_HANDLER( WM_PARENTNOTIFY, OnParentNotify )
	MESSAGE_HANDLER( WM_KILLFOCUS, OnRelayMessageHandler )
	MESSAGE_HANDLER( WM_SIZE, OnSize )
END_MSG_MAP()
// Handler prototypes:
//  LRESULT MessageHandler(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled);
//  LRESULT CommandHandler(WORD wNotifyCode, WORD wID, HWND hWndCtl, BOOL& bHandled);
//  LRESULT NotifyHandler(int idCtrl, LPNMHDR pnmh, BOOL& bHandled);


// ISupportsErrorInfo
	STDMETHOD(InterfaceSupportsErrorInfo)(REFIID riid)
	{
		static const IID* arr[] = 
		{
			&IID_IUltraVncAxObj,
		};
		for (int i=0; i<sizeof(arr)/sizeof(arr[0]); i++)
		{
			if (::ATL::InlineIsEqualGUID(*arr[i], riid))
				return S_OK;
		}
		return S_FALSE;
	}

// IViewObjectEx
	DECLARE_VIEW_STATUS(VIEWSTATUS_SOLIDBKGND | VIEWSTATUS_OPAQUE)

// IUltraVncAxObj
public:
	STDMETHOD(get_User)(/*[out, retval]*/ BSTR *pVal);
	STDMETHOD(put_User)(/*[in]*/ BSTR newVal);
	STDMETHOD(get_Password)(/*[out, retval]*/ BSTR *pVal);
	STDMETHOD(put_Password)(/*[in]*/ BSTR newVal);
	STDMETHOD(get_Server)(/*[out, retval]*/ BSTR *pVal);
	STDMETHOD(put_Server)(/*[in]*/ BSTR newVal);
	STDMETHOD(get_Proxy)(/*[out, retval]*/ BSTR *pVal);
	STDMETHOD(put_Proxy)(/*[in]*/ BSTR newVal);
	STDMETHOD(get_Connected)(/*[out, retval]*/ BOOL *pVal);
	STDMETHOD(Disconnect)();
	STDMETHOD(Connect)();
	STDMETHOD(ExecuteCommand)( /*[in]*/ BSTR cmdText, /*[out, retval]*/ BSTR *pRetVal );
	STDMETHOD(get_IsInitialized)(/*[out, retval]*/ BOOL *pVal);

	HRESULT OnDraw(ATL_DRAWINFO& di);

protected:

	//
	// handlers.
	//

	LRESULT OnDestroy (UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled);
	LRESULT OnRelayMessageHandler(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled);
	LRESULT OnParentNotify(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled);
	LRESULT OnSize(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled);

	//
	// implementation.
	//

	HWND GetVncWnd ();
	static HWND GetChildWnd( HWND parent );
	static LRESULT CALLBACK fnHookKeyboard( int code, WPARAM wParam, LPARAM lParam );
	static BOOL IsVncWnd( HWND hwnd );

	BOOL CanEnter ();

	//
	// data.
	//
	charstring			m_strUser;
	charstring			m_strPassword;

	charstring			m_csServer;
	charstring			m_csProxy;
	BOOL				m_bConnectedEvFired;
	HHOOK				m_hHook;
};

#endif //__ULTRAVNCAXOBJ_H_
