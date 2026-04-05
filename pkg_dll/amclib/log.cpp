#include <stdio.h>
#include <stdarg.h>
#include <mutex>
#include <thread>
#include <chrono>

extern "C" {
	class MyLog
	{
		FILE *fp;

	public:
		MyLog(const char* logfilename) : fp(NULL) {
			fp = fopen(logfilename, "a+");   //CIS
		}
		~MyLog() {
			if (fp != NULL) fclose(fp);
		}
		friend int mylog(const char* file, int line, const char* fmt, ...);
	};
	
	int mylog(const char* file, int line, const char* fmt, ...)
	{
		using time_point_ns = std::chrono::time_point<std::chrono::system_clock, std::chrono::nanoseconds>;
		static time_point_ns timeoffset = std::chrono::system_clock::now();
		static MyLog ins("amclog.log");
		static std::mutex m;
		std::lock_guard<std::mutex> guard(m);

		auto tid = std::this_thread::get_id();
		auto elapsed = std::chrono::system_clock::now() - timeoffset;
		int sec = elapsed.count() / 1000000000L;
		int msec = elapsed.count() % 1000000000L / 1000000;
		int usec = elapsed.count() % 1000000000L / 1000 - msec * 1000;
		 
		fprintf(ins.fp, "%5d.%03d;", sec, msec);
		va_list args;
		va_start(args, fmt);
		int ret = vfprintf(ins.fp, fmt, args);
		fflush(ins.fp);
		return ret;
	}

}
