#include "stdafx.h"
#include "pcdef.h"
#include "amc.h"
#include "FSystem.h"



INT fs_get_files(INT *pNum)
{
	FILECMD sFileCmd;
	FILECMD_ACK sFileCmdAck;
	UINT uiSize;
	INT err;

	memset(&sFileCmd, 0, sizeof(sFileCmd));

	sFileCmd.usCmd16 = FSCMD_FILENUM;
//	sFileCmd.uiCrc32 = _crc32_get_crc(&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);
	sFileCmd.uiCrc32 = _crc32_get_crc((char *)&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);	//2.8.5, 2011.12.22 syk int aa= int2 bb;
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0, &sFileCmd, sizeof(sFileCmd));
		_flush_dpram(FSCMD);

		uiSize = 0;
		if (_wait_for_reply(3000) == AMC_SUCCESS) 
		{
			mmc_error = err = MMC_OK;

			uiSize = sizeof(sFileCmdAck);
			memset(&sFileCmdAck, 0, uiSize);
			_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sFileCmdAck, &uiSize);

			uiSize = sFileCmdAck.uiTotLen;
		} else mmc_error = err = MMC_TIMEOUT_ERR;

//		*pNum = uiSize;		 //s:int* = uint
		*pNum = (int)uiSize; //2.8.5, 2011.12.22 syk int aa= int2 bb;

		MMCMutexUnlock();//2.8.05, 2011.10.22 리턴값이 항상 MMCOK라서 if문 필요 없음
		//if (MMCMutexUnlock() != MMC_OK) mmc_error = err = MMC_TIMEOUT_ERR;
	}
	return err;
}

INT fs_get_file_name(int ofs, char *pszName)
{
	FILECMD sFileCmd;
	FILECMD_ACK sFileCmdAck;
	UINT uiSize;
	INT err;

	memset(&sFileCmd, 0, sizeof(sFileCmd));

	sFileCmd.usCmd16 = FSCMD_FILENAME;
	sFileCmd.uiIndex = ofs;
//	sFileCmd.uiCrc32 = _crc32_get_crc(&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);
	sFileCmd.uiCrc32 = _crc32_get_crc((char *)&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);	//2.8.5, 2011.12.22 syk int aa= int2 bb;	
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0, &sFileCmd, sizeof(sFileCmd));
		_flush_dpram(FSCMD);

		if (pszName) strcpy(pszName, "");

		if (_wait_for_reply(3000) == AMC_SUCCESS) 
		{
			mmc_error = err = MMC_OK;

			uiSize = sizeof(sFileCmdAck);
			memset(&sFileCmdAck, 0, uiSize);
			_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sFileCmdAck, &uiSize);

			if (pszName) strcpy(pszName, sFileCmdAck.uszCmd);
		} else mmc_error = err = MMC_TIMEOUT_ERR;

		MMCMutexUnlock();//2.8.05, 2011.10.22 리턴값이 항상 MMCOK라서 if문 필요 없음
		//if (MMCMutexUnlock() != MMC_OK) mmc_error = err = MMC_TIMEOUT_ERR;
	}
	return err;
}

INT fs_get_file_size(int ofs, UINT *puiSize)
{
	FILECMD sFileCmd;
	FILECMD_ACK sFileCmdAck;
	UINT uiSize;
	INT err;

	memset(&sFileCmd, 0, sizeof(sFileCmd));

	sFileCmd.usCmd16 = FSCMD_FILESIZE;
	sFileCmd.uiIndex = ofs;
//	sFileCmd.uiCrc32 = _crc32_get_crc(&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);
	sFileCmd.uiCrc32 = _crc32_get_crc((char *)&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);	//2.8.5, 2011.12.22 syk int aa= int2 bb;
	
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0, &sFileCmd, sizeof(sFileCmd));
		_flush_dpram(FSCMD);

		uiSize = 0;

		if (_wait_for_reply(3000) == AMC_SUCCESS) 
		{
			mmc_error = err = MMC_OK;

			uiSize = sizeof(sFileCmdAck);
			memset(&sFileCmdAck, 0, uiSize);
			_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR, &sFileCmdAck, &uiSize);

			uiSize = sFileCmdAck.uiTotLen;
		} else mmc_error = err = MMC_TIMEOUT_ERR;

		*puiSize = uiSize;

		MMCMutexUnlock();//2.8.05, 2011.10.22 리턴값이 항상 MMCOK라서 if문 필요 없음
		//if (MMCMutexUnlock() != MMC_OK) mmc_error = err = MMC_TIMEOUT_ERR;
	}
	return err;
}

