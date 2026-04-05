// DataView.cpp : implementation file
//

#include "stdafx.h"
#include "AmcSetup.h"
#include "DataView.h"

#include <math.h>

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CDataView

CDataView::CDataView()
{
	m_pDataX = NULL;
	m_pDataY = NULL;
	m_pDataLen = 0;

	SetYMaxMin(32767);

	m_bHold = FALSE;

}

CDataView::~CDataView()
{
	if (m_pDataX) delete[] m_pDataX;
	if (m_pDataY) delete[] m_pDataY;
}


BEGIN_MESSAGE_MAP(CDataView, CWnd)
	//{{AFX_MSG_MAP(CDataView)
	ON_WM_SETFOCUS()
	ON_WM_KILLFOCUS()
	ON_WM_PAINT()
	ON_WM_MOUSEMOVE()
	ON_WM_LBUTTONDOWN()
	ON_WM_ERASEBKGND()
	ON_WM_TIMER()
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
// CDataView message handlers

BOOL CDataView::PreCreateWindow(CREATESTRUCT& cs) 
{
	// TODO: Add your specialized code here and/or call the base class
	return CWnd::PreCreateWindow(cs);
}

void CDataView::OnSetFocus(CWnd* pOldWnd) 
{
	// TODO: Add your message handler code here
}

void CDataView::OnKillFocus(CWnd* pNewWnd) 
{
//	CWnd::OnKillFocus(pNewWnd);
	
	// TODO: Add your message handler code here
	
}

void CDataView::DrawBackScreen(CDC *pDC)
{
	CRect rect;
	CPen cPen, *pOldPen;
	CBrush cBrush;

	cBrush.CreateSolidBrush(RGB(0, 0, 0));
	cPen.CreatePen(PS_DOT, 1, RGB(20, 90, 20));
	GetClientRect(&rect);

	// 전체화면을 지운다.
	pDC->SetBkColor(RGB(0, 0, 0));
	pDC->FillSolidRect(&rect, RGB(0, 0, 0));
//	pDC->FillRect(&rect, &cBrush);

	m_xScale = (double) rect.Width() / m_nXWidth;
	
	// x축에 10단계의 눈금을 넣는다.
	int stepX = 100;
	pOldPen = pDC->SelectObject(&cPen);
	for (int i = 0; i < m_nXWidth; i += stepX)
	{
		int xpos = GetXPos(i);

		pDC->MoveTo(xpos, rect.bottom - 1);
		pDC->LineTo(xpos, rect.top + 1);
	}
//	PutXScaleMark(0, m_nXWidth, stepX);

	// y축에 10단계의 눈금을 넣는다.
	m_yScale = (double) rect.Height() / m_nYHeight;

	// 32767의 경우 30000, 1234의 경우 1000을 계산해 낸다.
	int stY, stepY;
	int i;
	stY = CalcMinInteger(m_nMaxY, stepY);
	for (i = stY; i >= m_nMinY; i -= stepY)
	{
		int ypos = GetYPos(i);

		pDC->MoveTo(rect.left + 1, ypos);
		pDC->LineTo(rect.right - 1, ypos);
	}
	PutYScaleMark(stY, m_nMinY, stepY);

	// 값이 0일때를 다시한번 그린다.
	pDC->SelectObject(pOldPen);
	cPen.DeleteObject();
	cPen.CreatePen(PS_DOT, 1, RGB(20, 200, 20));
	pOldPen = pDC->SelectObject(&cPen);
	pDC->MoveTo(rect.left+1, GetYPos(0));
	pDC->LineTo(rect.right-1, GetYPos(0));

	pDC->SelectObject(pOldPen);
	cPen.DeleteObject();
	cBrush.DeleteObject();
}

void CDataView::SetYMaxMin(int max)
{
	m_nMaxY = max;
	m_nMinY = -max - 1;
	m_nYHeight = m_nMaxY - m_nMinY + 1;
	m_nSaveOfs = 0;
	m_bDataOverwrite = FALSE;
}

