// MyPropertySheet.cpp : implementation file
//

#include "stdafx.h"
#include "resource.h"
#include "MyPropertySheet.h"

#ifdef _DEBUG
#undef THIS_FILE
static char BASED_CODE THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CMyPropertySheet

IMPLEMENT_DYNAMIC(CMyPropertySheet, CPropertySheet)

CMyPropertySheet::CMyPropertySheet(CWnd* pWndParent)
	 : CPropertySheet(IDS_PROPSHT_CAPTION, pWndParent)
{
	// Add all of the property pages here.  Note that
	// the order that they appear in here will be
	// the order they appear in on screen.  By default,
	// the first page of the set is the active one.
	// One way to make a different property page the 
	// active one is to call SetActivePage().

	AddPage(&m_Page1);
	AddPage(&m_PageErrCode);
	AddPage(&m_Page2);
	AddPage(&m_Page3);
	AddPage(&m_Page4);
	AddPage(&m_Page5);
	AddPage(&m_Page6);
	AddPage(&m_Page7);
	AddPage(&m_Page8);

	// 각 페이지에 Sheet의 포인터를 설정해 준다.
	m_Page1.SetSheetPtr(this);
	m_Page2.SetSheetPtr(this);
	m_Page3.SetSheetPtr(this);
	m_Page4.SetSheetPtr(this);
	m_Page5.SetSheetPtr(this);
	m_Page6.SetSheetPtr(this);
	m_Page7.SetSheetPtr(this);
	m_Page8.SetSheetPtr(this);

	m_bDevOpen = FALSE;

	m_uiAxis = 0;

	strcpy(m_szAbsPath, "C:\\");

	SetTextFont("Arial");
}

CMyPropertySheet::~CMyPropertySheet()
{
	m_TextFont.Detach();
}


BEGIN_MESSAGE_MAP(CMyPropertySheet, CPropertySheet)
	//{{AFX_MSG_MAP(CMyPropertySheet)
	ON_WM_SETFOCUS()
	ON_WM_PAINT()
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
// CMyPropertySheet message handlers



void CMyPropertySheet::SetAbsPath(char *pszPath)
{
	if (strlen(pszPath) >= 299) return;
	strcpy(m_szAbsPath, pszPath);
	m_szAbsPath[299] = 0;
}

void CMyPropertySheet::OnSetFocus(CWnd* pOldWnd) 
{
	CPropertySheet::OnSetFocus(pOldWnd);
	
	// TODO: Add your message handler code here
	
}

void CMyPropertySheet::SetAxisNum(int ax)
{
	m_uiAxis = ax;
}

int CMyPropertySheet::GetAxisNum()
{
	return m_uiAxis;
}

extern CString FormattingVersion(int ver);
void CMyPropertySheet::OnPaint() 
{
	CPaintDC dc(this); // device context for painting
	
	// TODO: Add your message handler code here
	CString sVersion;
	sVersion = "  Cantops AMC Board Setup Program. Ver.";
//111029 syk_start	
	sVersion += FormattingVersion(VERSION_SETUP);
//111029 syk_end
	
	CRect rectTabCtrl;
	GetTabControl()->GetWindowRect(rectTabCtrl);
	ScreenToClient(rectTabCtrl);
	CRect rectOk;
	GetDlgItem(IDOK)->GetWindowRect(rectOk);
	ScreenToClient(rectOk);
	
	dc.SetBkMode(TRANSPARENT);
	
	CRect rectText;
	rectText.left = rectTabCtrl.left;
	rectText.top = rectOk.top;
	rectText.bottom = rectOk.bottom;
	rectText.right = rectOk.left;

	CFont *pOldFont = dc.SelectObject(&m_TextFont);
	COLORREF OldColor = dc.SetTextColor(::GetSysColor(COLOR_3DHILIGHT));
	dc.DrawText(sVersion, rectText + CPoint(1, 1), DT_SINGLELINE | DT_LEFT | DT_VCENTER);
	
	// 글씨가 좀더 선명하게 보이게하기 위해서 어둡게 변경했음. 2002. 3. 11. ckyu.
	//dc.SetTextColor(::GetSysColor(COLOR_3DSHADOW));
	dc.SetTextColor(RGB(50, 50, 50));
	dc.DrawText(sVersion, rectText, DT_SINGLELINE | DT_LEFT | DT_VCENTER);
	dc.SetTextColor(OldColor);
	dc.SelectObject(pOldFont);

	// Do not call CPropertySheet::OnPaint() for painting messages
	SetWindowText("AMC Board Setup Program (Cantops)");
}

void CMyPropertySheet::SetTextFont(char *pszFontName)
{
	if (m_TextFont.m_hObject) m_TextFont.Detach();
//	m_TextFont.CreateFont(nHeight, 0, 0, 0, nWeight, bItalic, bUnderline, 0, 0, 0, 0, 0, 0, pszFontName);
	m_TextFont.CreateFont(16, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, pszFontName);
}

BOOL CMyPropertySheet::PreTranslateMessage(MSG* pMsg) 
{
	// TODO: Add your specialized code here and/or call the base class
	if (pMsg->message == WM_KEYDOWN && pMsg->wParam == VK_ESCAPE) 
		return TRUE;
	return CPropertySheet::PreTranslateMessage(pMsg);
}
