// MyButton.cpp : implementation file
//

#include "stdafx.h"
#include "AmcSetup.h"
#include "MyButton.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CMyButton

CMyButton::CMyButton()
{
	m_uiAcc = 0;
	m_uiVel = 0;
	m_uiDcc = 0;
	m_hParentWnd = NULL;
}

CMyButton::~CMyButton()
{
}


BEGIN_MESSAGE_MAP(CMyButton, CButton)
	//{{AFX_MSG_MAP(CMyButton)
	ON_WM_LBUTTONDOWN()
	ON_WM_LBUTTONUP()
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CMyButton message handlers

void CMyButton::OnLButtonDown(UINT nFlags, CPoint point) 
{
	// TODO: Add your message handler code here and/or call default
	if (m_hParentWnd)
	{
//		::ReleaseCapture();
		::SendMessage(m_hParentWnd, m_uiMsgID, 0, 0);
	}

	CButton::OnLButtonDown(nFlags, point);
}

void CMyButton::OnLButtonUp(UINT nFlags, CPoint point) 
{
	// TODO: Add your message handler code here and/or call default
	if (m_hParentWnd)
	{
//		::SetCapture(m_hParentWnd);
		::SendMessage(m_hParentWnd, m_uiMsgID, 1, 0);
	}
	
	CButton::OnLButtonUp(nFlags, point);
}

void CMyButton::SetParam(UINT uiAcc, UINT uiVel, UINT uiDcc)
{
	m_uiAcc = uiAcc;
	m_uiVel = uiVel;
	m_uiDcc = uiDcc;
}

void CMyButton::SetWnd(HWND hWnd, UINT uiMsgID)
{
	m_hParentWnd = hWnd;
	m_uiMsgID = uiMsgID;
}
