#if !defined(AFX_SOFTLIMITS_H__3AD7AA1A_B567_419C_85ED_D86133017D89__INCLUDED_)
#define AFX_SOFTLIMITS_H__3AD7AA1A_B567_419C_85ED_D86133017D89__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// SoftLimits.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CSoftLimits dialog

class CSoftLimits : public CDialog
{
// Construction
public:
	void SetAxis(UINT uiAxis);
	void SetAxisConfigurations(int ax);
	void DisableSomeButtons();
	void SetELAction(int act);
	void SetLPAction(int act);
	void SetHPAction(int act);
	int GetELAction();
	int GetLPAction();
	int GetHPAction();
	void SetELObject(int nOnOfs);
	void SetLPObject(int nOnOfs);
	void SetHPObject(int nOnOfs);
	CSoftLimits(CWnd* pParent = NULL);   // standard constructor
	void SetSheetPtr(LPVOID lpv) {m_lpSheetPtr = lpv; }
	void * m_lpSheetPtr;

	char *GetParamPath();
	char m_strPath[300];


// Dialog Data
	//{{AFX_DATA(CSoftLimits)
	enum { IDD = IDD_DIALOG_SOFT_LIMITS };
	UINT	m_uiAxis;
	UINT	m_uiErrorLimit;
	UINT	m_uiEStopRate;
	float	m_fGearRatio;
	int		m_iHighestPos;
	UINT	m_uiInPosition;
	int		m_iLowestPos;
	UINT	m_uiMaxAccel;
	UINT	m_uiMaxVelocity;
	UINT	m_uiStopRate;
	float	m_fVTrackingFactor;
	int		m_nHPAbort;
	int		m_nHPEStop;
	int		m_nHPNoevent;
	int		m_nHPStop;
	int		m_nLPAbort;
	int		m_nLPEStop;
	int		m_nLPNoevent;
	int		m_nLPStop;
	int		m_iPulseRatio;
	int		m_nELAbort;
	int		m_nELEStop;
	int		m_nELNoevent;
	int		m_nELStop;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CSoftLimits)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(CSoftLimits)
	afx_msg void OnButtonSlDspRead();
	afx_msg void OnButtonSlDspWrite();
	afx_msg void OnButtonSlIniFlush();
	afx_msg void OnButtonSlAxisPrev();
	afx_msg void OnButtonSlAxisNext();
	afx_msg void OnRadioHpNoevent();
	afx_msg void OnRadioHpStop();
	afx_msg void OnRadioHpEstop();
	afx_msg void OnRadioHpAbort();
	afx_msg void OnRadioLpNoevent();
	afx_msg void OnRadioLpStop();
	afx_msg void OnRadioLpEstop();
	afx_msg void OnRadioLpAbort();
	afx_msg void OnRadioElNoevent();
	afx_msg void OnRadioElStop();
	afx_msg void OnRadioElEstop();
	afx_msg void OnRadioElAbort();
	virtual BOOL OnInitDialog();
	virtual void OnCancel();
	afx_msg void OnButtonClose();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_SOFTLIMITS_H__3AD7AA1A_B567_419C_85ED_D86133017D89__INCLUDED_)
