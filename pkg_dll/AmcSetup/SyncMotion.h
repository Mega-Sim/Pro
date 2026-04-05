#if !defined(AFX_SYNCMOTION_H__E8AEFDDA_191F_4F01_B682_226BD36A6B89__INCLUDED_)
#define AFX_SYNCMOTION_H__E8AEFDDA_191F_4F01_B682_226BD36A6B89__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// SyncMotion.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CSyncMotion dialog

class CSyncMotion : public CPropertyPage
{
	DECLARE_DYNCREATE(CSyncMotion)

// Construction
public:
	void UpdateControlStatus();
	CSyncMotion();
	~CSyncMotion();

public:
	BOOL IsValidAxis();
	void SetSheetPtr(void *pSheet) { m_lpSheetPtr = pSheet; }
	LPVOID m_lpSheetPtr;
	int GetInt(char *ptr);
	float GetFloat(char *ptr);
	short GetShort(char *ptr);
	char *GetCommandString(char cmd);
	void PutDbgStatus(unsigned char ucstatus[256]);
	void PutMotionMakeDbgStatus(unsigned char ucstatus[256]);
	void PutEventDbgStatus(unsigned char ucstatus[256]);
	void PutMotionCalDbgStatus(unsigned char ucstatus[256]);

// Dialog Data
	//{{AFX_DATA(CSyncMotion)
	enum { IDD = IDD_DIALOG_SYNC };
	UINT	m_nMAxis;
	UINT	m_nSAxis;
	float	m_fGain;
	BOOL	m_bWdtControl;
	BOOL	m_bWdtExtra;
	BOOL	m_bWdtMain;
	BOOL	m_bWdtSubcontrol;
	CString	m_sWdtStatus;
	CString	m_sDbgStatus;
	CString	m_sDpramTest;
	UINT	sync_pm_axis;
	UINT	sync_pm_use;
	UINT	sync_pm_value;
	
	UINT	sync_avm_axis;
	UINT	sync_avm_value;
	UINT	sync_avm_event;
	UINT	sync_avm_time;


	//}}AFX_DATA


// Overrides
	// ClassWizard generate virtual function overrides
	//{{AFX_VIRTUAL(CSyncMotion)
	public:
	virtual BOOL OnSetActive();
	virtual BOOL OnKillActive();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	// Generated message map functions
	//{{AFX_MSG(CSyncMotion)
	afx_msg void OnButtonSetMap();
	afx_msg void OnButtonControlOn();
	afx_msg void OnButtonControlOff();
	afx_msg void OnButtonSetGain();
	afx_msg void OnButtonGetGain();
	afx_msg void OnTimer(UINT nIDEvent);
	afx_msg void OnButtonEnableWdt();
	afx_msg void OnButtonSetWdtStatus();
	afx_msg void OnButtonGetWdtStatus();
	afx_msg void OnCheckMainStatus();
	afx_msg void OnCheckExtraStatus();
	afx_msg void OnCheckSubcontrolStatus();
	afx_msg void OnCheckControlStatus();
	afx_msg void OnButtonClrWdtReason();
	afx_msg void OnButtonGetDbgStatus();
	afx_msg void OnButtonGetDbgStatus2();
	afx_msg void OnButtonDpramTest();

	afx_msg void OnButtonSetVelCurve();
	afx_msg void OnButtonGetVelCurve();
	afx_msg void OnButtonSetActvelMargin();
	afx_msg void OnButtonGetActvelMargin();

	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_SYNCMOTION_H__E8AEFDDA_191F_4F01_B682_226BD36A6B89__INCLUDED_)
