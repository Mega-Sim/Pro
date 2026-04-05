
#include <thread>
#include <chrono>
#include <string>
#include <cstdint>
#include <mutex>
#include <deque>
#include <map>

#include "Poco/SingletonHolder.h"
#include "Poco/Net/SocketAddress.h"
#include "Poco/Net/StreamSocket.h"
#include "Poco/SingletonHolder.h"
#include "Poco/Timespan.h"
#include <winioctl.h>
#include <windows.h>
#include "../driver/amc_driver.h"

#include "pcdef.h"
#include "amclib.h"
#include "log.h"
#include "simple_circular_queue.h"

//#define MYLOG_SER MYLOG
#define MYLOG_SER(...)

using namespace sephi;
using namespace std::chrono_literals;
using std::string;
using std::this_thread::sleep_for;

using Poco::Net::SocketAddress;
using Poco::Net::StreamSocket;
using Poco::Net::ServerSocket;
using Poco::Net::Socket;
using Poco::Timespan;

extern void printBinary(void *data, int length, const char* title = nullptr, int offs = 0);

/// thread간의 event방식의 동기화 클래스
class ThreadCondition
{
public:
    ThreadCondition() : exit_(false) {}
    bool wait(float wait_time_in_sec = -1.0) { return wait(int64_t(wait_time_in_sec*1e9)); }
    bool wait(int64_t wait_ns) {
        std::unique_lock<std::mutex> lock(cv_m);
        if (wait_ns < 0) {
            cv.wait(lock);
            return true;
        }
        else {
            return std::cv_status::timeout != cv.wait_for(lock, std::chrono::nanoseconds(wait_ns));
        }
    }
    void wakeup() {
        cv.notify_all();
    }

    bool timeToExit() { return exit_; }
    bool exit() {
        exit_ = true;
        cv.notify_all();
    }
private:
    std::condition_variable cv;
    std::mutex cv_m;
    bool exit_;
};


// static functions
class Connector {
    enum {
        DPRAM_SIZE = 1024,
        DSP_HANDLER_ADDR = 1022,
    };
public:
    Connector(uint16_t portnum, const TCHAR* comport) :
        stop_{ false }, connected_{ false }, preset_timeout_(-1), port_num_(portnum), com_id_(comport), open_count(0)
    {
        GetCurrentDirectoryA(256, szCurDir_);
        strcat(szCurDir_, "\\ipconfig.ini");
        GetPrivateProfileString("IP_CONFIG", "MASTER_CONTROLLER_IP", "localhost", strName_, 100, szCurDir_);
        sa_ = { strName_, (Poco::UInt16) portnum };
        MYLOG("serial com connect to = %s %s\n", strName_, szCurDir_);

        connect();

        if (isConnected()) {
            rcv_thread_ = std::make_unique<std::thread>([this]() {svc(); });
        }

    }

    ~Connector()
    {
        stop_ = true;
        socket_.close();
        if (rcv_thread_) rcv_thread_->join();
    }

    int getPortNum() const { return port_num_; }
    const char* getComId() const { return com_id_.c_str(); }


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
        MYLOG("seroal com connected = %s\n", connected_ ? "true" : "false");
    }

    bool isEmpty()
    {
        return 0 == queue_.size();
    }

    int write(const void* data, int datalen) // send dpram data
    {
        int sent_len = socket_.sendBytes(data, datalen);
        MYLOG_SER("try send %d\n", sent_len);
        return sent_len;
    }

    void clearBuffer()
    {
        std::lock_guard<std::mutex> guard(lock_);
        queue_.clear();
    }

    bool read(void *buf, const int size, int *read_size)
    {
        return  readTimeout((char*)buf, size, read_size, preset_timeout_);
    }

    bool readTimeout(char *buf, const int size, int *read_size, int64_t timeout_ms = -1, int end_of_line = 0xffff)
    {
        std::lock_guard<std::mutex> guard(lock_);
        auto timeout_ns = timeout_ms * 1000000;
        auto t0 = std::chrono::system_clock::now();
        int read_len(0);
        char c = 0;
        while (true) {
            while (queue_.size() > 0 && read_len < size) {
                queue_.pop(&c, 1);
                buf[read_len] = c;
                read_len += 1;
                if (c == end_of_line) break; // end of line
            }
            if (read_len == size || c == end_of_line || timeout_ns == 0) break;
            if (timeout_ns < 0) {
                cond_.wait();
            }
            else {
                auto wait_ns = timeout_ns - (std::chrono::system_clock::now() - t0).count();
                if ((wait_ns < 0) || !cond_.wait(wait_ns)) break;  // timeout
            }
        }
        *read_size = read_len;
        return (read_len != 0);
    }
    bool isConnected() const { return connected_; }

