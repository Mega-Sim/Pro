#ifndef		__FSYSTEM_H
#define		__FSYSTEM_H

#include "amcdef.h"

#define		FSCMD_FILENUM		0x0100		// 파일의 갯수
#define		FSCMD_FILENAME		0x0101		// 파일의 이름
#define		FSCMD_FILESIZE		0x0102		// 파일의 크기
#define		FSCMD_CPUTYPE_READ	0x0000
#define		FSCMD_CPUTYPE_WRITE	0x0001
#define		FSCMD_FILE_UPLOAD	0x0110
#define		FSCMD_FILE_DNLOAD	0x0111
#define		FSCMD_BL_DNLOAD		0x0112
#define		FSCMD_FS_FORMAT		0x0200
#define		FSCMD_DEL_FILE		0x0202





#pragma	pack(1)

// 명령의 교환이나, 파일의 교환을 위해서 정의된 구조체.
// 2007.12.11, ckyu
typedef struct
{
    UINT	uiCrc32;	// 전송될 전체 데이터의 crc32
    UINT	uiTotLen;	// 전송될 전체 데이터의 바이트수
    USHORT	usBlkID;	// 현재 이용할 블럭의 ID
    USHORT	usCmd16;	// 단일명령에 이용하기위한 필드
    UCHAR	ucIndex;	// 순서 지정을 위한 용도.
    UCHAR	ucDir;		// 0:write, 1:read from IPC
    
    UCHAR 	uszFileName[50];
} FILECMD, *LPFILECMD;

typedef struct
{
	UINT	uiCrc32;
	USHORT	usBytes;	// 유효한 바이트수
	UCHAR	uszData[256];
} FILEBODY, *LPFILEBODY;

typedef struct
{
    UINT	uiCrc32;	// 뒤에 전송되는 50바이트에 대한 crc32
    UINT	uiTotLen;		// 파일크기를 올릴때 이용.
    UINT	uiValidBytes;	// 유효한 바이트수
    UCHAR	uszCmd[50];
} FILECMD_ACK, *LPFILECMD_ACK;

#pragma	pack()






#endif
