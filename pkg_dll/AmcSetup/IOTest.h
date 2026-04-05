#if !defined(AFX_IOTEST_H__A9643496_4F87_4FF2_99B0_F5D895B7EC0A__INCLUDED_)
#define AFX_IOTEST_H__A9643496_4F87_4FF2_99B0_F5D895B7EC0A__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// IOTest.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CIOTest dialog

class CIOTest : public CPropertyPage
{
	DECLARE_DYNCREATE(CIOTest)

public:
	void SetSheetPtr(void *pSheet) { m_lpParentSheet = pSheet; }
	LPVOID m_lpParentSheet;

// Construction
public:
	void UpdateOutputBitStatus() ;
	void DoOnOff(UINT uiID);
	void OutputMon();
	void SetInitialButtonState();
	void PutIOStatus(UINT uiID[], INT ui[2]);
	HANDLE m_hAutoOutputStopped;
	HANDLE m_hAutoOutputQuit;
	HANDLE m_hInputStopped;
	HANDLE m_hInputMonQuit;
	void InputMonitoring();
	void Set_io_Object(int nOnOfs);
	void Set_io_text();	
	CIOTest();
	~CIOTest();

// Dialog Data
	//{{AFX_DATA(CIOTest)
	enum { IDD = IDD_DIALOG_IOTEST };
	UINT	m_uiWaitTime;
	UINT	m_uiIncount;
	UINT	m_uiOutcount;
	int		s_iosel1;
	int		s_iosel2;
	int		s_iosel3;
	int		s_iosel4;
	//}}AFX_DATA


// Overrides
	// ClassWizard generate virtual function overrides
	//{{AFX_VIRTUAL(CIOTest)
	public:
	virtual BOOL OnSetActive();
	virtual BOOL OnKillActive();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	// Generated message map functions
	//{{AFX_MSG(CIOTest)
	afx_msg void OnButtonOutputAllset();
	afx_msg void OnButtonOutputAllreset();
	afx_msg void OnButtonOutputAutoChkStart();
	afx_msg void OnButtonOutputAutoChkStop();
	afx_msg void OnButtonInputOutputWrite();
	afx_msg void OnButtonInputOutputRead();
	afx_msg void OnCheckObit0();
	afx_msg void OnCheckObit1();
	afx_msg void OnCheckObit2();
	afx_msg void OnCheckObit3();
	afx_msg void OnCheckObit4();
	afx_msg void OnCheckObit5();
	afx_msg void OnCheckObit6();
	afx_msg void OnCheckObit7();
	afx_msg void OnCheckObit8();
	afx_msg void OnCheckObit9();
	afx_msg void OnCheckObit10();
	afx_msg void OnCheckObit11();
	afx_msg void OnCheckObit12();
	afx_msg void OnCheckObit13();
	afx_msg void OnCheckObit14();
	afx_msg void OnCheckObit15();
	afx_msg void OnCheckObit16();
	afx_msg void OnCheckObit17();
	afx_msg void OnCheckObit18();
	afx_msg void OnCheckObit19();
	afx_msg void OnCheckObit20();
	afx_msg void OnCheckObit21();
	afx_msg void OnCheckObit22();
	afx_msg void OnCheckObit23();
	afx_msg void OnCheckObit24();
	afx_msg void OnCheckObit25();
	afx_msg void OnCheckObit26();
	afx_msg void OnCheckObit27();
	afx_msg void OnCheckObit28();
	afx_msg void OnCheckObit29();
	afx_msg void OnCheckObit30();
	afx_msg void OnCheckObit31();
	afx_msg void OnCheckObit32();
	afx_msg void OnCheckObit33();
	afx_msg void OnCheckObit34();
	afx_msg void OnCheckObit35();
	afx_msg void OnCheckObit36();
	afx_msg void OnCheckObit37();
	afx_msg void OnCheckObit38();
	afx_msg void OnCheckObit39();
	afx_msg void OnCheckObit40();
	afx_msg void OnCheckObit41();
	afx_msg void OnCheckObit42();
	afx_msg void OnCheckObit43();
	afx_msg void OnCheckObit44();
	afx_msg void OnCheckObit45();
	afx_msg void OnCheckObit46();
	afx_msg void OnCheckObit47();
	afx_msg void OnCheckObit48();
	afx_msg void OnCheckObit49();
	afx_msg void OnCheckObit50();
	afx_msg void OnCheckObit51();
	afx_msg void OnCheckObit52();
	afx_msg void OnCheckObit53();
	afx_msg void OnCheckObit54();
	afx_msg void OnCheckObit55();
	afx_msg void OnCheckObit56();
	afx_msg void OnCheckObit57();
	afx_msg void OnCheckObit58();
	afx_msg void OnCheckObit59();
	afx_msg void OnCheckObit60();
	afx_msg void OnCheckObit61();
	afx_msg void OnCheckObit62();
	afx_msg void OnCheckObit63();

	afx_msg void OnRadio_sel_io1();
	afx_msg void OnRadio_sel_io2();
	afx_msg void OnRadio_sel_io3();
	afx_msg void OnRadio_sel_io4();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_IOTEST_H__A9643496_4F87_4FF2_99B0_F5D895B7EC0A__INCLUDED_)
