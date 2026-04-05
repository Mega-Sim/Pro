
COMM_DPRAM_TYPE	*CommDpram[MMC_BOARD_NUM];
AXIS_DPRAM_TYPE	*AxisDpram[TOTAL_AXIS_NUM];
BOOT_FRAME_TYPE	BootFrame[MMC_BOARD_NUM];
MOVE_FRAME_TYPE	Mf;
MOTION_DISP		Mdisp[TOTAL_AXIS_NUM];
SYNC_MOTION		SyncMotion;

unsigned LONG	Dpram_Addr[MMC_BOARD_NUM][BD_AXIS_NUM];
CHAR			*AxisInfo[MMC_BOARD_NUM];
CHAR   			*Ack2Dsp[MMC_BOARD_NUM];
CHAR   			*Int2Dsp[MMC_BOARD_NUM];
CHAR   			*DpramExistChk[MMC_BOARD_NUM][2];
INT				mmc_error;
INT				MMC_Bd_Num;
//INT	     		Int2Dsp_Flag[MMC_BOARD_NUM];
INT 			Active_Axis_Num;
//double			GearRatio[TOTAL_AXIS_NUM];
INT				Power_Flag[TOTAL_AXIS_NUM];
unsigned CHAR	ChkSum_Parity[MMC_BOARD_NUM];
LONG			Virtual_Pos[TOTAL_AXIS_NUM];
INT				Version_Info[MMC_BOARD_NUM];

CHAR		*Error_Msg[]={
/*	0	*/		"No Error",
/*	1	*/		"Boot Memory has been corrupted ",
/*	2	*/		"DPRAM Communication Error",
/*	3	*/		"Non Existent Axis",
/*	4	*/		"Illegal Analog Input Channel",
/*	5	*/		"Invalid I/O Port",
/*	6	*/		"Illegal Parameter",
/*	7	*/		"Not Define Map Axis",
/*	8	*/		"AMP Fault Occured",
/*	9 	*/		"Motion is not completed",
/*	10	*/		"MMC Board is not exist",
/*	11	*/		"MMC Boot File Read/Write Error",
/*	12	*/		"MMC Checksum File Read/Write Error",
/*	13	*/		"MMC Windows NT Driver Open Error"
};

	CRITICAL_SECTION	m_csLockAxisComm;
	HANDLE		hMutex_Axis_Comm;
	HANDLE		_hThread;
	HANDLE		hWinRT;		/* handle to WinRT driver */
	DWORD		iLength;	/* return length from Ioctl call */
	DWORD		_dwThreadId;
	DWORD		FreeAddr[MMC_BOARD_NUM];
	BOOL		fInit=0;
	BOOL		fLoadOk=0;

