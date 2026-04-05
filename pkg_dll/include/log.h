#ifndef __LOG_H__
#define __LOG_H__

#ifdef __cplusplus
extern "C" {
#endif
int mylog(const char* file, int line, const char* fmt, ...);
#ifdef __cplusplus
}
#endif 
#define MYLOG(...)  //mylog(__FILE__, __LINE__, __VA_ARGS__) // Log 비활성화
#define TMLOG(...)  mylog(__FILE__, __LINE__, __VA_ARGS__) // Log 비활성화

#endif 
