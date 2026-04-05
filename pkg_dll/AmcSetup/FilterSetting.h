#if !defined(AFX_FILTERSETTING_H__4C50488D_1841_487D_AA3E_C15D46306585__INCLUDED_)
#define AFX_FILTERSETTING_H__4C50488D_1841_487D_AA3E_C15D46306585__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// FilterSetting.h : header file

//

/////////////////////////////////////////////////////////////////////////////
// CFilterSetting dialog

class CFilterSetting : public CPropertyPage
{
	DECLARE_DYNCREATE(CFilterSetting)

public:
	void SetSheetPtr(void *pSheet) { m_lpSheetPtr = pSheet; }
	LPVOID m_lpSheetPtr;

// Construction
public:
	void GetAxisConfigurations(int ax, int mode);
	void SetAxisConfigurations(int ax, int mode);
	int DoButtonCheck(UINT uiID);
	void SystemMonitoring();
	HANDLE m_hsystemmoniQuit;
	CFilterSetting();
	~CFilterSetting();

// Dialog Data
	//{{AFX_DATA(CFilterSetting)
	enum { IDD = IDD_DIALOG_FILTERING };
	UINT	m_nPosLPF;
	UINT	m_nPosNotch;
	UINT	m_nVelLPF;
	UINT	m_nVelNotch;
	UINT	m_uiAxis;
	
	int		m_nmonipercent3p3;
	int		m_nmonipercent5p;
	int		m_nmonipercent12p;
	int		m_nmonipercent12m;
	int		m_nmonipercentaxis0;
	int		m_nmonipercentaxis1;
	int		m_nmonipercentaxis2;
	int		m_nmonipercentaxis3;

	//}}AFX_DATA


// Overrides
	// ClassWizard generate virtual function overrides
	//{{AFX_VIRTUAL(CFilterSetting)
	public:
	virtual BOOL OnKillActive();
	virtual BOOL OnSetActive();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	// Generated message map functions
	//{{AFX_MSG(CFilterSetting)
	afx_msg void OnButtonPosGet();
	afx_msg void OnButtonPosSet();
	afx_msg void OnButtonVelGet();
	afx_msg void OnButtonVelSet();
	afx_msg void OnButtonPrevAxis();
	afx_msg void OnButtonNextAxis();

	afx_msg void OnButtonMoniEnableSet();
	afx_msg void OnButtonMoniDisableSet();
	afx_msg void OnButtonMoniEnableGet();
	afx_msg void OnButtonMoniCutoffSet();
	afx_msg void OnButtonMoniCutoffGet();
	afx_msg void OnButtonMoniValueGet();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_FILTERSETTING_H__4C50488D_1841_487D_AA3E_C15D46306585__INCLUDED_)
