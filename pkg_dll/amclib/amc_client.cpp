
#include <thread>
#include <chrono>
#include <string>
#include <cstdint>
#include <mutex>

#include "Poco/SingletonHolder.h"
#include "Poco/Net/SocketAddress.h"
#include "Poco/Net/StreamSocket.h"
#include "Poco/SingletonHolder.h"
#include "Poco/Timespan.h"
#include <winioctl.h>
#include "../driver/amc_driver.h"

#include "pcdef.h"
#include "amclib.h"
#include "log.h"

using namespace std::chrono_literals;
using std::string;
using std::this_thread::sleep_for;

using Poco::Net::SocketAddress;
using Poco::Net::StreamSocket;
using Poco::Net::ServerSocket;
using Poco::Net::Socket;
using Poco::Timespan;

#define PACKET_CAPTURE 1

extern "C"
{
    extern INT mmc_error;
    extern INT MMCMutexLock(void);
    extern INT MMCMutexUnlock(void);
};

void printBinary(void *data, int length, const char* title = nullptr, int offs = 0)
{
    return;
    if (title != nullptr) {
		printf(">>> %s <<< \n", title);
	}
	for (int i = 0; i < length; i += 16) {
		auto end = min(length, i + 16);
		printf("[%4d] ", i+offs);
		for (int j = i; j < end; j += 4) {
			auto end2 = min(length, j + 4);
			for (int k = j; k < end2; k += 1) {
				printf("%02x ", ((uint8_t*)data)[k]);
			}
			printf(" ");
		}
		printf("\n");
	}
	fflush(stdout);
}

struct MMCLock
{
    void lock() {
        MMCMutexLock();
    }
    void unlock() {
        MMCMutexUnlock();
    }
};

class AMCDriver {
	enum {
		DPRAM_SIZE = 1024,
		DSP_HANDLER_ADDR = 1022,
	};
public:
	AMCDriver(short portnum = 9000) :
        stop_{ false }, connected_{false}
	{
        GetCurrentDirectoryA(256, szCurDir_);
        strcat(szCurDir_, "\\ipconfig.ini");
        GetPrivateProfileString("IP_CONFIG", "MASTER_CONTROLLER_IP", "localhost", strName_, 100, szCurDir_);
        sa_ = {strName_, (Poco::UInt16) portnum};
        MYLOG("connect to = %s %s\n", strName_, szCurDir_);

        memset(dpram_, 0, DPRAM_SIZE);
		memset(empty_, 0, DPRAM_SIZE);

        connect();

        if (isConnected()) {
            // initial sync
            this->receive();
            //sync_requester_ = std::make_unique<std::thread>([this]() {this->sync(); });
        }
		reconnect_th_ = std::make_unique<std::thread>([this]() {this->reconnect(); });		
	}

	~AMCDriver()
	{
		stop_ = true;
		socket_.close();
		sync_requester_->join();
		sync_requester_.reset(nullptr);
	}	

    void connect()
    {
        if (connected_) return;

        MYLOG("try connect\n");
        try {
            socket_.connect(sa_);
            connected_ = true;
        }
        catch (std::exception& e) {
            MYLOG("connection failed : %s\n", e.what());
            connected_ = false;
        }
        MYLOG("connected = %s\n", connected_ ? "true" : "false");
    }

	int read(AMC_REGSRW_IRP*irp)
	{
        if (!connected_) return -1;
		std::lock_guard<MMCLock> guard(mutex_);
		irp->m_nData = dpram_[irp->m_nOffset];
        //printBinary(&irp->m_nData, 1, "Read", irp->m_nOffset);
		irp->m_bSuccess = 1;
		return 1;
	}

