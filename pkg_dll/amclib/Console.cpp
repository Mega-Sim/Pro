// Console.cpp: implementation of the CConsole class.
//
//////////////////////////////////////////////////////////////////////

#include "stdafx.h"
#include "Console.h"

#ifdef _DEBUG
#undef THIS_FILE
static char THIS_FILE[]=__FILE__;
#define new DEBUG_NEW
#endif

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CConsole::CConsole()
{
	hStdOut = NULL;
}

CConsole::~CConsole()
{
	FreeConsole();
	if (hStdOut)
		CloseHandle(hStdOut);
}


static BOOL WINAPI ConsoleCtrlHandler(DWORD dwCtrlType)
{
	int n = 0;

	switch (dwCtrlType)
	{
	case CTRL_C_EVENT:
	case CTRL_BREAK_EVENT:
		return TRUE;
	}
	return FALSE;	// 다른 CtriHandler를 검색하도록 한다.
}

BOOL CConsole::InitInstance(char *pszTitle) 
{
	// TODO: Add your command handler code here
	static BOOL bFirst = TRUE;
	if (m_bFirst)
	{
		m_bFirst = FALSE;
		AllocConsole();
		SetConsoleTitle(pszTitle);

		BOOL bf = SetConsoleCtrlHandler(ConsoleCtrlHandler, TRUE);
		
		// ctrl-c만 무시하도록 한다.  <== 별로 효과가 없다.
		//SetConsoleCtrlHandler(NULL, TRUE);
		hStdOut = GetStdHandle(STD_OUTPUT_HANDLE);
		return TRUE;

	} else
		hStdOut = NULL;
	return FALSE;
}


void CConsole::PutsConsole(char *pszString)
{
	char szStr[300];
	SYSTEMTIME stCur;

	DWORD dwBytes;
	if (hStdOut)
	{
		GetLocalTime(&stCur);
		sprintf(szStr, "%d%02d%02d %02d:%02d:%02d-%03d : %s",
			stCur.wYear, stCur.wMonth, stCur.wDay, stCur.wHour, stCur.wMinute, stCur.wSecond, stCur.wMilliseconds,
			pszString);
		WriteFile(hStdOut, szStr, lstrlen(szStr), &dwBytes, NULL);
	}
}

