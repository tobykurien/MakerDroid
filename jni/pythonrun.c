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
Java_za_co_house4hack_paint3d_gcode_SkeinforgeWrapper_nativeInit ( JNIEnv*  env, jobject thiz )
{
    LOG("in nativeInit >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
	int argc = 0;
	char * argv[] = { };
	main( argc, argv );
};



extern C_LINKAGE void
Java_za_co_house4hack_paint3d_gcode_SkeinforgeWrapper_nativeSetEnv ( JNIEnv*  env, jobject thiz, jstring j_name, jstring j_value )
{
     LOG("in nativesetenv >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    jboolean iscopy;
    const char *name = (*env)->GetStringUTFChars(env, j_name, &iscopy);
    const char *value = (*env)->GetStringUTFChars(env, j_value, &iscopy);
    setenv(name, value, 1);
    (*env)->ReleaseStringUTFChars(env, j_name, name);
    (*env)->ReleaseStringUTFChars(env, j_value, value);
}

extern C_LINKAGE void
Java_za_co_house4hack_paint3d_gcode_SkeinforgeWrapper_runPython ( JNIEnv*  env, jobject thiz, jstring j_script )
{
     LOG("in  runPython >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    jboolean iscopy;
    const char *script = (*env)->GetStringUTFChars(env, j_script, &iscopy);
    PyRun_SimpleString(script);

    (*env)->ReleaseStringUTFChars(env, j_script, script);
}

static int isSdcardUsed = 0;

extern C_LINKAGE void
Java_za_co_house4hack_paint3d_gcode_SkeinforgeWrapper_nativeIsSdcardUsed ( JNIEnv*  env, jobject thiz, jint flag )
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

int main(int argc, char **argv) {

    char *env_argument = NULL;
    int ret = 0;
    FILE *fd;

    LOG("Initialize Python for Android");
    env_argument = getenv("ANDROID_ARGUMENT");
LOG("env argument is:>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
LOG(env_argument);
    setenv("ANDROID_APP_PATH", env_argument, 1);
LOG("wow it is set!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

	//setenv("PYTHONVERBOSE", "2", 1);
    Py_SetProgramName(argv[0]);
    Py_Initialize();
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
    PyRun_SimpleString(
        "import sys, posix\n" \
        "private = posix.environ['ANDROID_PRIVATE']\n" \
        "argument = posix.environ['ANDROID_ARGUMENT']\n" \
        "sys.path[:] = [ \n" \
		"    private + '/lib/python27.zip', \n" \
		"    private + '/lib/python2.7/', \n" \
		"    private + '/lib/python2.7/lib-dynload/', \n" \
		"    private + '/lib/python2.7/site-packages/', \n" \
		"    argument ]\n" \
        "import androidembed\n" \
        "class LogFile(object):\n" \
        "    def __init__(self):\n" \
        "        self.buffer = ''\n" \
        "    def write(self, s):\n" \
        "        s = self.buffer + s\n" \
        "        lines = s.split(\"\\n\")\n" \
        "        for l in lines[:-1]:\n" \
        "            androidembed.log(l)\n" \
        "        self.buffer = lines[-1]\n" \
        "sys.stdout = sys.stderr = LogFile()\n" \
		"import site; print site.getsitepackages()\n"\
		"print 'Android path', sys.path\n" \
        "print 'Android kivy bootstrap done. __name__ is', __name__");

    /* run it !
     */
    LOG("Run user program, change dir and execute main.py");
    chdir(env_argument);

	/* search the initial main.py
	 */
	char *main_py = "main.pyo";
	if ( file_exists(main_py) == 0 ) {
		if ( file_exists("main.py") )
			main_py = "main.py";
		else
			main_py = NULL;
	}

	if ( main_py == NULL ) {
		LOG("No main.pyo / main.py found.");
		return -1;
	}

    fd = fopen(main_py, "r");
    if ( fd == NULL ) {
        LOG("Open the main.py(o) failed");
        return -1;
    }

    /* run python !
     */
    ret = PyRun_SimpleFile(fd, main_py);

    if (PyErr_Occurred() != NULL) {
        ret = 1;
        PyErr_Print(); /* This exits with the right code if SystemExit. */
        if (Py_FlushLine())
			PyErr_Clear();
    }

    /* close everything
     */
	Py_Finalize();
    fclose(fd);

    LOG("Python for android ended.");
    return ret;
}

#endif