	int read(AMC_REGSRWGRP_IRP*irp)
	{
        if (!connected_) return -1;
		std::lock_guard<MMCLock> guard(mutex_);
        memcpy(irp->m_arrData, &dpram_[irp->m_nOffset], irp->m_nCount);
       // printBinary(irp->m_arrData, irp->m_nCount, "Read Grp", irp->m_nOffset);
		irp->m_bSuccess = 1;
		return 1;
	}
	int write(AMC_REGSRW_IRP* irp)
	{
        if (!connected_) return -1;
		std::lock_guard<MMCLock> guard(mutex_);
		auto old = dpram_[irp->m_nOffset];
		dpram_[irp->m_nOffset] = irp->m_nData;
		//printBinary(dpram_ + irp->m_nOffset, 1, "Write", irp->m_nOffset);
		if (send() != DPRAM_SIZE || receive() != DPRAM_SIZE) {
			dpram_[irp->m_nOffset] = old;
			irp->m_bSuccess = 0;
			return 0;
		}
		irp->m_bSuccess = 1;
		return 1;
	}
	int write(AMC_REGSRWGRP_IRP* irp)
	{
        if (!connected_) return -1;
		std::lock_guard<MMCLock> guard(mutex_);
		uint8_t old[DPRAM_SIZE];
		memcpy(old + irp->m_nOffset, dpram_ + irp->m_nOffset, irp->m_nCount);
		memcpy(dpram_ + irp->m_nOffset, irp->m_arrData, irp->m_nCount);
		//printBinary(dpram_ + irp->m_nOffset, irp->m_nCount, "Write Grp", irp->m_nOffset);
		if (send() != DPRAM_SIZE || receive() != DPRAM_SIZE) {
			memcpy(dpram_ + irp->m_nOffset, old + irp->m_nOffset, irp->m_nCount);
			irp->m_bSuccess = 0;
			return 0;
		}
		irp->m_bSuccess = 1;
		return 1;
	}

	uint8_t * addr() const { return (uint8_t*)dpram_; }

    bool isConnected() const { return connected_; }

private:
	void reconnect()
	{
		while (!stop_) {
			std::this_thread::sleep_for(1000ms); // try to reconnect each 1000ms
			if (!stop_ && !connected_) {
				socket_.close();
				MYLOG("Reconnect");
				mutex_.lock();
				connect();
				if(connected_) {
					// initial sync
					this->receive();
				}
				mutex_.unlock();
			}
		}
	}

	void sync() 
	{
		while (!stop_) {
			std::this_thread::sleep_for(25000us); // adjust cycle time
			mutex_.lock();
			dpram_[DSP_HANDLER_ADDR] = 0; // SYNC Request command : 0
			send();
            receive();
			mutex_.unlock();
		}
	}

	int receive() // receive dpram data
	{
		int receive_cnt = 0;
        if (!connected_) return -1;
		int received_len = 0;
        int remain= DPRAM_SIZE;
        int offset = 0;
        try {
            __try_again:
            //TMLOG("try received\n");
            while (received_len < DPRAM_SIZE) {
				int len = socket_.receiveBytes(dpram_ + received_len, DPRAM_SIZE - received_len);
				received_len += len;
				// Check DPRAM Receive
				if (receive_cnt++ > DPRAM_SIZE)
				{
					TMLOG("fail receive!\n");
					break;
				}
			}
            //TMLOG("Received: %d bytes\n", received_len);
            printBinary(dpram_, DPRAM_SIZE, "receive", 0);
#ifdef DEBUG_FRAME_COUNT

            if (isEmpty()) {				
				printf("Empty stream received\n");
				received_len = 0;
				goto __try_again;
			}
#endif // DEBUG_FRAME_COUNT
		}
		catch (...)
		{
			TMLOG("Fail to receive...\n");
			connected_ = false;		
			return -1;
		}

		return received_len;
	}

	int send() // send dpram data
	{
#ifdef DEBUG_FRAME_COUNT
		static uint8_t frame_counter{ 0 };
		dpram_[1008] = frame_counter++;
#endif //DEBUG_FRAME_COUNT
        //TMLOG("try send\n");
		int sent_len(0);
		try {
			sent_len = socket_.sendBytes(dpram_, DPRAM_SIZE);
		}
		catch (...) {
			TMLOG("Fail to send...\n");
			connected_ = false;
			return -1;
		}
		SetEvent(g_stAMCData.hRetEvent);
		//TMLOG("Sent: %d bytes, service No = %d\n", sent_len, dpram_[1022]);
        printBinary(dpram_, DPRAM_SIZE, "send", 0);
#ifdef DEBUG_FRAME_COUNT
        if (isEmpty()) {
			printf("Empty stream sent\n");
		}
#endif // DEBUG_FRAME_COUNT
		return sent_len;
	}

