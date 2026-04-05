#ifndef		__AMC_FS_H
#define		__AMC_FS_H

#include <string.h>
#include <io.h>
#include "amc_type.h"

#ifdef AMCLIB_EXPORTS
#define AMCLIB_API __declspec(dllexport)
#else
#define AMCLIB_API __declspec(dllimport)
#endif


#define		FSCMD_FILENUM		0x0100		// ������ ����
#define		FSCMD_FILENAME		0x0101		// ������ �̸�
#define		FSCMD_FILESIZE		0x0102		// ������ ũ��
#define		FSCMD_CPUTYPE_READ	0x0000
#define		FSCMD_CPUTYPE_WRITE	0x0001
#define		FSCMD_FILE_UPLOAD	0x0110
#define		FSCMD_FILE_DNLOAD	0x0111
#define		FSCMD_BL_DNLOAD		0x0112
#define		FSCMD_FS_FORMAT		0x0200
#define		FSCMD_DEL_FILE		0x0202
#define		FSCMD_SET_BOOT		0x0203
#define		FSCMD_GET_SYSTEM	0x0300





#pragma pack(1)

// ����� ��ȯ�̳�, ������ ��ȯ�� ���ؼ� ���ǵ� ����ü.
// 2007.12.11, ckyu
typedef struct
{
    UINT	uiCrc32;	// ���۵� ��ü �������� crc32
    UINT	uiTotLen;	// ���۵� ��ü �������� ����Ʈ��
    USHORT	usBlkID;	// ���� �̿��� ���� ID
    USHORT	usCmd16;	// ���ϸ�ɿ� �̿��ϱ����� �ʵ�
    UCHAR	ucIndex;	// ���� ������ ���� �뵵.
    UCHAR	ucData;		// 0:cmd, 1:upload-data, 2:dnload-data
    USHORT	usPackLen;	// filler
    
    UCHAR 	uszFileName[50];

	// ����� ���ϸ��� ����Ʈ���� �����Ѵ�.
	int SetFilename(char *pname)
	{
		int len = strlen(pname);
		if (len >= sizeof(uszFileName)) len = sizeof(uszFileName) - 1;
		strncpy((char *) uszFileName, pname, len);
		uszFileName[len] = '\0';
		return len;
	}
} FILECMD, *LPFILECMD;

typedef struct
{
	UINT	uiCrc32;
	USHORT	usBytes;	// ��ȿ�� ����Ʈ��
	UCHAR	uszData[256];
} FILEBODY, *LPFILEBODY;

typedef struct
{
    UINT	uiCrc32;	// �ڿ� ���۵Ǵ� 50����Ʈ�� ���� crc32
    UINT	uiTotLen;		// ����ũ�⸦ �ø��� �̿�.
    UINT	uiValidBytes;	// ��ȿ�� ����Ʈ��
    USHORT	usFiller;
	USHORT	usPackLen;
    UCHAR	uszCmd[50];
	void GetFileName(char *pdstn)
	{
		int len = usPackLen - 16;
		if (len > 0)
		{
			memcpy(pdstn, uszCmd, len);
			pdstn[len] = '\0';
		}
	}
} FILECMD_ACK, *LPFILECMD_ACK;


typedef struct
{
	char m_sName[60];
	unsigned int m_nSize;
	unsigned char *m_pBuffer;
} FILE_DESC;




typedef struct _SYSTEM_DATA
{
	int m_nCPUType;   //0:6712, 1:6713
	char m_sRunFileName[48];
}SYSTEM_DATA;


#pragma pack()


#ifdef		__cplusplus
extern "C" {
#endif

AMCLIB_API int fs_files(int *pfiles);
AMCLIB_API int fs_getfilename(int ofs, char *pszDstn);
AMCLIB_API int fs_getfilesize(int ofs, unsigned long *plen);
AMCLIB_API int fs_getfilenamesize(char *pszname, unsigned long *plen);
AMCLIB_API int fs_deletefile(char *pszname);
AMCLIB_API int fs_download(char *pszname, int *prtn, HWND hWnd, unsigned int uiMsg);
AMCLIB_API int fs_install(char *pszBootLoader, char *pszBootFile, int *prtn, HWND hWnd, UINT uiMsg);
AMCLIB_API int fs_upload(char *pszfile, char *pdstn, unsigned long *plen, HWND hWnd, unsigned int uiMsg);
AMCLIB_API int fs_setbootfile(char *pszname);
AMCLIB_API int fs_format();
AMCLIB_API int fs_getsysteminfo(SYSTEM_DATA *pdata);	// get cpu type and boot file name.
AMCLIB_API int fs_dumpinfo(int nzDumpInfo);	// get cpu type and boot file name.
AMCLIB_API int fs_autopatch(char *pszpath = 0, char *pszname = 0, HWND hWnd = 0);
AMCLIB_API int fs_bootcheck(pINT bootcheck);

#ifdef		__cplusplus
};
#endif

#endif
