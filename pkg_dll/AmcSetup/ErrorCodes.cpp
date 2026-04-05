// ErrorCodes.cpp : implementation file
//

#include "stdafx.h"
#include "amcsetup.h"
#include "ErrorCodes.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CErrorCodes property page

IMPLEMENT_DYNCREATE(CErrorCodes, CPropertyPage)

CErrorCodes::CErrorCodes() : CPropertyPage(CErrorCodes::IDD)
{
	//{{AFX_DATA_INIT(CErrorCodes)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
}

CErrorCodes::~CErrorCodes()
{
}

void CErrorCodes::DoDataExchange(CDataExchange* pDX)
{
	CPropertyPage::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CErrorCodes)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CErrorCodes, CPropertyPage)
	//{{AFX_MSG_MAP(CErrorCodes)
		// NOTE: the ClassWizard will add message map macros here
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CErrorCodes message handlers
