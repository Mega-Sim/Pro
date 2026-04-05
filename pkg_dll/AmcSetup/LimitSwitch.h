#if !defined(AFX_LIMITSWITCH_H__CA581E3E_658A_431E_9C2A_23DC9464F711__INCLUDED_)
#define AFX_LIMITSWITCH_H__CA581E3E_658A_431E_9C2A_23DC9464F711__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// LimitSwitch.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CLimitSwitch dialog

class CLimitSwitch : public CDialog
{
// Construction
public:
	void SetAxis(UINT uiAxis);
	void SetAxisConfigurations(int ax);
	INT GetPLEvent();
	INT GetPLLevel();
	INT GetNLEvent();
	INT GetNLLevel();
	INT GetHSEvent();
	INT GetHSLevel();
	INT GetAFEvent();
	INT GetAFLevel();
	INT GetAELevel();
	INT GetARLevel();
	INT GetIPLevel();

	void SetIPLevel(int ofs);
	void SetAELevel(int ofs);
	void SetARLevel(int ofs);
	void SetAFLevel(int ofs);
	void SetAFEvent(int ofs);
	void SetHSLevel(int ofs);
	void SetHSEvent(int ofs);
	void SetNLLevel(int ofs);
	void SetNLEvent(int ofs);

	CLimitSwitch(CWnd* pParent = NULL);   // standard constructor
	void SetSheetPtr(LPVOID lpv) {m_lpSheetPtr = lpv; }
	void * m_lpSheetPtr;
	void SetPLEvent(int ofs);
	void SetPLLevel(int ofs);

	char *GetParamPath();

// Dialog Data
	//{{AFX_DATA(CLimitSwitch)
	enum { IDD = IDD_DIALOG_LIMIT_SWITCH };
	UINT	m_uiAxis;
	BOOL	m_bAEHighActive;
	BOOL	m_bAELowActive;
	BOOL	m_bAFHighActive;
	BOOL	m_bAFLowActive;
	BOOL	m_bARHighActive;
	BOOL	m_bARLowActive;
	BOOL	m_bHSHighActive;
	BOOL	m_bHSLowActive;
	BOOL	m_bIPHighActive;
	BOOL	m_bIPLowActive;
	BOOL	m_bNLHighActive;
	BOOL	m_bNLLowActive;
	BOOL	m_bPLHighActive;
	BOOL	m_bPLLowActive;
	int		m_iAFAbort;
	int		m_iAFEStop;
	int		m_iAFNoevent;
	int		m_iAFStop;
	int		m_iHSAbort;
	int		m_iHSEStop;
	int		m_iHSNoevent;
	int		m_iHSStop;
	int		m_iNLAbort;
	int		m_iNLEStop;
	int		m_iNLNoevent;
	int		m_iNLStop;
	int		m_iPLAbort;
	int		m_iPLEStop;
	int		m_iPLNoevent;
	int		m_iPLStop;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CLimitSwitch)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(CLimitSwitch)
	afx_msg void OnRadioPlNoevent();
	afx_msg void OnRadioPlStop();
	afx_msg void OnRadioPlEstop();
	afx_msg void OnRadioPlAbort();
	afx_msg void OnCheckPlHighactive();
	afx_msg void OnCheckPlLowactive();
	virtual BOOL OnInitDialog();
	afx_msg void OnRadioNlNoevent();
	afx_msg void OnRadioNlStop();
	afx_msg void OnRadioNlEstop();
	afx_msg void OnRadioNlAbort();
	afx_msg void OnCheckNlHighactive();
	afx_msg void OnCheckNlLowactive();
	afx_msg void OnRadioHsNoevent();
	afx_msg void OnRadioHsStop();
	afx_msg void OnRadioHsEstop();
	afx_msg void OnRadioHsAbort();
	afx_msg void OnCheckHsHighactive();
	afx_msg void OnCheckHsLowactive();
	afx_msg void OnRadioAfNoevent();
	afx_msg void OnRadioAfStop();
	afx_msg void OnRadioAfEstop();
	afx_msg void OnRadioAfAbort();
	afx_msg void OnCheckAfHighactive();
	afx_msg void OnCheckAfLowactive();
	afx_msg void OnCheckArHighactive();
	afx_msg void OnCheckArLowactive();
	afx_msg void OnCheckAeHighactive();
	afx_msg void OnCheckAeLowactive();
	afx_msg void OnCheckIpHighactive();
	afx_msg void OnCheckIpLowactive();
	afx_msg void OnButtonLsAxisPrev();
	afx_msg void OnButtonLsAxisNext();
	virtual void OnCancel();
	afx_msg void OnButtonClose();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_LIMITSWITCH_H__CA581E3E_658A_431E_9C2A_23DC9464F711__INCLUDED_)