int CDataView::CalcMinInteger(int val, int &stepY)
{
	int nlog10 = (int)log10(val);	// 32767의 경우 4//2011.10.10
	stepY = (int)pow(10, nlog10);//2011.10.10
	int nr = val / stepY;	// pow(10, 4) => 10000, so nr = int(3.2767)

	int nrtn = nr * stepY;

	if (nrtn >= 20000) stepY = 10000;
	else if (nrtn >= 10000) stepY = 5000;
	else if (nrtn >= 5000) stepY = 2000;
	else if (nrtn >= 2000) stepY = 1000;
	else if (nrtn >= 1000) stepY = 500;
	else if (nrtn >= 500) stepY = 200;
	else if (nrtn >= 200) stepY = 100;
	else if (nrtn >= 100) stepY = 50;
	else if (nrtn >= 50) stepY = 20;
	else stepY = 20;

	while (nrtn + stepY < val) nrtn += stepY;

	return nrtn;
}


void CDataView::OnPaint() 
{
	CPaintDC dc(this); // device context for painting
	
	// TODO: Add your message handler code here
	CDC *pDC = GetDC();
	OnDraw(pDC);

	ReleaseDC(pDC);
	// Do not call CWnd::OnPaint() for painting messages
}

void CDataView::OnDraw(CDC *pDC)
{
	int n = 0;
}

void CDataView::SetFocus()
{
	InitIfNecessary();

	CDC *pDC = GetDC();
	DrawBackScreen(pDC);
	ReleaseDC(pDC);
}

void CDataView::KillFocus()
{

}


void CDataView::SetData(short dat)
{
	int x, y;

	if (m_bHold == TRUE) return;

	CDC *pDC = GetDC();

	int rpmode = pDC->GetROP2();
	pDC->SetROP2(R2_XORPEN);

	x = GetXPos(m_nSaveOfs);
	if (m_bDataOverwrite == TRUE)
	{
		int ofs = m_nSaveOfs + 10;
		if (ofs >= m_nXWidth) ofs = 0;
		// 예전에 그려놓은 위치를 삭제한다.
		pDC->SetPixelV(m_pDataX[ofs], m_pDataY[ofs], RGB(255, 255, 0));
	}
	y = GetYPos(dat);
	if (y < m_sWindowRect.top + 1) y = m_sWindowRect.top + 1;
	else if (m_sWindowRect.bottom - 1 <= y) y = m_sWindowRect.bottom - 1;

	m_pDataX[m_nSaveOfs] = x;
	m_pDataY[m_nSaveOfs] = y;
	if (++m_nSaveOfs >= m_nXWidth) 
	{
		m_nSaveOfs = 0;
		m_bDataOverwrite = TRUE;

		// 하나 앞서서 지우도록 하기 위함임.
		// 예전에 그려놓은 위치를 삭제한다.
		for (int i = 0; i < 10; i ++)
			pDC->SetPixelV(m_pDataX[i], m_pDataY[i], RGB(255, 255, 0));
	}

	pDC->SetPixelV(x, y, RGB(255, 255, 0));
	pDC->SetROP2(rpmode);
	ReleaseDC(pDC);
/*
	int stY, stepY;
	stY = CalcMinInteger(m_nMaxY, stepY);
	PutYScaleMark(stY, m_nMinY, stepY);
*/
}

void CDataView::OnMouseMove(UINT nFlags, CPoint point) 
{
	// TODO: Add your message handler code here and/or call default
	
	CWnd::OnMouseMove(nFlags, point);
}

void CDataView::OnLButtonDown(UINT nFlags, CPoint point) 
{
	// TODO: Add your message handler code here and/or call default
	
	CWnd::OnLButtonDown(nFlags, point);
}

int CDataView::GetYPos(int ival)
{
	return (int) (m_sWindowRect.bottom - (ival - m_nMinY) * m_yScale);//2011.10.10
}

