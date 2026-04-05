#include "stdafx.h"
#include "pcdef.h"
#include "../include/amc_fs.h"
#include <stdio.h>
#include "amc_internal.h"
#include "crc/crc32.h"
#include "log.h"
#include <chrono>

#define		MMC_OK					0
#define		AMC_SUCCESS				0
#define		MMC_TIMEOUT_ERR			2

extern "C" 
{
extern INT mmc_error;
extern INT MMCMutexLock(void);
extern INT MMCMutexUnlock(void);
};

// ��Ŷ�� ���̸� �����Ѵ�.
int ReadAck(FILECMD_ACK *pAck)
{
	_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR, pAck, 16);
	if (pAck->usPackLen > 16)
	{
		// ���ϸ��� �ִ°�� ���ϸ��� ��´�.
		int len = pAck->usPackLen - 16;
		_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 16, pAck->uszCmd, len);
		pAck->uszCmd[len] = 0;
	}
	return pAck->usPackLen;
}

// fs�� ����� ������ ������ �����´�. 
INT fs_files(int *pfiles)
{
	FILECMD sCmd;
	FILECMD_ACK sAck;
	INT err;

	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = FSCMD_FILENUM;
	sCmd.usPackLen = 16;
	sCmd.ucIndex = 0;
	sCmd.usBlkID = 0;
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);
	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	ReadAck(&sAck);
	MMCMutexUnlock();

	*pfiles = (int)sAck.uiTotLen;
	return err;
}

// ofs�� �Ѱܹ��� ������ ���ϸ��� �����Ѵ�.
INT fs_getfilename(int ofs, char *pszDstn)
{
	FILECMD sCmd;
	FILECMD_ACK sAck;
	INT err;

	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = FSCMD_FILENAME;
	sCmd.usPackLen = 16;
	sCmd.ucIndex = ofs;
	sCmd.usBlkID = 0;
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);
	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	ReadAck(&sAck);
	MMCMutexUnlock();

	sAck.GetFileName(pszDstn);
	return err;
}


// ofs�� �Ѱܹ��� ������ ������ ũ�⸦ �����Ѵ�.
INT fs_getfilesize(int ofs, ULONG *plen)
{
	FILECMD sCmd;
	FILECMD_ACK sAck;
	INT err;

	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = FSCMD_FILESIZE;
	sCmd.usPackLen = 16;	// ����Ʈ���� ���� �����V, �̸����� ������ �޶�����.
	sCmd.ucIndex = ofs;
	sCmd.usBlkID = 0;
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);
	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	ReadAck(&sAck);
	MMCMutexUnlock();

	*plen = sAck.uiTotLen;
	return err;
}


// ofs�� �Ѱܹ��� ������ ������ ũ�⸦ �����Ѵ�.
INT fs_getfilenamesize(char *pszname, ULONG *plen)
{
	FILECMD sCmd;
	FILECMD_ACK sAck;
	INT err;

	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = FSCMD_FILESIZE;
	sCmd.usPackLen = 16;
	sCmd.ucIndex = 0;
	sCmd.usBlkID = 0;
	sCmd.usPackLen += sCmd.SetFilename(pszname);
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);
	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	ReadAck(&sAck);
	MMCMutexUnlock();

	*plen = sAck.uiTotLen;
	return err;
}


// ������ �����Ѵ�. (���ϵ�ī��� ������ �ʴ´�.)
// ������ ��� 1
// �����Ѱ�� 0�� �����Ѵ�.
INT fs_deletefile(char *pszname)
{
	FILECMD sCmd;
	FILECMD_ACK sAck;
	INT err;

	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = FSCMD_DEL_FILE;
	sCmd.usPackLen = 16;
	sCmd.usPackLen += sCmd.SetFilename(pszname);
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);
	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	ReadAck(&sAck);
	MMCMutexUnlock();

	return err;
}

BOOL IsFileExist(char *psz1, char *psz2)
{
	FILE *fp = fopen(psz1, "rb");
	if (fp == NULL) return FALSE;
	fclose(fp);

	fp = fopen(psz2, "rb");
	if (fp == NULL) return FALSE;
	fclose(fp);
	
	return TRUE;
}