public:
    int open_count;
    int64_t preset_timeout_;
    void setTimeout(int64_t to) {
        preset_timeout_ = to;
    }

private:
    Poco::Net::SocketAddress sa_;
    Poco::Net::StreamSocket socket_;
    int port_num_;
    std::string com_id_;
    bool stop_;
    bool connected_;
    char szCurDir_[256] = { NULL, };
    char strName_[100] = { NULL, };
    std::unique_ptr<std::thread> rcv_thread_;
    std::mutex lock_;
    ThreadCondition cond_;
    SimpleQueue<1024, NullMutex> queue_;


    void onReceive(const char* data, size_t bytes_transferred)
    {
        MYLOG_SER("[%d] Received %d bytes\n", port_num_, bytes_transferred);
        if (queue_.remains() < bytes_transferred) {
            MYLOG_SER("queue buffer is small to write serial data! Data loss!!!");
        }

        queue_.push(data, bytes_transferred);
        cond_.wakeup();
    }

    void svc()
    {
        char data[1024];
        while (!stop_) {
            auto rcv_len = socket_.receiveBytes(data, sizeof(data));
            if (rcv_len <= 0) {
                MYLOG_SER("Socket Receive Error!");
                stop_ = true;
                break;
            }
            onReceive(data, rcv_len);
        }
    }
};


// comm port map 
static std::map<std::string, uint16_t> _port_map = {
    { "\\\\.\\COM5", 41015 }, // CNB0, node bcr
    { "\\\\.\\COM6", 41016 },// CNB1, trans bcr
    { "\\\\.\\COM11", 41014 },// CNB2, pio
    { "\\\\.\\COM14", 41013 },// CNB3, cid
};

static std::deque<Connector> _port_vector;

bool isComOverTCP(HANDLE hcom)
{
    for (auto& port : _port_vector) {
        if (&port == hcom) return true;
    }
    return false;
}
#if 0
HINSTANCE hinstLib = nullptr;
typedef BOOL(*FnSetCommMask)(HANDLE h, DWORD mask);
typedef BOOL(*FnSetupComm)(HANDLE h, DWORD in, DWORD out);
typedef BOOL(*FnPurgeComm)(HANDLE h, DWORD arg);
typedef BOOL(*FnSetCommTimeouts)(HANDLE h, LPCOMMTIMEOUTS tout);
typedef BOOL(*FnGetCommState)(HANDLE h, LPDCB st);
typedef BOOL(*FnSetCommState)(HANDLE h, LPDCB st);
typedef BOOL(*FnCloseHandle)(HANDLE h);
typedef BOOL(*FnReadFile)(HANDLE h, PVOID data, DWORD len, PDWORD outlen, LPOVERLAPPED flag);
typedef BOOL(*FnWriteFile)(HANDLE h, LPCVOID data, DWORD len, LPDWORD outlen, LPOVERLAPPED);
FnSetCommMask _SetCommMask{ nullptr };
FnSetupComm _SetupComm{ nullptr };
FnPurgeComm _PurgeComm{ nullptr };
FnSetCommTimeouts _SetCommTimeouts{ nullptr };
FnGetCommState _GetCommState{ nullptr };
FnSetCommState _SetCommState{ nullptr };
FnCloseHandle _CloseHandle{ nullptr };
FnReadFile _ReadFile{ nullptr };
FnWriteFile _WriteFile{ nullptr };

struct InitSysDll {
    InitSysDll() {
        MYLOG_SER(__FUNCDNAME__"\n");

        if (hinstLib == nullptr) {
            hinstLib = LoadLibraryA("kernel32.dll");
        }
        _SetCommMask = (FnSetCommMask)GetProcAddress(hinstLib, "SetCommMask");
        _SetupComm = (FnSetupComm)GetProcAddress(hinstLib, "SetupComm");
        _PurgeComm = (FnPurgeComm)GetProcAddress(hinstLib, "PurgeComm");
        _SetCommTimeouts = (FnSetCommTimeouts)GetProcAddress(hinstLib, "SetCommTimeouts");
        _GetCommState = (FnGetCommState)GetProcAddress(hinstLib, "GetCommState");
        _SetCommState = (FnSetCommState)GetProcAddress(hinstLib, "SetCommState");
        _CloseHandle = (FnCloseHandle)GetProcAddress(hinstLib, "CloseHandle");
        _ReadFile = (FnReadFile)GetProcAddress(hinstLib, "ReadFile");
        _WriteFile = (FnWriteFile)GetProcAddress(hinstLib, "WriteFile");

        MYLOG_SER("_SetCommMask = %p\n", _SetCommMask);
    }
    ~InitSysDll() {
        MYLOG_SER(__FUNCDNAME__"\n");
        if (hinstLib) {
            FreeLibrary(hinstLib);
            hinstLib = nullptr;
        }
    }
};