INT fs_format()
{
	FILECMD sFileCmd;
	FILECMD_ACK sFileCmdAck;
	UINT uiSize;
	INT err;

	memset(&sFileCmd, 0, sizeof(sFileCmd));

	sFileCmd.usCmd16 = FSCMD_FS_FORMAT;
//	sFileCmd.uiCrc32 = _crc32_get_crc(&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);
	sFileCmd.uiCrc32 = _crc32_get_crc((char *)&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);	//2.8.5, 2011.12.22 syk int aa= int2 bb;
	
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0, &sFileCmd, sizeof(sFileCmd));
		_flush_dpram(FSCMD);

		uiSize = 0;

		if (_wait_for_reply(3000) == AMC_SUCCESS) 
		{
			mmc_error = err = MMC_OK;
		} else mmc_error = err = MMC_TIMEOUT_ERR;

		*puiSize = uiSize;

		MMCMutexUnlock();//2.8.05, 2011.10.22 리턴값이 항상 MMCOK라서 if문 필요 없음
		//if (MMCMutexUnlock() != MMC_OK) mmc_error = err = MMC_TIMEOUT_ERR;
	}
	return err;
}

INT fs_file_delete(char *pszDelFileName)
{
	FILECMD sFileCmd;
	FILECMD_ACK sFileCmdAck;
	UINT uiSize;
	INT err;

	memset(&sFileCmd, 0, sizeof(sFileCmd));

	sFileCmd.usCmd16 = FSCMD_DEL_FILE;
	strcpy(sFileCmd.uszFileName, pszDelFileName);
//	sFileCmd.uiCrc32 = _crc32_get_crc(&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);
	sFileCmd.uiCrc32 = _crc32_get_crc((char *)&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);	//2.8.5, 2011.12.22 syk int aa= int2 bb;
	
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0, &sFileCmd, sizeof(sFileCmd));
		_flush_dpram(FSCMD);

		if (_wait_for_reply(3000) == AMC_SUCCESS) 
		{
			mmc_error = err = MMC_OK;
		} else mmc_error = err = MMC_TIMEOUT_ERR;

		MMCMutexUnlock();//2.8.05, 2011.10.22 리턴값이 항상 MMCOK라서 if문 필요 없음
		//		if (MMCMutexUnlock() != MMC_OK) mmc_error = err = MMC_TIMEOUT_ERR;
	}
	return err;
}

INT fs_file_upload(char *pszLoadFileName, char *pszSaveFileName)
{
	FILECMD sFileCmd;
	FILECMD_ACK sFileCmdAck;
	UINT uiSize;
	UCHAR *pBody = NULL;
	int blklen, i;
	FILE *fp;
	INT err;

	memset(&sFileCmd, 0, sizeof(sFileCmd));

	sFileCmd.usCmd16 = FSCMD_FILE_UPLOAD;
	strcpy(sFileCmd.uszFileName, pszLoadFileName);
//	sFileCmd.uiCrc32 = _crc32_get_crc(&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);
	sFileCmd.uiCrc32 = _crc32_get_crc((char *)&sFileCmd.uiTotLen, sizeof(sFileCmd) - 4);	//2.8.5, 2011.12.22 syk int aa= int2 bb;
	
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0, &sFileCmd, sizeof(sFileCmd));
		_flush_dpram(FSCMD);

		if (_wait_for_reply(3000) == AMC_SUCCESS) 
		{
			mmc_error = err = MMC_OK;
		}
		else							// 2012.3.16 2.9.2 syk , DSP TIMEOUT ERROR발생시 바로 리턴
		{
			MMCMutexUnlock();
			mmc_error = err = MMC_TIMEOUT_ERR;
			return err;
		}

		MMCMutexUnlock();//2.8.05, 2011.10.22 리턴값이 항상 MMCOK라서 if문 필요 없음
		//if (MMCMutexUnlock() != MMC_OK) mmc_error = err = MMC_TIMEOUT_ERR;
		//else {
		//올라올 파일의 바이트수
		uiSize = sFileCmdAck.uiTotLen;
		blklen = uiSize / 256;
		if (blklen * 256 != uiSize) blklen ++;

		// 몸체를 저장할 공간을 할당한다.
		pBody = (UCHAR *) malloc(uiSize);

		// 몸체를 가져온다.
		for (i = 0; i < blklen; i ++)
		{
			sFileCmd.uiTotLen = uiSize;
			sFileCmd.usBlkID = i;

			if (MMCMutexLock() != MMC_OK)	//2.8.05, 2011.10.22 에러 코드 리턴토록 수정 
			{
				//mmc_error = err = MMC_TIMEOUT_ERR;
				mmc_error = err = MMC_MUTEXLOCK_ERROR; // 2012.3.16 2.9.2 syk , DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 
				break;
			}

			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0, &sFileCmd, sizeof(sFileCmd));
			_flush_dpram(FSCMD);

			if (_wait_for_reply(3000) == AMC_SUCCESS)
			{
				_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 8, &nValidBytes, 4);

				// 데이터 몸체를 가져온다.
				_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 16, &pBody + i * 256, nValidBytes);
			}
			else							// 2012.3.16 2.9.2 syk , DSP TIMEOUT ERROR발생시 바로 리턴
			{
				MMCMutexUnlock();
				mmc_error = err = MMC_TIMEOUT_ERR;
				return err;
			}
	
			MMCMutexUnlock();//2.8.05, 2011.10.22 리턴값이 항상 MMCOK라서 if문 필요 없음