// error codes:
// -1: file open error
// -2: no ack
// -3: some error
AMCLIB_API int fs_install_file(char *pszFile, int cmd, int *prtn, HWND hWnd, UINT uiMsg);
INT fs_install(char *pszBootLoader, char *pszBootFile, int *prtn, HWND hWnd, UINT uiMsg)
{
    MYLOG("fs_install\n");
    
    ULONG ulSize =0;
	
	char szFileNameOnly[MAX_PATH];
	if (strrchr(pszBootFile, '\\') != NULL) strcpy(szFileNameOnly, strrchr(pszBootFile, '\\')+1);
	else if (strrchr(pszBootFile, '/') != NULL) strcpy(szFileNameOnly, strrchr(pszBootFile, '/')+1);
	else strcpy(szFileNameOnly, pszBootFile);

	if (IsFileExist(pszBootLoader, pszBootFile) == FALSE)
	{
		return MMC_NON_EXIST;
	}

	int nfile, nrtn;
	SYSTEM_DATA sData;
	char szname[100];
	
	if (fs_getsysteminfo(&sData) != MMC_OK) return MMC_NON_EXIST;
	
	if (fs_files(&nfile) == MMC_OK)
	{
		for (int i = 0; i < nfile; i ++)
		{
			if(fs_getfilename(i, szname) != MMC_OK) return MMC_NON_EXIST;

			if (strcmp(sData.m_sRunFileName, szname)!= 0)
			{
				if(fs_deletefile(szname) != MMC_OK) return MMC_NON_EXIST;
			}
		}
	}

	if(strcmp(sData.m_sRunFileName, szFileNameOnly) == 0)
	{
		rename(pszBootFile, "C:\\User\\OHT\\DataFiles\\amc.out");

		memset(szFileNameOnly, 0, MAX_PATH);			
			
		pszBootFile = "C:\\User\\OHT\\DataFiles\\amc.out";
		strcpy(szFileNameOnly, "amc.out");
	}		
			
	nrtn = fs_install_file(pszBootFile, FSCMD_FILE_DNLOAD, prtn, hWnd, uiMsg);
		
	if (nrtn == MMC_OK)
	{
		nrtn = fs_setbootfile(szFileNameOnly);
	}
	
	return nrtn;
}

// error codes:
// -1: file open error
// -2: no ack
// -3: some error
INT fs_download(char *pszname, int *prtn, HWND hWnd, UINT uiMsg)
{
	ULONG ulSize =0;
	
	char szFile[MAX_PATH];
	if (strrchr(pszname, '\\') != NULL) strcpy(szFile, strrchr(pszname, '\\')+1);
	else if (strrchr(pszname, '/') != NULL) strcpy(szFile, strrchr(pszname, '/')+1);
	else strcpy(szFile, pszname);

	return fs_install_file(pszname, FSCMD_FILE_DNLOAD, prtn, hWnd, uiMsg);
}


// error codes: prtn
//  0: ����(write)
// -1: file open error
// -2: no ack
// -3: some error
// -4: file size over
// -5: lack of space
INT fs_install_file(char *pszFile, int cmd, int *prtn, HWND hWnd, UINT uiMsg)
{
	ULONG ulSize =0;
	FILECMD sCmd;
	FILECMD_ACK sAck;
	INT err;
	
	char szFile[MAX_PATH];
	if (strrchr(pszFile, '\\') != NULL) strcpy(szFile, strrchr(pszFile, '\\')+1);
	else if (strrchr(pszFile, '/') != NULL) strcpy(szFile, strrchr(pszFile, '/')+1);
	else strcpy(szFile, pszFile);

	FILE *fp = fopen(pszFile, "rb");  
	if (fp == NULL) { *prtn = -1; return MMC_NON_EXIST; }
	fseek(fp, 0L, SEEK_END);
	ulSize = ftell(fp);
	fseek(fp, 0L, SEEK_SET);
	
	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = cmd;
	sCmd.usPackLen = 16;
	sCmd.uiTotLen = ulSize;
	sCmd.usPackLen += sCmd.SetFilename(szFile);
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{	
		fclose(fp);
		return err;
	}

	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);

	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
	    fclose(fp);
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	ReadAck(&sAck);
	MMCMutexUnlock();

	if (sAck.uiValidBytes != 1) 
	{
		*prtn = -2;
		mmc_error = MMC_TIMEOUT_ERR;
	    fclose(fp);
		return MMC_TIMEOUT_ERR;
	}

	int ofs = 0, len = 0;
	int nBlkLen = ulSize / 256;
	if ((unsigned long)(nBlkLen * 256) != ulSize) nBlkLen ++;

	if (hWnd)
	{
		SendMessage(hWnd, uiMsg, 0, nBlkLen);
	}
	char data[256];
	int blkid;
	for (blkid = 0; blkid < nBlkLen; blkid ++)
	{
		len = min(ulSize - blkid * 256, 256);
		fread(data, 1, len, fp);

		sCmd.usBlkID = blkid;
		sCmd.uiCrc32 = _crc32_get_crc(data, len);
		sCmd.ucData = 2;	// for download
		sCmd.usPackLen = len;

		if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
		{
			fclose(fp);
			return err;
		}

		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, 16);
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR+16, data, len);

		_flush_dpram(AMC_FS_CMD);

		// DSP�� �����͸� �������� �θ��������� �뺸�ϴ� ������ �����Ѵ�.
		if (hWnd) SendMessage(hWnd, uiMsg, blkid, nBlkLen);
		if (blkid == nBlkLen-1)
		{
			if (hWnd) SendMessage(hWnd, uiMsg, nBlkLen, nBlkLen);
			Sleep(2000);
		}

        //������ ����� dsp�� �����ϸ� dsp ���� writefile�� ������ �� ack�� �ִµ� ���ϻ���� 
        //Ŀ���� ack ���� �ð��� ������Ƿ� �Ʒ��� ���� ������� ���� 
		int loop_end =5;		
		for(int loop=0; loop <loop_end; loop++)
		{
			if(_wait_for_reply((blkid == nBlkLen-1)? 15000 : 3000) != AMC_SUCCESS) 
			{
				if(loop == (loop_end-1))
				{
					MMCMutexUnlock ();
					mmc_error = err = MMC_TIMEOUT_ERR;
					goto go_install_file_end;
				}
			}
			else loop = loop_end;
		}

		ReadAck(&sAck);
		MMCMutexUnlock();

		if (sAck.uiValidBytes == 0) 
		{
			mmc_error = err = MMC_TIMEOUT_ERR;
			break;
		}
	}
