// Console.h: interface for the CConsole class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(AFX_CONSOLE_H__9A8E6043_667F_4BC7_8E86_5ADBD61DB0DE__INCLUDED_)
#define AFX_CONSOLE_H__9A8E6043_667F_4BC7_8E86_5ADBD61DB0DE__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class CConsole  
{
public:
	BOOL InitInstance(char *pszTitle = NULL);
	BOOL m_bFirst;
	void PutsConsole(char *pszString);
	HANDLE hStdOut;
	CConsole();
	virtual ~CConsole();

};

#endif // !defined(AFX_CONSOLE_H__9A8E6043_667F_4BC7_8E86_5ADBD61DB0DE__INCLUDED_)