//			if (MMCMutexUnlock() != MMC_OK) break;
		}

		fp = fopen(pszSaveFileName, "wb");
		if (fp) 
		{
			fwrite(pBody, uiSize, 1, fp);
			fclose(fp);
			free(pBody);
		}
		else 
		{
			free(pBody);
			mmc_error = FUNC_ERR;  // 2012.3.16 2.9.2 syk , DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR -> FUNC_ERR
			return FUNC_ERR;
		}
	}
	return err;
}

INT fs_file_dnload(char *pszDnloadFileName, BOOL bBootLoader)
{
	FILECMD sFileCmd;
	FILECMD_ACK sFileCmdAck;
	UINT uiSize;
	UCHAR *pBody = NULL;
	int blklen, i;
	FILE *fp = NULL;
	INT err;

	fp = fopen(pszDnloadFileName, "rb");
	if (fp == NULL) return FUNC_ERR;   //MMC_OK -> FUNC_ERR
	fseek(fp, 0L, SEEK_END);
	uiSize = ftell(fp);
	fseek(fp, 0L, SEEK_SET);
	
	pBody = (UCHAR *) malloc(uiSize);
	fread(pBody, uiSize, 1, fp);
	fclose(fp);

	memset(&sFileCmd, 0, sizeof(sFileCmd));

	sFileCmd.usCmd16 = (bBootLoader) ? FSCMD_BL_DNLOAD : FSCMD_FILE_DNLOAD;
	sFileCmd.uiTotLen = uiSize;
	strcpy(sFileCmd.uszFileName, pszDnloadFileName);
//	sFileCmd.uiCrc32 = _crc32_get_crc(pBody, uiSize);
	sFileCmd.uiCrc32 = _crc32_get_crc((char *)pBody, uiSize);	//2.8.5, 2011.12.22 syk int aa= int2 bb;
	
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0, &sFileCmd, sizeof(sFileCmd));
		_flush_dpram(FSCMD);

		if (_wait_for_reply(3000) == AMC_SUCCESS) 
		{
			mmc_error = err = MMC_OK;
		}
		else
		{
			MMCMutexUnlock();
			mmc_error = err = MMC_TIMEOUT_ERR;
			return err;
		}

		MMCMutexUnlock();//2.8.05, 2011.10.22 리턴값이 항상 MMCOK라서 if문 필요 없음
		//if (MMCMutexUnlock() != MMC_OK) mmc_error = err = MMC_TIMEOUT_ERR;
		//else {
		// 내려보낼 파일의 블럭수를 계산한다.
		blklen = uiSize / 256;
		if (blklen * 256 != uiSize) blklen ++;

		// 몸체를 저장할 공간을 할당한다.
		pBody = (UCHAR *) malloc(uiSize);

		// 몸체를 가져온다.
		for (i = 0; i < blklen; i ++)
		{
			sFileCmd.uiTotLen = uiSize;
			sFileCmd.usBlkID = i;

			if (MMCMutexLock() != MMC_OK)
			{
//				mmc_error = err = MMC_TIMEOUT_ERR;//2.8.05, 2011.10.22 에러 코드가 리턴되도록
				mmc_error = err = MMC_MUTEXLOCK_ERROR; // 2012.3.16 2.9.2 syk , DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 
				break;
			}

			nValidBytes = min(uiSize - i * 256, 256);
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0, &sFileCmd, sizeof(sFileCmd));
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 16, pBody + i * 256, nValidBytes);
			_flush_dpram(FSCMD);

			if (_wait_for_reply(3000) != AMC_SUCCESS) 
			{
				MMCMutexUnlock();
				//break;
				mmc_error = err = MMC_TIMEOUT_ERR;
				return err;
			}
			if (i == blklen - 1)
			{
				// 마지막 블럭을 보냈으므로 응답을 읽는다.
				_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 8, &nValidBytes, 4);
			}
			MMCMutexUnlock();//2.8.05, 2011.10.22 리턴값이 항상 MMCOK라서 if문 필요 없음
//			if (MMCMutexUnlock() != MMC_OK) break;
		}
		if (nValidBytes == 1) mmc_error = err = MMC_OK;
		else mmc_error = err = MMC_TIMEOUT_ERR;
	}
	return err;
}

