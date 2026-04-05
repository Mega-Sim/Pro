#if !defined(AFX_MYBUTTON_H__FAE6AA55_7CB3_40B4_B1FD_89B9B49979E1__INCLUDED_)
#define AFX_MYBUTTON_H__FAE6AA55_7CB3_40B4_B1FD_89B9B49979E1__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// MyButton.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CMyButton window

class CMyButton : public CButton
{
// Construction
public:
	CMyButton();

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CMyButton)
	//}}AFX_VIRTUAL

// Implementation
public:
	UINT m_uiMsgID;
	HWND m_hParentWnd;
	void SetWnd(HWND hWnd, UINT uiMsgID);
	void SetParam(UINT uiAcc, UINT uiVel, UINT uiDcc);
	UINT m_uiVel;
	UINT m_uiDcc;
	UINT m_uiAcc;
	virtual ~CMyButton();

	// Generated message map functions
protected:
	//{{AFX_MSG(CMyButton)
	afx_msg void OnLButtonDown(UINT nFlags, CPoint point);
	afx_msg void OnLButtonUp(UINT nFlags, CPoint point);
	//}}AFX_MSG

	DECLARE_MESSAGE_MAP()
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_MYBUTTON_H__FAE6AA55_7CB3_40B4_B1FD_89B9B49979E1__INCLUDED_)
