#if !defined(AFX_MOVEMULTI_H__3204DB23_CE23_4440_A3D0_25963E979C16__INCLUDED_)
#define AFX_MOVEMULTI_H__3204DB23_CE23_4440_A3D0_25963E979C16__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// MoveMulti.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CMoveMulti dialog

class CMoveMulti : public CPropertyPage
{
	DECLARE_DYNCREATE(CMoveMulti)

// Construction
public:
	char m_strPath[300];
	char * GetParamPath();
	BOOL CheckAllDone();
	void DoAction();
	void SetOptions(int p1, int p2, int p3, int p4);

	int m_Pos[4];
	int m_AxisOn[4];
	int m_nActAxisCount;
	int m_nRepeatCmd;
	HANDLE m_hQuitRepeat;
	void Repeat();
	void Moni();
	HANDLE m_hQuit;
	HANDLE m_hStopped;
	CMoveMulti();
	~CMoveMulti();

	void SetSheetPtr(LPVOID lpv) {m_lpSheetPtr = lpv; }
	void * m_lpSheetPtr;

	UINT GetVelocity(int axis);
	UINT GetAcc(int axis);

// Dialog Data
	//{{AFX_DATA(CMoveMulti)
	enum { IDD = IDD_DIALOG_MOVE234 };
	BOOL	m_bAction1;
	BOOL	m_bAction2;
	BOOL	m_bAction3;
	BOOL	m_bAction4;
	int		m_nPos1;
	int		m_nPos2;
	int		m_nPos3;
	int		m_nPos4;
	int		m_nMaxVelocity;
	BOOL	m_bSCurve;
	int		m_nPauseAxis;
	UINT	m_nAccMsec;
	BOOL	m_bUseStMove;
	UINT	m_nSeqVel;
	UINT	m_nSeqAcc;
	//}}AFX_DATA


// Overrides
	// ClassWizard generate virtual function overrides
	//{{AFX_VIRTUAL(CMoveMulti)
	public:
	virtual BOOL OnKillActive();
	virtual BOOL OnSetActive();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	// Generated message map functions
	//{{AFX_MSG(CMoveMulti)
	afx_msg void OnButtonPositionClear();
	afx_msg void OnButtonActionAll();
	afx_msg void OnButtonStopAll();
	afx_msg void OnButtonClearStopAll();
	afx_msg void OnButtonFramesClearAll();
	afx_msg void OnButtonAxisOnAll();
	afx_msg void OnButtonAxisOffAll();
	afx_msg void OnButtonRepeat();
	afx_msg void OnButtonRepeatStop();
	afx_msg void OnButtonActionHome();
	afx_msg void OnButtonPause();
	afx_msg void OnButtonClearStop();
	virtual BOOL OnInitDialog();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_MOVEMULTI_H__3204DB23_CE23_4440_A3D0_25963E979C16__INCLUDED_)
