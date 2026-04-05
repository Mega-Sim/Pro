#if !defined(AFX_ACTIONPAGE_H__C8E187C1_C78A_49C3_8F9A_1CB0F3A22711__INCLUDED_)
#define AFX_ACTIONPAGE_H__C8E187C1_C78A_49C3_8F9A_1CB0F3A22711__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// ActionPage.h : header file
//

#include "MyButton.h"

/////////////////////////////////////////////////////////////////////////////
// CActionPage dialog

class CActionPage : public CPropertyPage
{
	DECLARE_DYNCREATE(CActionPage)

public:
	void SetSheetPtr(void *pSheet) { m_lpSheetPtr = pSheet; }
	LPVOID m_lpSheetPtr;

// Construction
public:
	char m_strPath[300];
	char *GetParamPath();
	void putinfo();

	int m_nOneCycleDir;
	void RepeatAction(int ncmd);
	volatile int m_nRptCmd;
	void MotionAction();

	void SetCurveMode(int ofs);
	void SetInitialButtonState();
	void SetAxisConfigurations(int ax) ;

	void ShowStatus();
	HANDLE m_hShowStatusStopped;
	HANDLE m_hShowStatusQuit;
	HANDLE m_hMotionActionStopped;
	HANDLE m_hMotionActionQuit;
	volatile int m_nAxis;
	CActionPage();
	~CActionPage();

// Dialog Data
	//{{AFX_DATA(CActionPage)
	enum { IDD = IDD_DIALOG_ACTION };
	BOOL	m_bAmpEnable;
	BOOL	m_bAmpFault;
	BOOL	m_bHome;
	BOOL	m_bNeg;
	BOOL	m_bPos;
	BOOL	m_bSCurve;
	UINT	m_uiAxis;
	UINT	m_uiAccel;
	UINT	m_uiDecel;
	UINT	m_uiDelay;
	UINT	m_uiVel;
	UINT	m_uiJogPS;
	UINT	m_uiJogRM;
	BOOL	m_bTrapezoidal;
	BOOL	m_bRelative;
	int		m_iPos1;
	int		m_iPos2;
	int		m_iEncoderPosition;
	BOOL	m_bPos2Rnd;
	//}}AFX_DATA

	CMyButton	m_JogPos;
	CMyButton	m_JogNeg;


// Overrides
	// ClassWizard generate virtual function overrides
	//{{AFX_VIRTUAL(CActionPage)
	public:
	virtual BOOL OnKillActive();
	virtual BOOL OnSetActive();
	virtual BOOL DestroyWindow();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	// Generated message map functions
	//{{AFX_MSG(CActionPage)
	afx_msg void OnButtonTunning();
	afx_msg void OnButtonSwLimit();
	afx_msg void OnButtonLimitSwitch();
	afx_msg void OnButtonAxisCfg();
	virtual BOOL OnInitDialog();
	afx_msg void OnButtonAxisPrev();
	afx_msg void OnButtonAxisNext();
	afx_msg void OnButtonGotoPos();
	afx_msg void OnTimer(UINT nIDEvent);
	afx_msg void OnCheckTrapezoid();
	afx_msg void OnCheckScurve();
	afx_msg void OnButtonCyclePos();
	afx_msg void OnButtonRepeat();
	afx_msg void OnButtonRepeatStop();
	afx_msg void OnButtonEstop();
	afx_msg void OnButtonStop();
	afx_msg void OnButtonReload();

	afx_msg void OnButtonPause();
	afx_msg void OnButtonClear();
	afx_msg void OnButtonAmpOn();
	afx_msg void OnButtonAmpOff();
	afx_msg void OnButtonReset();
	afx_msg void OnButtonPauseClear();
	afx_msg void OnButtonClearStop();
	afx_msg void OnButtonClearall();
	afx_msg void OnButtonFrameClear();
	afx_msg void OnButtonGetPosition();
	afx_msg void OnButtonSetPosition();
	afx_msg void OnButtonCyclePos2();
	afx_msg void OnButtonDebug();
	afx_msg void OnButtonTqLimitSet();
	afx_msg void OnButtonTqLimitReset();
	//}}AFX_MSG
	afx_msg LRESULT OnJogPos(WPARAM wParam, LPARAM lParam);
	afx_msg LRESULT OnJogNeg(WPARAM wParam, LPARAM lParam);
	DECLARE_MESSAGE_MAP()

};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_ACTIONPAGE_H__C8E187C1_C78A_49C3_8F9A_1CB0F3A22711__INCLUDED_)
