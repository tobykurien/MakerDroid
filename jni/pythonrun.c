#define PY_SSIZE_T_CLEAN
#include "Python.h"
#ifndef Py_PYTHON_H
    #error Python headers needed to compile C extensions, please install development version of Python.
#else

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "android/log.h"

#include <jni.h>

/* JNI-C wrapper stuff */

#ifdef __cplusplus
#define C_LINKAGE "C"
#else
#define C_LINKAGE
#endif

#define LOG(x) __android_log_write(ANDROID_LOG_INFO, "python", (x))



extern C_LINKAGE void
Java_za_co_house4hack_paint3d_gcode_PythonRunner_nativeSetEnv ( JNIEnv*  env, jobject thiz, jstring j_name, jstring j_value )
{
    jboolean iscopy;
    const char *name = (*env)->GetStringUTFChars(env, j_name, &iscopy);
    const char *value = (*env)->GetStringUTFChars(env, j_value, &iscopy);
    setenv(name, value, 1);
    (*env)->ReleaseStringUTFChars(env, j_name, name);
    (*env)->ReleaseStringUTFChars(env, j_value, value);
}

extern C_LINKAGE void
Java_za_co_house4hack_paint3d_gcode_PythonRunner_runPython ( JNIEnv*  env, jobject thiz, jstring j_script )
{
    jboolean iscopy;
    const char *script = (*env)->GetStringUTFChars(env, j_script, &iscopy);
    run(script);    
    (*env)->ReleaseStringUTFChars(env, j_script, script);
}

static int isSdcardUsed = 0;

extern C_LINKAGE void
Java_za_co_house4hack_paint3d_gcode_PythonRunner_nativeIsSdcardUsed ( JNIEnv*  env, jobject thiz, jint flag )
{
	isSdcardUsed = flag;
}

#undef C_LINKAGE



static PyObject *androidembed_log(PyObject *self, PyObject *args) {
    char *logstr = NULL;
    if (!PyArg_ParseTuple(args, "s", &logstr)) {
        return NULL;
    }
    LOG(logstr);
    Py_RETURN_NONE;
}

static PyMethodDef AndroidEmbedMethods[] = {
    {"log", androidembed_log, METH_VARARGS,
     "Log on android platform"},
    {NULL, NULL, 0, NULL}
};

PyMODINIT_FUNC initandroidembed(void) {
    (void) Py_InitModule("androidembed", AndroidEmbedMethods);
}

int file_exists(const char * filename)
{
	FILE *file;
    if (file = fopen(filename, "r")) {
        fclose(file);
        return 1;
    }
    return 0;
}

int run(char *script) {
    int ret = 0;
    char *env_argument = NULL;


    LOG("Initialize Python for Android in runPython");

	//setenv("PYTHONVERBOSE", "2", 1);
    Py_SetProgramName("PythonRunner");
    Py_Initialize();
	int argc = 0;
	char * argv[] = { };
    PySys_SetArgv(argc, argv);

    /* ensure threads will work.
     */
    PyEval_InitThreads();

    /* our logging module for android
     */
    initandroidembed();

    /* inject our bootstrap code to redirect python stdin/stdout
     * replace sys.path with our path
     */


    ret = PyRun_SimpleString(script);

    if (PyErr_Occurred() != NULL) {
        ret = 1;
        PyErr_Print(); /* This exits with the right code if SystemExit. */
        if (Py_FlushLine())
			PyErr_Clear();
    }

    /* close everything
     */
	Py_Finalize();

    LOG("Python for android ended.");


}


#endif