	bool isEmpty()
	{
		return 0 == memcmp(dpram_, empty_, DPRAM_SIZE);
	}

	Poco::Net::SocketAddress sa_;
	Poco::Net::StreamSocket socket_;
	std::unique_ptr<std::thread> sync_requester_;
	std::unique_ptr<std::thread> reconnect_th_;
    MMCLock mutex_;
	bool stop_;
    bool connected_;
    char szCurDir_[256] = { NULL, };
    char strName_[100] = { NULL, };
	uint8_t dpram_[DPRAM_SIZE];	
	uint8_t empty_[DPRAM_SIZE]; // empty data stream check¿ë 
};

Poco::SingletonHolder<AMCDriver> the_amc_driver;


BOOL myDeviceIoControl(
	_In_ HANDLE hDevice,
	_In_ DWORD dwIoControlCode,
	_In_reads_bytes_opt_(nInBufferSize) LPVOID lpInBuffer,
	_In_ DWORD nInBufferSize,
	_Out_writes_bytes_to_opt_(nOutBufferSize, *lpBytesReturned) LPVOID lpOutBuffer,
	_In_ DWORD nOutBufferSize,
	_Out_opt_ LPDWORD lpBytesReturned,
	_In_ DWORD lpOverlapped)
{
    static AMC_INITIALIZE_IRP initirp;	
	auto amc_driver = the_amc_driver.get();
	auto dpram = amc_driver->addr();

	switch (dwIoControlCode) {
	    case IOCTL_INIT_AMCDRV: {
            memcpy(&initirp, lpInBuffer, sizeof(initirp));		
		    initirp.m_DpramPhyAddr = (ULONG)dpram;
		    initirp.m_pDpramVirAddr = (char*)dpram;
		    memcpy(lpOutBuffer, &initirp, sizeof(initirp));
            *lpBytesReturned = sizeof(AMC_INITIALIZE_IRP);
		    break;
	    }

	    case IOCTL_READ_BUFREG: 
	    {		
		    AMC_REGSRW_IRP *irp = (AMC_REGSRW_IRP *)lpOutBuffer;
		    amc_driver->read(irp);
		    //MYLOG("READ REG:%d\n", irp->m_bSuccess);
		    *lpBytesReturned = sizeof(AMC_REGSRW_IRP);
		    break;
	    }
	    case IOCTL_READ_BUFREGS:
	    {
		    AMC_REGSRWGRP_IRP *irps = (AMC_REGSRWGRP_IRP *)lpOutBuffer;
		    amc_driver->read(irps);
		    //MYLOG("READ REG:%d\n", irps->m_bSuccess);
		    *lpBytesReturned = sizeof(AMC_REGSRWGRP_IRP);
		    break;
	    }

	    case IOCTL_WRITE_BUFREG:
	    {
		    AMC_REGSRW_IRP *irp = (AMC_REGSRW_IRP *)lpInBuffer;
		    amc_driver->write(irp);
		    //MYLOG("WRITE REG:%d\n", irp->m_bSuccess);
		    *lpBytesReturned = sizeof(AMC_REGSRW_IRP);
		    break;
	    }

	    case IOCTL_WRITE_BUFREGS:
	    {
		    AMC_REGSRWGRP_IRP *irps = (AMC_REGSRWGRP_IRP *)lpInBuffer;
		    amc_driver->write(irps);
		    //MYLOG("WRITE REGS:%d\n", irps->m_bSuccess);
		    *lpBytesReturned = sizeof(AMC_REGSRWGRP_IRP);
		    break;
	    }
	}

    if (!amc_driver->isConnected()) {
        //TMLOG("Master B/D is not connected!\n");
        return FALSE;
    }

	return TRUE;
}

BOOL checkComm()
{
	auto amc_driver = the_amc_driver.get();
	return amc_driver->isConnected();
}