go_install_file_end:
	if (hWnd) SendMessage(hWnd, uiMsg, nBlkLen, nBlkLen);

	fclose(fp);
	if (blkid >= nBlkLen) *prtn = 0;
	else{ *prtn = -3; return err; }
    
    //write�� ���¸� �����ش�.
	if(sAck.usFiller==1)		 *prtn = 0;
	else if(sAck.usFiller==2)	 *prtn = -4; 
	else if(sAck.usFiller==3)	 *prtn = -5; 

	return err;
}

// ������ ������ ������ pdstn�� �����Ѵ�.
// *plen ���� ������ ����Ʈ���� ��� �ִ�.
INT fs_upload(char *pszfile, char *pdstn, ULONG *plen, HWND hWnd, unsigned int uiMsg)
{
	FILECMD sCmd;
	FILECMD_ACK sAck;
	ULONG ulSize;
	INT err;

	*plen = 0;
	fs_getfilenamesize(pszfile, &ulSize);

	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = FSCMD_FILE_UPLOAD;
	sCmd.usPackLen = 16;
	sCmd.usPackLen += sCmd.SetFilename(pszfile);
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);

	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	ReadAck(&sAck);
	MMCMutexUnlock();

	if (sAck.uiTotLen != ulSize || sAck.uiTotLen == 0)
	{
		*plen = sAck.uiTotLen;

		return mmc_error = err= MMC_TIMEOUT_ERR;
	}

	int nBlkLen = ulSize / 256;
	if ((unsigned long)nBlkLen*256 != ulSize) nBlkLen ++;
	int ofs;

	if (hWnd) SendMessage(hWnd, uiMsg, 0, nBlkLen);

	sCmd.uiTotLen = sAck.uiTotLen;
	for (int i = 0; i < nBlkLen; i ++)
	{
		sCmd.ucData = 1;	// for upload
		sCmd.usBlkID = i;
		if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) break;
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
		_flush_dpram(AMC_FS_CMD);

		if(_wait_for_reply(3000) != AMC_SUCCESS) 
		{
			MMCMutexUnlock ();
			err = MMC_TIMEOUT_ERR;
			break;
		}
		ReadAck(&sAck);

		int nLen = (int)sAck.uiTotLen;
		ofs = i * 256;
		_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 16, pdstn + ofs, nLen);
		UINT uicrc = _crc32_get_crc((char *) pdstn+ofs, nLen);
		if (uicrc != sAck.uiCrc32)
		{
			MMCMutexUnlock ();
			err = MMC_BOOTPARAM_CRC_ERROR;
			break;
		}

		if (hWnd) SendMessage(hWnd, uiMsg, i, nBlkLen);

		*plen += (ULONG)nLen;

		MMCMutexUnlock();
	}
	if (hWnd) SendMessage(hWnd, uiMsg, nBlkLen, nBlkLen);

	mmc_error = err;
	return err;
}

