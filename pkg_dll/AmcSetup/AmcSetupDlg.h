// AmcSetupDlg.h : header file
//

#if !defined(AFX_AMCSETUPDLG_H__E2A248FC_E830_49A1_8894_3D1F364AC58B__INCLUDED_)
#define AFX_AMCSETUPDLG_H__E2A248FC_E830_49A1_8894_3D1F364AC58B__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

/////////////////////////////////////////////////////////////////////////////
// CAmcSetupDlg dialog

class CAmcSetupDlg : public CDialog
{
// Construction
public:
	void OnProperties();
	CAmcSetupDlg(CWnd* pParent = NULL);	// standard constructor

// Dialog Data
	//{{AFX_DATA(CAmcSetupDlg)
	enum { IDD = IDD_AMCSETUP_DIALOG };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA

	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CAmcSetupDlg)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	HICON m_hIcon;

	// Generated message map functions
	//{{AFX_MSG(CAmcSetupDlg)
	virtual BOOL OnInitDialog();
	afx_msg void OnSysCommand(UINT nID, LPARAM lParam);
	afx_msg void OnPaint();
	afx_msg HCURSOR OnQueryDragIcon();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_AMCSETUPDLG_H__E2A248FC_E830_49A1_8894_3D1F364AC58B__INCLUDED_)