static InitSysDll __dll__;
#endif 



static std::mutex port_lock;
HANDLE MakeCommOverTCP(TCHAR *port_name)
{
    std::lock_guard<std::mutex> guard(port_lock);


    auto portid = _port_map[port_name];
    if (portid == 0) {
        MYLOG("Not reserved port %s\n", port_name);
        SetLastError(0xFFFFFFFF);
        return nullptr;
    }

    MYLOG(__FUNCDNAME__" %s, portnum=%d\n", port_name, int(portid));
    // check duplicate open
    for (auto& con : _port_vector) {
        if (con.getPortNum() == portid) {
            HANDLE ret = (HANDLE)&con;
            MYLOG("MakeCommOverTCP reopen %s handle = %p\n", port_name, ret);
            con.open_count++;
            return ret;
        }
    }

    // create connector
    _port_vector.emplace_back(portid, port_name);
    if (!_port_vector.back().isConnected()) {
        _port_vector.pop_back();
        SetLastError(0xFFFFFFFE);
        return nullptr;
    }

    HANDLE ret = (HANDLE)&_port_vector.back();
    MYLOG("MakeCommOverTCP %s handle = %p\n", port_name, ret);
    _port_vector.back().open_count = 1;
    return ret;
}

BOOL CloseCommOverTCP(HANDLE h)
{
    std::lock_guard<std::mutex> guard(port_lock);
    MYLOG(__FUNCDNAME__" %p\n", h);
    if (h == (HANDLE)0xFFFFFFFF) return TRUE;
    if (!isComOverTCP(h)) return CloseHandle(h);
    auto& con = *(Connector*)h;
    con.open_count--;
    MYLOG("CloseCommOverTCP %s handle = %p, use_count=%d\n", con.getComId(), &con, con.open_count);
    return TRUE;
}


BOOL _SetCommMask(HANDLE h, DWORD mask)
{
    MYLOG_SER(__FUNCDNAME__"\n");
    if (!isComOverTCP(h)) return SetCommMask(h, mask);
    return TRUE;
}

BOOL _SetupComm(HANDLE h, DWORD in, DWORD out)
{
    MYLOG_SER(__FUNCDNAME__"\n");
    if (!isComOverTCP(h)) return SetupComm(h, in, out);
    return TRUE;
}

BOOL _PurgeComm(HANDLE h, DWORD arg)
{
    MYLOG_SER(__FUNCDNAME__"\n");
    if (!isComOverTCP(h)) return PurgeComm(h, arg);
    auto& con = *(Connector*)h;
    con.clearBuffer();
    return TRUE;
}

BOOL _SetCommTimeouts(HANDLE h, LPCOMMTIMEOUTS tout)
{
    MYLOG_SER(__FUNCDNAME__"\n");
    if (!isComOverTCP(h)) return SetCommTimeouts(h, tout);
    auto& con = *(Connector*)h;
    con.setTimeout(tout->ReadTotalTimeoutConstant*tout->ReadTotalTimeoutMultiplier);
    return TRUE;
}

BOOL _GetCommState(HANDLE h, LPDCB st)
{
    MYLOG_SER(__FUNCDNAME__"\n");
    if (!isComOverTCP(h)) return GetCommState(h, st);
    return TRUE;
}

BOOL _SetCommState(HANDLE h, LPDCB st)
{
    MYLOG_SER(__FUNCDNAME__"\n");
    if (!isComOverTCP(h)) return SetCommState(h, st);
    return TRUE;
}



BOOL _ReadFile(HANDLE h, PVOID data, DWORD len, PDWORD outlen, LPOVERLAPPED flag)
{
    if (!isComOverTCP(h)) return ReadFile(h, data, len, outlen, flag);
    auto& con = *(Connector*)h;

    int readlen;
    if (con.read(data, len, &readlen)) {
        if (outlen) *outlen = readlen;
        MYLOG_SER("_ReadFile [%d] %d bytes\n", con.getPortNum(), readlen);
        return TRUE;
    }
    MYLOG_SER("_ReadFile [%d] fail\n", con.getPortNum());
    return FALSE;
}

BOOL _WriteFile(HANDLE h, LPCVOID data, DWORD len, LPDWORD outlen, LPOVERLAPPED flag)
{
    if (!isComOverTCP(h)) return WriteFile(h, data, len, outlen, flag);
    auto& con = *(Connector*)h;
    MYLOG_SER("_WriteFile [%d]\n", con.getPortNum());
    DWORD writelen = con.write(data, len);
    if (writelen > 0) {
        if (outlen) *outlen = writelen;
        return TRUE;
    }
    return FALSE;
}

