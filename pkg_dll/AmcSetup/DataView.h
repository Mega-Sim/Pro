#if !defined(AFX_DATAVIEW_H__D2A5A203_7B14_4798_8834_77B4201F0AB5__INCLUDED_)
#define AFX_DATAVIEW_H__D2A5A203_7B14_4798_8834_77B4201F0AB5__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// DataView.h : header file
//


/////////////////////////////////////////////////////////////////////////////
// CDataView window

class CDataView : public CWnd
{
// Construction
public:
	CDataView();

// Attributes
public:
	short * m_pDataY;
	short * m_pDataX;
	int m_pDataLen;
	int m_nSaveOfs;
	int	m_nMaxY;
	int m_nMinY;
	BOOL	m_bDataOverwrite;	// 한화면을 쓰고난 후 부터는 1로 설정된다.

	int m_nXWidth;	// unit:ms 최대 200~5000
	int m_nYHeight;

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CDataView)
	public:
	virtual BOOL DestroyWindow();
	protected:
	virtual BOOL PreCreateWindow(CREATESTRUCT& cs);
	//}}AFX_VIRTUAL

// Implementation
public:
	BOOL m_bHold;
	void SetHold(BOOL bHold);
	int GetMax();
	void InitIfNecessary();
	CFont *m_pTextFont;
	void SetXYMarkWnd(CWnd *pX, CWnd *pY);
	CWnd * m_phXMarkWnd;
	CWnd * m_phYMarkWnd;
	void PutXScaleMark(int min, int max, int step);
	void PutYScaleMark(int min, int max, int step);

	int GetXPos(int ival);
	int GetYPos(int ival);
	CRect m_sWindowRect;
	double m_yScale;
	double m_xScale;
	void SetData(short dat);
	void KillFocus();
	void SetFocus();
	void OnDraw(CDC *pDC);
	int CalcMinInteger(int val, int &stepY);
	void SetYMaxMin(int max);
	void DrawBackScreen(CDC *pDC);
	virtual ~CDataView();

	// Generated message map functions
protected:
	//{{AFX_MSG(CDataView)
	afx_msg void OnSetFocus(CWnd* pOldWnd);
	afx_msg void OnKillFocus(CWnd* pNewWnd);
	afx_msg void OnPaint();
	afx_msg void OnMouseMove(UINT nFlags, CPoint point);
	afx_msg void OnLButtonDown(UINT nFlags, CPoint point);
	afx_msg BOOL OnEraseBkgnd(CDC* pDC);
	afx_msg void OnTimer(UINT nIDEvent);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_DATAVIEW_H__D2A5A203_7B14_4798_8834_77B4201F0AB5__INCLUDED_)
