//
// Created by 李錚 on 2017/9/28.
//

#ifndef FFPLAYER_LOG_H
#define FFPLAYER_LOG_H
#include <android/log.h>
#define LOGD(FORMAT,...) __android_log_print(ANDROID_LOG_DEBUG,"VTech_SE0877",FORMAT,##__VA_ARGS__)
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR,"VTech_SE0877",FORMAT,##__VA_ARGS__)
#define LOGW(FORMAT,...) __android_log_print(ANDROID_LOG_WARN,"VTech_SE0877",FORMAT,##__VA_ARGS__)
#endif //FFPLAYER_LOG_H
