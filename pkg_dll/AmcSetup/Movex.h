#if !defined(AFX_MOVEX_H__2DD1F1C8_BFDC_43DF_91D8_1CB32968A262__INCLUDED_)
#define AFX_MOVEX_H__2DD1F1C8_BFDC_43DF_91D8_1CB32968A262__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// Movex.h : header file
//

#include "DataView.h"


/////////////////////////////////////////////////////////////////////////////
// CMovex dialog

class CMovex : public CPropertyPage
{
	DECLARE_DYNCREATE(CMovex)

	enum {BUTTON_MOVEP = 0, BUTTON_MOVEN, BUTTON_MOVES, BUTTON_MOVEDS};

public:
	void SetSheetPtr(void *pSheet) { m_lpParentSheet = pSheet; }
	LPVOID m_lpParentSheet;
	void SetAxis(UINT uiAxis) { m_uiAxis = uiAxis; }

// Construction
public:
	BOOL m_bThreadRun;
	int m_sign;
	BOOL m_bRun;
	void SetMoveButtonStatus(int curBtn);
	void MovexAction();
	void ShowStatus();
	HANDLE m_hMovexStopped;
	HANDLE m_hStatusStopped;
	HANDLE m_hQuitAll;
	void SetAxisConfigurations(UINT uiAxis);
	CMovex();
	~CMovex();

// Dialog Data
	//{{AFX_DATA(CMovex)
	enum { IDD = IDD_DIALOG_MOVEX };
	UINT	m_uiAxis;
	UINT	m_uiMaxSpeed;
	int		m_iStopPos;
	BOOL	m_bAccMove;
	BOOL	m_bDivMove;
	float	m_fAcc;
	float	m_fDcc;
	//}}AFX_DATA
	CDataView	m_cDataView;


// Overrides
	// ClassWizard generate virtual function overrides
	//{{AFX_VIRTUAL(CMovex)
	public:
	virtual BOOL OnSetActive();
	virtual BOOL OnKillActive();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	// Generated message map functions
	//{{AFX_MSG(CMovex)
	afx_msg void OnButtonAxisPrev();
	afx_msg void OnButtonAxisNext();
	afx_msg void OnTimer(UINT nIDEvent);
	afx_msg void OnButtonAmpon();
	afx_msg void OnButtonAmpoff();
	afx_msg void OnButtonPause();
	afx_msg void OnButtonPauseclear();
	afx_msg void OnButtonClearstatus();
	afx_msg void OnButtonMovep();
	afx_msg void OnButtonMoven();
	afx_msg void OnButtonMoves();
	afx_msg void OnButtonMoveds();
	afx_msg void OnButtonSetpositionZero();
	afx_msg void OnButtonRun();
	afx_msg void OnButtonStop();
	afx_msg void OnButtonRunOneshot();
	afx_msg void OnMouseMove(UINT nFlags, CPoint point);
	afx_msg void OnSetFocus(CWnd* pOldWnd);
	afx_msg void OnShowWindow(BOOL bShow, UINT nStatus);
	afx_msg void OnKillFocus(CWnd* pNewWnd);
	afx_msg BOOL OnMouseWheel(UINT nFlags, short zDelta, CPoint pt);
	afx_msg void OnPaint();
	afx_msg void OnCheckAccmove();
	afx_msg void OnCheckDivmove();
	afx_msg void OnButtonViewHold();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_MOVEX_H__2DD1F1C8_BFDC_43DF_91D8_1CB32968A262__INCLUDED_)
