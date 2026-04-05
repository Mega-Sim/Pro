// MyPropertySheet.h : header file
//
// This class defines custom modal property sheet 
// CMyPropertySheet.
 
#ifndef __MYPROPERTYSHEET_H__
#define __MYPROPERTYSHEET_H__

#include "DeviceOpenPage.h"
#include "IOTest.h"
#include "ActionPage.h"
#include "Movex.h"
#include "MoveMulti.h"
#include "ErrorCodes.h"
#include "FsShow.h"
#include "SyncMotion.h"
#include "FilterSetting.h"

/////////////////////////////////////////////////////////////////////////////
// CMyPropertySheet

class CMyPropertySheet : public CPropertySheet
{
	DECLARE_DYNAMIC(CMyPropertySheet)

// Construction
public:
	CMyPropertySheet(CWnd* pWndParent = NULL);

// Attributes
public:
	BOOL m_bDevOpen;

	CErrorCodes		m_PageErrCode;
	CDeviceOpenPage m_Page1;
	CIOTest			m_Page2;
	CActionPage		m_Page3;
	CMovex			m_Page4;
	CMoveMulti		m_Page5;
	CFsShow			m_Page6;
	CSyncMotion		m_Page7;
	CFilterSetting	m_Page8;

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CMyPropertySheet)
	public:
	virtual BOOL PreTranslateMessage(MSG* pMsg);
	//}}AFX_VIRTUAL

// Implementation
public:
	void SetTextFont(char *pszFontName);
	CFont m_TextFont;

	int GetAxisNum();
	void SetAxisNum(int ax);
	int m_uiAxis;
	void SetAbsPath(char *pszPath);
	char m_szAbsPath[300];
	virtual ~CMyPropertySheet();

// Generated message map functions
protected:
	//{{AFX_MSG(CMyPropertySheet)
	afx_msg void OnSetFocus(CWnd* pOldWnd);
	afx_msg void OnPaint();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

/////////////////////////////////////////////////////////////////////////////

#endif	// __MYPROPERTYSHEET_H__
