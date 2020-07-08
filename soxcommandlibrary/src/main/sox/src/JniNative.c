//
// Created by SE0877 on 2020/7/3.
//
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include "sox.h"
#include "Log.h"
#include <assert.h>
static sox_format_t * in, * out; /* input and output files */

/* The function that will be called to input samples into the effects chain.
 * In this example, we get samples to process from a SoX-openned audio file.
 * In a different application, they might be generated or come from a different
 * part of the application. */
static int input_drain(
        sox_effect_t * effp, sox_sample_t * obuf, size_t * osamp)
{
    (void)effp;   /* This parameter is not needed in this example */

    /* ensure that *osamp is a multiple of the number of channels. */
    *osamp -= *osamp % effp->out_signal.channels;

    /* Read up to *osamp samples into obuf; store the actual number read
     * back to *osamp */
    *osamp = sox_read(in, obuf, *osamp);

    /* sox_read may return a number that is less than was requested; only if
     * 0 samples is returned does it indicate that end-of-file has been reached
     * or an error has occurred */
    if (!*osamp && in->sox_errno)
        fprintf(stderr, "%s: %s\n", in->filename, in->sox_errstr);
    return *osamp? SOX_SUCCESS : SOX_EOF;
}

/* The function that will be called to output samples from the effects chain.
 * In this example, we store the samples in a SoX-opened audio file.
 * In a different application, they might perhaps be analysed in some way,
 * or displayed as a wave-form */
static int output_flow(sox_effect_t *effp LSX_UNUSED, sox_sample_t const * ibuf,
                       sox_sample_t * obuf LSX_UNUSED, size_t * isamp, size_t * osamp)
{
    /* Write out *isamp samples */
    size_t len = sox_write(out, ibuf, *isamp);
    //printf("len is: %d",len);

    /* len is the number of samples that were actually written out; if this is
     * different to *isamp, then something has gone wrong--most often, it's
     * out of disc space */
    if (len != *isamp) {
        fprintf(stderr, "%s: %s\n", out->filename, out->sox_errstr);
        return SOX_EOF;
    }

    /* Outputting is the last `effect' in the effect chain so always passes
     * 0 samples on to the next effect (as there isn't one!) */
    *osamp = 0;

    (void)effp;   /* This parameter is not needed in this example */

    return SOX_SUCCESS; /* All samples output successfully */
}



/* A `stub' effect handler to handle inputting samples to the effects
 * chain; the only function needed for this example is `drain' */
static sox_effect_handler_t const * input_handler(void)
{
    static sox_effect_handler_t handler = {
            "input", NULL, SOX_EFF_MCHAN|
                           SOX_EFF_MODIFY, NULL, NULL, NULL, input_drain, NULL, NULL, 0
    };
    return &handler;
}

/* A `stub' effect handler to handle outputting samples from the effects
 * chain; the only function needed for this example is `flow' */
