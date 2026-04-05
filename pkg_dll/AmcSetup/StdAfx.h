// stdafx.h : include file for standard system include files,
//  or project specific include files that are used frequently, but
//      are changed infrequently
//

#if !defined(AFX_STDAFX_H__81CD5E2E_11E9_4E7A_90DE_F70A091D9DF0__INCLUDED_)
#define AFX_STDAFX_H__81CD5E2E_11E9_4E7A_90DE_F70A091D9DF0__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#define VC_EXTRALEAN		// Exclude rarely-used stuff from Windows headers

#include <afxwin.h>         // MFC core and standard components
#include <afxext.h>         // MFC extensions
#include <afxdisp.h>        // MFC Automation classes
#include <afxdtctl.h>		// MFC support for Internet Explorer 4 Common Controls
#ifndef _AFX_NO_AFXCMN_SUPPORT
#include <afxcmn.h>			// MFC support for Windows Common Controls
#endif // _AFX_NO_AFXCMN_SUPPORT

#include "../include/amc.h"

#define		AMC_PARAMETER_FILENAME		"AMCParam.ini"

extern char *GetSection(int ax);
extern char *GetString(int val);

//111029 syk_start
// 2.9.8  --> 2.10.1 수정사항, 버젼만 바꿈 
#define		VERSION_SETUP		VERSION_PCLIB // 120702 통합 버젼 2.9.x 로 사용

//111029 syk_end

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_STDAFX_H__81CD5E2E_11E9_4E7A_90DE_F70A091D9DF0__INCLUDED_)
