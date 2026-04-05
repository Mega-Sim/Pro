#if !defined(AFX_DEVICEOPENPAGE_H__461F17A2_67D3_4AA8_BB27_12FBBF1772C4__INCLUDED_)
#define AFX_DEVICEOPENPAGE_H__461F17A2_67D3_4AA8_BB27_12FBBF1772C4__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// DeviceOpenPage.h : header file
//


/////////////////////////////////////////////////////////////////////////////
// CDeviceOpenPage dialog

class CDeviceOpenPage : public CPropertyPage
{
	DECLARE_DYNCREATE(CDeviceOpenPage)

public:
	void SetTestingCount(UINT uiID, int cnt);
	void RunTesting() ;

public:
	void SetSheetPtr(void *pSheet) { m_lpParentSheet = pSheet; }
	LPVOID m_lpParentSheet;
	UINT ToHex(CString str);

// Construction
public:
	CDeviceOpenPage();
	~CDeviceOpenPage();

// Dialog Data
	//{{AFX_DATA(CDeviceOpenPage)
	enum { IDD = IDD_PROPPAGE1 };
	CString	m_sPath;
	BYTE	m_byIRQNum;
	CString	m_sAddr;
	CString	m_sDspVer;
	CString	m_sPcVer;
	CString	m_sThisVer;
	int		m_nEncoderOffset;
	UINT	m_nDpramTestCount;
	//}}AFX_DATA

// Overrides
	// ClassWizard generate virtual function overrides
	//{{AFX_VIRTUAL(CDeviceOpenPage)
	public:
	virtual void OnCancel();
	virtual BOOL OnSetActive();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	// Generated message map functions
	//{{AFX_MSG(CDeviceOpenPage)
	afx_msg void OnButtonDevOpen();
	virtual BOOL OnInitDialog();
	afx_msg void OnButtonDspReset();
	afx_msg void OnButtonFlush();
	afx_msg void OnButtonDpramTest();
	afx_msg void OnButtonBtInit();
	afx_msg void OnButtonBtStartstop();
	//}}AFX_MSG
	afx_msg LRESULT OnDpramTestMsg(WPARAM, LPARAM);
	DECLARE_MESSAGE_MAP()

};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_DEVICEOPENPAGE_H__461F17A2_67D3_4AA8_BB27_12FBBF1772C4__INCLUDED_)
