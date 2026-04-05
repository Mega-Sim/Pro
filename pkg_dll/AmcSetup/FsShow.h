#if !defined(AFX_FSSHOW_H__9CF685BA_6EF6_433D_A5B5_34F44D13DE8F__INCLUDED_)
#define AFX_FSSHOW_H__9CF685BA_6EF6_433D_A5B5_34F44D13DE8F__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// FsShow.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CFsShow dialog

class CFsShow : public CPropertyPage
{
	DECLARE_DYNCREATE(CFsShow)

public:
	void SetSheetPtr(void *pSheet) { m_lpParentSheet = pSheet; }
	LPVOID m_lpParentSheet;

// Construction
public:
	CFsShow();
	~CFsShow();

public:
	int GetCurSel();
	void RefreshSystemData();
	void RefreshFileLists();

	void SetInitialButtonState();
	void DeleteAllItems();

// Dialog Data
	//{{AFX_DATA(CFsShow)
	enum { IDD = IDD_DIALOG_FS };
	CProgressCtrl	m_cProgress;
	CListCtrl	m_cList;
	//}}AFX_DATA


// Overrides
	// ClassWizard generate virtual function overrides
	//{{AFX_VIRTUAL(CFsShow)
	public:
	virtual BOOL OnSetActive();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	// Generated message map functions
	//{{AFX_MSG(CFsShow)
	afx_msg void OnButtonRefresh();
	afx_msg void OnButtonDelete();
	afx_msg void OnButtonDnload();
	afx_msg void OnButtonUpload();
	afx_msg void OnButtonSetboot();
	afx_msg void OnButtonFormat();
	afx_msg void OnButtonSysteminfo();
	virtual BOOL OnInitDialog();
	afx_msg void OnButtonRestart();
	afx_msg void OnButtonDump1();
	afx_msg void OnButtonDump2();
	afx_msg void OnButtonDump3();
	afx_msg void OnButtonFileInstall();
	//}}AFX_MSG
	LRESULT OnProceedings(WPARAM wParam, LPARAM lParam);
	DECLARE_MESSAGE_MAP()

};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_FSSHOW_H__9CF685BA_6EF6_433D_A5B5_34F44D13DE8F__INCLUDED_)
