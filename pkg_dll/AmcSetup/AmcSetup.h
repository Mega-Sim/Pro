// AmcSetup.h : main header file for the AMCSETUP application
//

#if !defined(AFX_AMCSETUP_H__B39CA126_C18A_451D_B1D0_D2D476FF6835__INCLUDED_)
#define AFX_AMCSETUP_H__B39CA126_C18A_451D_B1D0_D2D476FF6835__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#ifndef __AFXWIN_H__
	#error include 'stdafx.h' before including this file for PCH
#endif

#include "resource.h"		// main symbols

/////////////////////////////////////////////////////////////////////////////
// CAmcSetupApp:
// See AmcSetup.cpp for the implementation of this class
//

class CAmcSetupApp : public CWinApp
{
public:
	CAmcSetupApp();

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CAmcSetupApp)
	public:
	virtual BOOL InitInstance();
	//}}AFX_VIRTUAL

// Implementation

	//{{AFX_MSG(CAmcSetupApp)
		// NOTE - the ClassWizard will add and remove member functions here.
		//    DO NOT EDIT what you see in these blocks of generated code !
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};


/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_AMCSETUP_H__B39CA126_C18A_451D_B1D0_D2D476FF6835__INCLUDED_)