// ��ΰ� ���� ���ϸ� �Է��Ѵ�.
INT fs_setbootfile(char *pszname)
{
	FILECMD sCmd;
	FILECMD_ACK sAck;
	INT err;

	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = FSCMD_SET_BOOT;
	sCmd.usPackLen = 16;
	sCmd.usPackLen += sCmd.SetFilename(pszname);
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);
	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	ReadAck(&sAck);
	MMCMutexUnlock();

	return err;
}

INT fs_format()
{
    MYLOG("fs_format\n");
    
    FILECMD sCmd;
	FILECMD_ACK sAck;
	INT err;

	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = FSCMD_FS_FORMAT;
	sCmd.usPackLen = 16;
	sCmd.ucIndex = 0;
	sCmd.usBlkID = 0;
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);

	//flash�� ���� ������ ���� ���  ack�� �ʾ� �Ʒ��� ���� �ٲ�. 
	int loop_end =10;		
	for(int loop=0; loop <loop_end; loop++)
	{
		if(_wait_for_reply(10000) != AMC_SUCCESS) 
		{
			if(loop == (loop_end-1))
			{
				MMCMutexUnlock ();
				mmc_error = err = MMC_TIMEOUT_ERR;
				return MMC_TIMEOUT_ERR;
				}
		}
		else loop = loop_end;
	}
	ReadAck(&sAck);
	MMCMutexUnlock();

	return err;
}

INT fs_getsysteminfo(SYSTEM_DATA *pdata)	// get cpu type and boot file name.
{
	FILECMD sCmd;
	FILECMD_ACK sAck;
	INT err;

	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = FSCMD_GET_SYSTEM;
	sCmd.usPackLen = 16;
	sCmd.usBlkID = 0;
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);
	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	int npack = ReadAck(&sAck);
	MMCMutexUnlock();
	
	if (npack > 0)
	{
		sAck.GetFileName(pdata->m_sRunFileName);
		pdata->m_nCPUType = (int)sAck.uiValidBytes;
	}

	return err;
}

AMCLIB_API int fs_dumpinfo(int nzDumpInfo)
{
	FILECMD sCmd;
	FILECMD_ACK sAck;
	INT err;

	memset(&sCmd, 0, sizeof(sCmd));
	sCmd.usCmd16 = 0xff00 | nzDumpInfo;
	sCmd.usPackLen = 16;
	sCmd.usBlkID = 0;
	sCmd.uiCrc32 = _crc32_get_crc((char *) &sCmd.uiTotLen, sCmd.usPackLen - 4);

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sCmd, sCmd.usPackLen);
	_flush_dpram(AMC_FS_CMD);
	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	int npack = ReadAck(&sAck);
	MMCMutexUnlock();
	
	return err;
}

INT fs_autopatch(char *pszpath, char *pszname, HWND hWnd)
{
    //Check Patchfile exists
	TMLOG("Auto Patch Start");
    struct _finddata_t fd;
    intptr_t handle;
    int patchcheckflag = 0;
    auto t0 = std::chrono::system_clock::now();
	int bootcheck = 0;
	_flush_dpram(DOAUTOPATCH);
	return 0;
	/*
	while (true)
	{
		auto running_time = (std::chrono::system_clock::now() - t0);
		int running_time_sec = running_time.count() / 1000000000L;
		fs_bootcheck(&bootcheck);
		if (bootcheck) {
			return 1;
		}
		else {
			//patch fail, timeout
			if (running_time_sec > 300) {
				return 0;
			}
		}
	}
	*/
	/*
    if (((handle = _findfirst("//192.168.0.2/patch/*.tar.gz", &fd)) == -1L) && (patchcheckflag == 0)) {
        //Patch fail - No patch file exists
        _findclose(handle);
        return 1;
    } else{
        patchcheckflag = 1;
        //start autopatch
        _flush_dpram(DOAUTOPATCH);
        //wait for patch done
        while(true){
			auto running_time = (std::chrono::system_clock::now() - t0);
			int running_time_sec = running_time.count() / 1000000000L;
            if (((handle = _findfirst("//192.168.0.2/patch/*.tar.gz", &fd)) == -1L) && (patchcheckflag == 1)){
				fs_bootcheck(&bootcheck);
				//Autopatch success
				if (bootcheck) {
					_findclose(handle);
					patchcheckflag = 0;
					return 0;
				}
				else {
					//patch fail, timeout
					if (running_time_sec > 300) {
						patchcheckflag = 0;
						return 3;
					}
				}
            } else {
                //Unkown patch fail, timeout
                if (running_time_sec > 600){
                    patchcheckflag = 0;
                    return 2;
                }
            }
			Sleep(5000);
        }
    }
	*/
}