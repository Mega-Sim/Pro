#if !defined(AFX_PARAMTUNING_H__65C5E713_385A_42C1_882F_437BEE0B9D6A__INCLUDED_)
#define AFX_PARAMTUNING_H__65C5E713_385A_42C1_882F_437BEE0B9D6A__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// ParamTuning.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CParamTuning dialog

class CParamTuning : public CDialog
{
// Construction
public:
	void SetAxis(UINT uiAxis);
	void ShowAxisSettings(int ax);
	void DisableAllButtons();
	CParamTuning(CWnd* pParent = NULL);   // standard constructor
	void WriteIntIniParam(char *pszFile, char *pszSec, char *pszKey, int val);
	void SetSheetPtr(LPVOID lpv) {m_lpSheetPtr = lpv; }
	void * m_lpSheetPtr;

	char *GetParamPath();
	char m_strPath[300];

// Dialog Data
	//{{AFX_DATA(CParamTuning)
	enum { IDD = IDD_DIALOG_TUNING };
	UINT	m_uiAnalogLimit;
	int		m_nAnalogOffset;
	UINT	m_uiAnalogOutput;
	UINT	m_uiDGain;
	UINT	m_uiFGain;
	UINT	m_uiIGain;
	UINT	m_uiILimit;
	UINT	m_uiPGain;
	UINT	m_uiVDGain;
	UINT	m_uiVFGain;
	UINT	m_uiVIGain;
	UINT	m_uiVILimit;
	UINT	m_uiVPGain;
	UINT	m_uiAxis;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CParamTuning)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(CParamTuning)
	afx_msg void OnButtonAxisNext();
	afx_msg void OnButtonAxisPrev();
	afx_msg void OnButtonRead();
	afx_msg void OnButtonSave();
	afx_msg void OnButtonWrite();
	virtual void OnCancel();
	virtual BOOL OnInitDialog();
	afx_msg void OnButtonClose();
	afx_msg void OnButtonEditoffset();
	afx_msg void OnKillfocusEditAnalogOffset();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_PARAMTUNING_H__65C5E713_385A_42C1_882F_437BEE0B9D6A__INCLUDED_)
