#if !defined(AFX_AXISCONFIG_H__8CC0D28B_B8D6_4B10_9FF4_7E4A4A1ED2D6__INCLUDED_)
#define AFX_AXISCONFIG_H__8CC0D28B_B8D6_4B10_9FF4_7E4A4A1ED2D6__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// AxisConfig.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CAxisConfig dialog

class CAxisConfig : public CDialog
{
// Construction
public:
	void SetAxis(UINT uiAxis);
	void SetAxisConfiguration(int ofs);
	void SetEncoderDirection(int ofs);
	void SetVelimode(int ofs);
	void SetPosimode(int ofs);
	void SetFeedback(int ofs);
	void SetVoltage(int ofs);
	void SetControl(int ofs);
	void SetControlLoop(int ofs);
	void SetMotorStatus(int ofs);
	CAxisConfig(CWnd* pParent = NULL);   // standard constructor
	void SetSheetPtr(LPVOID lpv) {m_lpSheetPtr = lpv; }
	void * m_lpSheetPtr;

	char *GetParamPath();

// Dialog Data
	//{{AFX_DATA(CAxisConfig)
	enum { IDD = IDD_DIALOG_AXIS_CONFIG };
	UINT	m_uiAxis;
	int		m_iControlTorque;
	int		m_iControlVelocity;
	int		m_iControlCloseLoop;
	int		m_iControlOpenLoop;
	int		m_iEncoderCCW;
	int		m_iEncoderCW;
	int		m_iFeedbackAnalog;
	int		m_iFeedbackBianalog;
	int		m_iFeedbackEncoder;
	int		m_iMotorMicro;
	int		m_iMotorServo;
	int		m_iMotorStepper;
	int		m_iPosimodeAlways;
	int		m_iPosimodeOnlyStanding;
	int		m_iVelimodeAlways;
	int		m_iVelimodeOnlyStanding;
	int		m_iVoltageBipolar;
	int		m_iVoltageUnipolar;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CAxisConfig)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(CAxisConfig)
	afx_msg void OnRadioMotorServo();
	afx_msg void OnRadioMotorStepper();
	afx_msg void OnRadioMotorMicro();
	afx_msg void OnRadioControlOpenloop();
	afx_msg void OnRadioControlClosedloop();
	afx_msg void OnRadioControlVelocity();
	afx_msg void OnRadioControlTorque();
	afx_msg void OnRadioVoltageBipolar();
	afx_msg void OnRadioVoltageUnipolar();
	afx_msg void OnRadioFeedbackEncoder();
	afx_msg void OnRadioFeedbackAnalog();
	afx_msg void OnRadioFeedbackBianalog();
	afx_msg void OnRadioPosimodeOnlyStanding();
	afx_msg void OnRadioPosimodeAlways();
	afx_msg void OnRadioVelimodeOnlyStanding();
	afx_msg void OnRadioVelimodeAlways();
	afx_msg void OnRadioEncoderdirectionCw();
	afx_msg void OnRadioEncoderdirectionCcw();
	virtual BOOL OnInitDialog();
	afx_msg void OnButtonAxisPrev();
	afx_msg void OnButtonAxisNext();
	afx_msg void OnClose();
	virtual void OnCancel();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_AXISCONFIG_H__8CC0D28B_B8D6_4B10_9FF4_7E4A4A1ED2D6__INCLUDED_)