static sox_effect_handler_t const * output_handler(void)
{
    static sox_effect_handler_t handler = {
            "output", NULL, SOX_EFF_MCHAN |
                            SOX_EFF_MODIFY | SOX_EFF_LENGTH, NULL, NULL, output_flow, NULL, NULL, NULL, 0
    };
    return &handler;
}
char* jstringTostring(JNIEnv* env, jstring jstr)
{
    char* rtn = NULL;
    jclass clsstring = (*env)->FindClass(env,"java/lang/String");
    jstring strencode = (*env)->NewStringUTF(env,"utf-8");
    jmethodID mid = (*env)->GetMethodID(env,clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr= (jbyteArray)(*env)->CallObjectMethod(env,jstr, mid, strencode);
    jsize alen = (*env)->GetArrayLength(env,barr);
    jbyte* ba = (*env)->GetByteArrayElements(env,barr, JNI_FALSE);
    if (alen > 0)
    {
        rtn = (char*)malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    (*env)->ReleaseByteArrayElements(env,barr, ba, 0);
    return rtn;
}

typedef struct {
    char **str;     //the PChar of string array
    size_t num;     //the number of string
}IString;


/** \Split string by a char
 *
 * \param  src:the string that you want to split
 * \param  delim:split string by this char
 * \param  istr:a srtuct to save string-array's PChar and string's amount.
 * \return  whether or not to split string successfully
 *
 */
int Split(char *src, char *delim, IString* istr)//split buf
{
    int i;
    char *str = NULL, *p = NULL;

    (*istr).num = 1;
    str = (char*)calloc(strlen(src)+1,sizeof(char));
    if (str == NULL) return 0;
    (*istr).str = (char**)calloc(1,sizeof(char *));
    if ((*istr).str == NULL) return 0;
    strcpy(str,src);

    p = strtok(str, delim);
    (*istr).str[0] = (char*)calloc(strlen(p)+1,sizeof(char));
    if ((*istr).str[0] == NULL) return 0;
    strcpy((*istr).str[0],p);
    for(i=1; p = strtok(NULL, delim); i++)
    {
        (*istr).num++;
        (*istr).str = (char**)realloc((*istr).str,(i+1)*sizeof(char *));
        if ((*istr).str == NULL) return 0;
        (*istr).str[i] = (char*)calloc(strlen(p)+1,sizeof(char));
        if ((*istr).str[0] == NULL) return 0;
        strcpy((*istr).str[i],p);
    }
    free(str);
    str = p = NULL;

    return 1;
}
int ChangeVoiceRobot(char ** filePath){

    //初始化
    assert(SOX_SUCCESS == sox_init());

    //初始化输入文件
    assert(in = sox_open_read(filePath[0],NULL,NULL,NULL));

    //初始化输出文件
    assert(out = sox_open_write(filePath[1],&in->signal,&in->encoding,in->filetype,NULL,NULL));

    //构造效果器链
    sox_effects_chain_t* Chain;
    Chain = sox_create_effects_chain(&in->encoding , &out->encoding);

    //构建输入数据的效果器
    sox_effect_t* InputEffect;
    InputEffect = sox_create_effect(input_handler());

    //将效果器的效果参数配置到效果器和输入文件中
    char* Buf[10];
    //将效果器配置到效果器链中，并且将效果器free掉
    assert(SOX_SUCCESS == sox_add_effect(Chain,InputEffect,&in->signal,&out->signal));
    free(InputEffect);
    sox_effect_t* SoxEffect;

    SoxEffect = sox_create_effect(sox_find_effect("pitch"));
    Buf[0] = "660";
    assert(SOX_SUCCESS == sox_effect_options(SoxEffect, 1, Buf));
    assert(SOX_SUCCESS == sox_add_effect(Chain, SoxEffect, &in->signal, &out->signal));
    free(SoxEffect);

    SoxEffect = sox_create_effect(sox_find_effect("tempo"));
    Buf[0] = "1.19";
    assert(SOX_SUCCESS == sox_effect_options(SoxEffect, 1, Buf));
    assert(SOX_SUCCESS == sox_add_effect(Chain, SoxEffect, &in->signal, &in->signal));
    free(SoxEffect);

    SoxEffect = sox_create_effect(sox_find_effect("echo"));
    Buf[0] = "0.8";
    Buf[1] = "0.7";
    Buf[2] = "10";
    Buf[3] = "0.7";
    assert(SOX_SUCCESS == sox_effect_options(SoxEffect, 4, Buf));
    assert(SOX_SUCCESS == sox_add_effect(Chain, SoxEffect, &in->signal, &out->signal));
    free(SoxEffect);

    SoxEffect = sox_create_effect(sox_find_effect("echo"));
    Buf[0] = "0.9";
    Buf[1] = "0.7";
    Buf[2] = "12";
    Buf[3] = "0.7";
    assert(SOX_SUCCESS == sox_effect_options(SoxEffect, 4, Buf));
    assert(SOX_SUCCESS == sox_add_effect(Chain, SoxEffect, &in->signal, &out->signal));
    free(SoxEffect);

    SoxEffect = sox_create_effect(sox_find_effect("echo"));
    Buf[0] = "0.8";
    Buf[1] = "0.9";
    Buf[2] = "7";
    Buf[3] = "0.7";
    assert(SOX_SUCCESS == sox_effect_options(SoxEffect, 4, Buf));
    assert(SOX_SUCCESS == sox_add_effect(Chain, SoxEffect, &in->signal, &out->signal));
    free(SoxEffect);

    SoxEffect = sox_create_effect(sox_find_effect("echo"));
    Buf[0] = "0.8";
    Buf[1] = "0.88";
    Buf[2] = "6";
    Buf[3] = "0.4";
    assert(SOX_SUCCESS == sox_effect_options(SoxEffect, 4, Buf));
    assert(SOX_SUCCESS == sox_add_effect(Chain, SoxEffect, &in->signal, &out->signal));
    free(SoxEffect);

    SoxEffect = sox_create_effect(sox_find_effect("rate"));
    char* argrate = NULL;
    assert(SOX_SUCCESS == sox_effect_options(SoxEffect, 0, &argrate));
    assert(SOX_SUCCESS == sox_add_effect(Chain, SoxEffect, &in->signal, &out->signal));
    free(SoxEffect);

    SoxEffect = sox_create_effect(sox_find_effect("vol"));
    Buf[0] = "12dB";
    assert(SOX_SUCCESS == sox_effect_options(SoxEffect, 1, Buf));
    assert(SOX_SUCCESS == sox_add_effect(Chain, SoxEffect, &in->signal, &in->signal));
    free(SoxEffect);

    SoxEffect = sox_create_effect(sox_find_effect("bass"));
    Buf[0] = "15";
    assert(SOX_SUCCESS == sox_effect_options(SoxEffect, 1, Buf));
    assert(SOX_SUCCESS == sox_add_effect(Chain, SoxEffect, &in->signal, &in->signal));
    free(SoxEffect);

    //添加一个输出数据的效果器
    sox_effect_t*  OutputEffect;
    OutputEffect = sox_create_effect(output_handler());
    assert(SOX_SUCCESS == sox_add_effect(Chain,OutputEffect,&in->signal,&out->signal));
    free(OutputEffect);

    //运行效果器
    sox_flow_effects(Chain,NULL,NULL);

    //销毁，退出
    sox_delete_effects_chain(Chain);
    sox_close(out);
    sox_close(in);
    sox_quit();
    return 0;
}
JNIEXPORT jint JNICALL
Java_com_vtech_audio_helper_SoxCommandLib_ExcuateCommand(JNIEnv *env, jclass clazz, jstring cmd) {
    // TODO: implement ExcuateCommand()
    char * CMD=jstringTostring(env,cmd);
    int size=0;
    IString istr;
    Split(CMD," ",&istr);
    int result=mymain(istr.num,istr.str);//=ChangeVoiceRobot(args);
    free(CMD);
    return (jint)result;

}