int CDataView::GetXPos(int ival)
{
	return (int)(ival * m_xScale);//2011.10.10
}

BOOL CDataView::OnEraseBkgnd(CDC* pDC) 
{
	// TODO: Add your message handler code here and/or call default
	InitIfNecessary();
	DrawBackScreen(pDC);
	
	return CWnd::OnEraseBkgnd(pDC);
}

void CDataView::PutYScaleMark(int min, int max, int step)
{
	CPen cPen, *pOldPen;
	CRect rect;
	CWnd *pWnd = m_phYMarkWnd;

	pWnd->GetClientRect(&rect);

	CDC *pDC = pWnd->GetDC();
	CFont *pOldFont = pDC->SelectObject(m_pTextFont);
	pDC->FillSolidRect(&rect, RGB(180, 180, 180));
	
	
	cPen.CreatePen(PS_SOLID, 1, RGB(200, 0, 200));
	pOldPen = pDC->SelectObject(&cPen);

	int nBkMode;
	nBkMode = pDC->SetBkMode(TRANSPARENT);

//	pDC->FillSolidRect(&rect, RGB(0, 0, 0));

	// X 눈금을 보여주는 창과 1:1 크기 이므로 미리 계산해 놓은 비율로 좌표를 계산한다.
	for (int i = min; i > max; i -= step)
	{
		int ypos = (int) (rect.bottom - (i - m_nMinY) * m_yScale);//2011.10.10
		
		ypos -= 1;

		int yTextPos = ypos;
		if (i == min) yTextPos -= 4;
		else if (i - step < max) yTextPos -= 10;
		else yTextPos -= 5;

		pDC->MoveTo(rect.left + 1, ypos);
		pDC->LineTo(rect.left + 5, ypos);

		CString cstr;
		cstr.Format("%d", i);
		pDC->TextOut(rect.left + 10, yTextPos, cstr);
	}
	pDC->SetBkMode(nBkMode);
	pDC->SelectObject(pOldPen);
	pDC->SelectObject(pOldFont);
	pWnd->ReleaseDC(pDC);
	cPen.DeleteObject();
}

void CDataView::SetXYMarkWnd(CWnd *pX, CWnd *pY)
{
	InitIfNecessary();

	m_phXMarkWnd = pX;
	m_phYMarkWnd = pY;
}


BOOL CDataView::DestroyWindow() 
{
	// TODO: Add your specialized code here and/or call the base class
	m_pTextFont->DeleteObject();
	return CWnd::DestroyWindow();
}

void CDataView::InitIfNecessary()
{
	if (m_pDataX == NULL)
	{
		CRect rect;

		GetClientRect(&rect);
		// 배열 초기화 및 지우기
		m_pDataLen = max(rect.Width(), 500);	// 최대 5초까지를 표시하기 위함.
		m_pDataX =new short[m_pDataLen];
		m_pDataY =new short[m_pDataLen];
		memset(m_pDataX, 0, sizeof(short) * m_pDataLen);
		memset(m_pDataY, 0, sizeof(short) * m_pDataLen);
		m_nSaveOfs = 0;

		m_nXWidth = m_pDataLen;	// 최근 1초까지의 데이터를 화면에 표시토록 한다.

		m_bDataOverwrite = FALSE;
		m_pTextFont = new CFont;
		BOOL bUnderline = FALSE;
		m_pTextFont->CreateFont(12, 0, 0, 0, 1, 0, bUnderline, 0,0,0, 0,0,0, "Arial");

		m_sWindowRect = rect;
	}
}



int CDataView::GetMax()
{
	return m_nMaxY;
}

void CDataView::OnTimer(UINT nIDEvent) 
{
	// TODO: Add your message handler code here and/or call default
	if (nIDEvent == 0)
	{
		KillTimer(0);
		SetFocus();
	}
	CWnd::OnTimer(nIDEvent);
}

void CDataView::SetHold(BOOL bHold)
{
	m_bHold = bHold;
}
