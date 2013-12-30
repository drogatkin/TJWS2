// servservice.cpp : Defines the entry point for the console application.
//
// 
// $Id: servservice.cpp,v 1.14 2011/07/05 20:14:55 dmitriy Exp $
#include "stdafx.h"
#include <stdio.h>
#include <tchar.h>
#include <wtypes.h>
#include <sys/stat.h>

#include <jni.h>
#ifdef _WIN32
#define PATH_SEPARATOR ';'
#else /* UNIX */
#define PATH_SEPARATOR ':'
#endif
#define JVM_DLL "jvm.dll"
#define JAVA_DLL "java.dll"
#define JRE_KEY	    "Software\\JavaSoft\\Java Runtime Environment"

#define SERVICENAME _T("TinyJavaWebServer")
#define MAX_CLASSPATH_LEN 1024
#define MAXPATHLEN 1024
#define SVC_STOP_TIMEOUT 10000  // 10 seconds
#define SVC_START_TIMEOUT 8000 // 8 seconds

#define NUP_SERVICE_NAME   "Mup"
#define TCPIP_SERVICE_NAME "Tcpip"
#define AFD_SERVICE_NAME    "Afd"

#define VERSION _T("1.1")

#define REG_ROOT _T("SOFTWARE\\Rogatkin\\TinyJavaWebServer")
#define REG_K_CURRVER _T("CurrentVersion")
#define REG_V_PATH _T("Path")
#define REG_V_CP _T("CP")
#define REG_V_JVM_ARGS _T("Args")
#define REG_V_ENTRY_POINT _T("MainClass")

#define DEFAULT_SRV_CLASS _T("Acme/Serve/Main")


void CALLBACK serviceMain(DWORD dwArgc, LPTSTR *lpszArgv);
void logServiceMessage(LPCTSTR, int);

void stop();
void run();
void cleanup();

BOOL createJVM();
void installService(LPCTSTR serviceName, LPCTSTR displayName, LPCTSTR serviceExe,
                    LPCTSTR dependencies, int currentDependenciesLen,
                    LPCTSTR homeDir, LPCTSTR classPath, LPCTSTR jvmArgs, LPCTSTR mainClass);
void unistallService(LPCTSTR serviceName);

BOOL fillCP();

jboolean GetApplicationHome(char *buf, jint bufsize);
static jboolean GetPublicJREHome(char *buf, jint bufsize);
static void usage();
static char *argsToLine(int argc, char* argv[]);

// global service variables
BOOL bStandAlone;
SC_HANDLE   		scm;
SERVICE_STATUS_HANDLE   serviceStatusHandle;
SERVICE_STATUS          serviceStatus;       
HANDLE                  threadHandle = NULL;
JavaVM *jvm = NULL;
JNIEnv *env;
jclass jserv_cls;
TCHAR *installDir = NULL;
TCHAR *customCP = NULL; // class path for servlets
TCHAR *jvmArgs = NULL; // extra JVM args
TCHAR *mainClass = NULL; // main class unless default

int main(int argc, char* argv[])
{
	SERVICE_TABLE_ENTRY dispatchTable[] = {
        {SERVICENAME, serviceMain },
        {NULL, NULL }
    };
	if(argc > 1) {
		if(_tcsicmp(_T("-run"), argv[1]) == 0) {
			bStandAlone = TRUE;
			fillCP();
			if (argc > 2) {
				if (*(argv[2]+strlen(argv[2])-1) == '\\')
					*(argv[2]+strlen(argv[2])-1) = 0;
				installDir = _strdup(argv[2]);
			} 				

			if (createJVM()) {
				run();
			}
			cleanup();
		} else if(_tcsicmp(_T("-install"), argv[1]) == 0 || _tcsicmp(_T("-installa"), argv[1]) == 0) {
			// TODO use strstr
			// if (strstr(argv[1], _T("-install")) == argv[1])
			if (argc < 3) {
				usage();
				return -1;
			}
			scm = OpenSCManager(NULL, NULL, SC_MANAGER_ALL_ACCESS);
			installService(argc<5?SERVICENAME:argv[4], argc<6?SERVICENAME:argv[5],
				argc<7?argv[0]:argv[6], NULL, 0, argv[2], argc<4?NULL:argv[3], argc<8?NULL:argsToLine(argc-7, argv+7),
				_tcsicmp(_T("-installa"), argv[1]) == 0?"rogatkin/app/Main":NULL);
			CloseServiceHandle(scm);
		} else if(_tcsicmp(_T("-uninstall"), argv[1]) == 0) {
			scm = OpenSCManager(NULL, NULL, SC_MANAGER_ALL_ACCESS);
			unistallService(argc>2?argv[2]:SERVICENAME);
			CloseServiceHandle(scm);
		} else if(_tcsicmp(_T("-help"), argv[1]) == 0)
			usage();
	} else {
		if (!StartServiceCtrlDispatcher(dispatchTable)) {
			logServiceMessage(_T("StartServiceCtrlDispatcher failed."), EVENTLOG_ERROR_TYPE);
		}
	}

	return 0;
}

void usage() {
		_tprintf(_T("usage servservice.exe -install[a] directory [servlet_cp [service_name [service_description [service_exe [jvm_param1... ]]]]]]|\n"
		"                      -uninstall [service_name]|\n"
		"                      -run directory|\n"
		"                      -help\n"
		));
}


jboolean
GetJREPath(char *path, jint pathsize)
{
    char javadll[MAXPATHLEN];
    struct stat s;
	
    if (GetApplicationHome(path, pathsize)) {
		// Is JRE co-located with the application?
		sprintf_s(javadll, sizeof javadll, "%s\\bin\\"JAVA_DLL, path);
		if (stat(javadll, &s) == 0) {
			goto found;
		}
		
		// Does this app ship a private JRE in <apphome>\jre directory? 
		sprintf_s(javadll, sizeof javadll, "%s\\jre\\bin\\" JAVA_DLL, path);
		if (stat(javadll, &s) == 0) {
			strcat_s(path, pathsize, "\\jre");
			goto found;
		}
    }
	
    // Look for a public JRE on this machine.
    if (GetPublicJREHome(path, pathsize)) {
		goto found;
    }
	
    return JNI_FALSE;
	
found:
    return JNI_TRUE;
}

jboolean
GetJVMPath(const char *jrepath, const char *jvmtype,
		   char *jvmpath, jint jvmpathsize)
{
    struct stat s;
    sprintf_s(jvmpath, jvmpathsize, "%s\\bin\\%s\\" JVM_DLL, jrepath, jvmtype);
    if (stat(jvmpath, &s) == 0) {
		return JNI_TRUE;
    } else {
		return JNI_FALSE;
    }
}

jboolean
GetApplicationHome(char *buf, jint bufsize)
{
    char *cp;
    GetModuleFileName(0, buf, bufsize);
    *strrchr(buf, '\\') = '\0'; /* remove .exe file name */
    if ((cp = strrchr(buf, '\\')) == 0) {
	/* This happens if the application is in a drive root, and
		* there is no bin directory. */
		buf[0] = '\0';
		return JNI_FALSE;
    }
    *cp = '\0';  /* remove the bin\ part */
    return JNI_TRUE;
}

static jboolean
GetStringFromRegistry(HKEY key, const char *name, char *buf, jint bufsize)
{
    DWORD type, size;
	
    if (RegQueryValueEx(key, name, 0, &type, 0, &size) == 0
		&& type == REG_SZ
		&& (size < (unsigned int)bufsize)) {
		if (RegQueryValueEx(key, name, 0, 0, (unsigned char*)buf, &size) == 0) {
			return JNI_TRUE;
		}
    }
    return JNI_FALSE;
}

static jboolean
GetPublicJREHome(char *buf, jint bufsize)
{
    HKEY key, subkey;
    char version[MAXPATHLEN];
	
    // Find the current version of the JRE
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, JRE_KEY, 0, KEY_READ, &key) != 0) {
		return JNI_FALSE;
    }
	
    if (!GetStringFromRegistry(key, "CurrentVersion",
		version, sizeof(version))) {
		RegCloseKey(key);
		return JNI_FALSE;
    }
	
    // Find directory where the current version is installed. 
    if (RegOpenKeyEx(key, version, 0, KEY_READ, &subkey) != 0) {
		RegCloseKey(key);
		return JNI_FALSE;
    }
	
    if (!GetStringFromRegistry(subkey, "JavaHome", buf, bufsize)) {
		RegCloseKey(key);
		RegCloseKey(subkey);
		return JNI_FALSE;
    }
	
	
    RegCloseKey(key);
    RegCloseKey(subkey);
    return JNI_TRUE;
}

void logServiceMessage(LPCTSTR lpcszMsg, int severity=EVENTLOG_INFORMATION_TYPE) {
    TCHAR    chMsg[256];
    HANDLE  evSrc;
    LPCTSTR  lpszStrings[2];
	
    DWORD lastErr = GetLastError();
	
    _stprintf_s(chMsg, sizeof chMsg, _T("Service error code: %d"), lastErr);
    lpszStrings[0] = chMsg;
    lpszStrings[1] = lpcszMsg;
	
    evSrc = RegisterEventSource(NULL, TEXT(SERVICENAME));
	
    if (evSrc != NULL) {
		ReportEvent(evSrc, severity, 0, 0, NULL, 2, 0,
			(const char**)lpszStrings, NULL);              
		
		(VOID) DeregisterEventSource(evSrc);
    }
	if (bStandAlone) {
		_ftprintf(stderr, lpcszMsg);
		_ftprintf(stderr, "\n");
	}
}

LPTSTR getErrorMsg(DWORD err, LPCTSTR serviceName) {
    LPTSTR msg = 0;
    TCHAR buf[256];
    switch (err) {
	case ERROR_ACCESS_DENIED:
		msg = _T("You are not logged in as the Administrator.");
		break;
	case ERROR_DUP_NAME:
	case ERROR_SERVICE_EXISTS:
		_stprintf_s(buf, sizeof buf, _T("Service has already been added. To remove, run: "
			"tjwss -uninstall [%s]."), serviceName);
		msg = _tcsdup(buf);
		break;
	case ERROR_SERVICE_DOES_NOT_EXIST:
		_stprintf_s(buf, sizeof buf, _T("Service has not been added. To add, run: "
			"tjwss -install [[%s] {service_description}]."), serviceName);
		msg = _tcsdup(buf);
		break;
	case ERROR_INVALID_NAME:
		msg = _T("The service's name is invalid.");
		break;
	case ERROR_INVALID_PARAMETER:
		msg = _T("One of the service's parameters is invalid.");
		break;
	case ERROR_SERVICE_MARKED_FOR_DELETE:
		msg = _T("The service marked for deletion.");
		break;
    }
    return msg;
}

BOOL sendStatusToSCMgr(DWORD dwCurrentState,
                       DWORD dwWin32ExitCode,
                       DWORD dwCheckPoint,
                       DWORD dwWaitHint) {
    BOOL result;
    if (dwCurrentState == SERVICE_START_PENDING)
		serviceStatus.dwControlsAccepted = 0;
    else
		serviceStatus.dwControlsAccepted = SERVICE_ACCEPT_STOP |
		SERVICE_ACCEPT_PAUSE_CONTINUE;
    serviceStatus.dwCurrentState = dwCurrentState;
    serviceStatus.dwWin32ExitCode = dwWin32ExitCode;
    serviceStatus.dwCheckPoint = dwCheckPoint;
    serviceStatus.dwWaitHint = dwWaitHint;
    if (!(result = SetServiceStatus(serviceStatusHandle, &serviceStatus))) {
        logServiceMessage(_T("SetServiceStatus"), EVENTLOG_ERROR_TYPE);
    }
    return result;
}

/* This method dispatches events from the service control manager. */
void CALLBACK serviceCtrl(DWORD dwCtrlCode) {
    DWORD dwState = SERVICE_RUNNING;
    DWORD dwThreadID;
	
    switch(dwCtrlCode) {
	case SERVICE_CONTROL_PAUSE:
		if (serviceStatus.dwCurrentState == SERVICE_RUNNING) {
			SuspendThread(threadHandle);
			dwState = SERVICE_PAUSED;
		}
		break;
	case SERVICE_CONTROL_CONTINUE:
		if (serviceStatus.dwCurrentState == SERVICE_PAUSED) {
			ResumeThread(threadHandle);
			dwState = SERVICE_RUNNING;
		}
		break;
	case SERVICE_CONTROL_STOP:
		//dwState = SERVICE_STOP_PENDING;
		sendStatusToSCMgr(SERVICE_STOP_PENDING, NO_ERROR, 1, SVC_STOP_TIMEOUT);
		// Try the shutdown
		CreateThread(NULL,0,
			(LPTHREAD_START_ROUTINE) stop,
			(LPVOID)NULL, 0, &dwThreadID);
		return;
	case SERVICE_CONTROL_INTERROGATE:
		break;
	default:
		break;
    }
    sendStatusToSCMgr(dwState, NO_ERROR, 0, 0);
}

/* This is called when the service control manager starts the service.
* The service stops when this method returns so there is a wait on an
* event at the end of this method.
*/
void CALLBACK serviceMain(DWORD dwArgc, LPTSTR *lpszArgv) {
	if (!fillCP()) {
        cleanup();
		return;
    }

	if (!createJVM()) {
        cleanup();
		return;
    }
	
    serviceStatusHandle = RegisterServiceCtrlHandler(
		TEXT(SERVICENAME),
		(LPHANDLER_FUNCTION)serviceCtrl);
	
    if(!serviceStatusHandle) {
        cleanup();
		return;
    }
	
    serviceStatus.dwServiceType = SERVICE_WIN32_OWN_PROCESS;
    serviceStatus.dwServiceSpecificExitCode = 0;
	
    if(!sendStatusToSCMgr(SERVICE_START_PENDING, NO_ERROR, 1, SVC_START_TIMEOUT)) {
        cleanup();
		return;
    }
    if(!sendStatusToSCMgr(SERVICE_RUNNING, NO_ERROR, 0, 0)) {
        cleanup();
		return;
    }
	run();
    if(serviceStatusHandle != 0) {
        sendStatusToSCMgr(SERVICE_STOPPED, NO_ERROR, 0, 0);
    }
    return;
}

void stop() {
	if (jvm->AttachCurrentThread((LPVOID*)&env, NULL) < 0) {
		logServiceMessage(_T("Thread %d: attach failed\n"));
		return;
	}
	if (jserv_cls) {
		
		jmethodID mid;
		mid = env->GetStaticMethodID(jserv_cls, "stop", "()V");
		if (mid == 0) {
			logServiceMessage(_T("Thread %d: Can't find Serve.stop()"), EVENTLOG_ERROR_TYPE);
		} else
			env->CallStaticVoidMethod(jserv_cls, mid, NULL);
		
	}
    if (env->ExceptionOccurred()) {
        env->ExceptionDescribe();
    }
    jvm->DetachCurrentThread();
	sendStatusToSCMgr(SERVICE_STOPPED, NO_ERROR, 0, 0);
    logServiceMessage("stopped");
}

void cleanup() {
	if (jvm)
		jvm->DestroyJavaVM();
	free(installDir);
	free(customCP);
	free(jvmArgs);
	free(mainClass);
	//FreeLibrary(hLib);
}

void run() {
    jmethodID mid;
    jobjectArray args;

	jserv_cls = env->FindClass(mainClass?mainClass:DEFAULT_SRV_CLASS);
    if (jserv_cls == 0) {
		logServiceMessage(_T("Can't find Acme.Serve.Main class"), EVENTLOG_ERROR_TYPE);
        return;
    }
 
    mid = env->GetStaticMethodID(jserv_cls, "main", "([Ljava/lang/String;)V");
    if (mid == 0) {
		logServiceMessage(_T("Can't find main(String[])"), EVENTLOG_ERROR_TYPE);
        return;
    }

    args = env->NewObjectArray(0, 
                        env->FindClass("java/lang/String"), NULL);
    if (args == 0) {
		logServiceMessage(_T("Out of memory"), EVENTLOG_ERROR_TYPE);
        return;
    }
    env->CallStaticVoidMethod(jserv_cls, mid, args);
}

BOOL createJVM() {
    JavaVMInitArgs vm_args;
    jint res;
    char classpath[MAX_CLASSPATH_LEN];
	char userdir[MAXPATHLEN];
	char jvmpath[MAXPATHLEN];
	char jrepath[MAXPATHLEN];
	char apppath[2*MAXPATHLEN];
    HINSTANCE handle;
	if (installDir == NULL || strlen(installDir) > MAXPATHLEN)
		return FALSE;
	if (GetJREPath(jrepath, sizeof jrepath) == FALSE) {
		logServiceMessage(_T("Can't find Java VM path"), EVENTLOG_ERROR_TYPE);
		return FALSE;
	}
	if (GetJVMPath(jrepath, "server", jvmpath, sizeof jvmpath) == FALSE) {
		if (GetJVMPath(jrepath, "client", jvmpath, sizeof jvmpath) == FALSE) {
			logServiceMessage(_T("Can't find neither server nor client Java VM path"), EVENTLOG_ERROR_TYPE);
			return FALSE;
		}
	}
    // Load the Java VM DLL 
    if ((handle = LoadLibrary(jvmpath)) == 0) {
		logServiceMessage(_T("Error loading JVM"), EVENTLOG_ERROR_TYPE);
		return JNI_FALSE;
    }


    // IMPORTANT: specify vm_args version # if you use JDK1.1.2 and beyond 
	FARPROC jni_create_java_vm = GetProcAddress(handle, "JNI_CreateJavaVM");
	FARPROC jni_get_default_java_vm_init_args = GetProcAddress(handle, "JNI_GetDefaultJavaVMInitArgs");
	if(jni_create_java_vm && jni_get_default_java_vm_init_args == NULL) {
		return JNI_FALSE;
	}
	// find extra JVM parameters
	int narg = 0;
	char *pc = jvmArgs;
	if (jvmArgs) {
		while(*pc) {
			if (*pc == '\n') {
				narg++;
				*pc = 0;
			}	
			*pc++;
		}
	}
	narg += 2;
	JavaVMOption *options = new JavaVMOption[narg];
	vm_args.version = JNI_VERSION_1_4;
	vm_args.nOptions = narg;
	vm_args.options = options;
	vm_args.ignoreUnrecognized = JNI_TRUE;
	if (mainClass) 
		sprintf_s(apppath, "%c%s\\lib\\app.jar%c%s\\app.jar", PATH_SEPARATOR, installDir, PATH_SEPARATOR, installDir);
	else
		*apppath = 0;
    // Append USER_CLASSPATH to the end of default system class path 
	if (customCP)
		sprintf_s(classpath, sizeof classpath, "-Djava.class.path=%s\\webserver.jar%c%s\\lib\\webserver.jar%c"
		"%s\\servlet.jar%c%s\\lib\\servlet.jar%c%s\\war.jar%c%s\\lib\\war.jar%c%s%s", installDir, PATH_SEPARATOR, installDir, PATH_SEPARATOR,
		installDir, PATH_SEPARATOR, installDir,  PATH_SEPARATOR, installDir,  PATH_SEPARATOR, installDir, PATH_SEPARATOR, customCP, apppath);
	else
		sprintf_s(classpath, sizeof classpath, "-Djava.class.path=%s\\webserver.jar%c%s\\lib\\webserver.jar%c"
		"%s\\servlet.jar%c%s\\lib\\servlet.jar%c%s\\war.jar%c%s\\lib\\war.jar%s", installDir, PATH_SEPARATOR, installDir, PATH_SEPARATOR,
		installDir, PATH_SEPARATOR, installDir, PATH_SEPARATOR, installDir, PATH_SEPARATOR, installDir,apppath);	
	options[0].optionString = classpath;
	sprintf_s(userdir, sizeof userdir, "-Duser.dir=%s", installDir);
	options[1].optionString = userdir;
	pc = jvmArgs;
	for (int ia=2; ia<narg; ia++) {
		options[ia].optionString = pc;
		options[ia].extraInfo = NULL;
		pc += strlen(pc)+sizeof(char);
	}
	//logServiceMessage(userdir);
    // Create the Java VM 
    res = ((jint (JNICALL *)(JavaVM **, void **, void *))jni_create_java_vm)(&jvm,(LPVOID*)&env,&vm_args);
    if (res < 0) {
		logServiceMessage(_T("Can't create Java VM"), EVENTLOG_ERROR_TYPE);
        return FALSE;
    }
	return TRUE;
}

BOOL fillCP() {
	HKEY hKey;
	if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, REG_ROOT, 0, KEY_READ, &hKey) != 0) {
		return JNI_FALSE;
    }
	unsigned long type = REG_SZ;
	TCHAR version[10];
	unsigned long size = sizeof version;
	// TODO service name with version has to be used to separate
	// multiple services
	if (RegQueryValueEx(hKey, REG_K_CURRVER, 0, &type, (unsigned char*)version, &size) == 0) {
		HKEY hKeyVer;
		if (RegOpenKeyEx(hKey, version, 0, KEY_READ, &hKeyVer) == 0) {
			size = (sizeof TCHAR)*(MAXPATHLEN+1);
			installDir = (LPTSTR)malloc(size);
			if (RegQueryValueEx(hKeyVer, REG_V_PATH, 0, &type, (unsigned char*)installDir, &size) == 0) {
				size = (sizeof TCHAR)*(MAXPATHLEN+1);
				customCP  = (LPTSTR)malloc(size);
				type = REG_SZ;
				if (RegQueryValueEx(hKeyVer, REG_V_CP, 0, &type, (unsigned char*)customCP, &size) != 0) {
					free(customCP);
					customCP = NULL;
				}
				type = REG_SZ;
				if (RegQueryValueEx(hKeyVer, REG_V_JVM_ARGS, 0, &type, NULL, &size) == ERROR_SUCCESS) {
					jvmArgs = (LPTSTR)malloc(size);
					if (RegQueryValueEx(hKeyVer, REG_V_JVM_ARGS, 0, &type, (unsigned char*)jvmArgs, &size) != 0) {
						free(jvmArgs);
						jvmArgs = NULL;
					}
				}
				type = REG_SZ;
				if (RegQueryValueEx(hKeyVer, REG_V_ENTRY_POINT, 0, &type, NULL, &size) == ERROR_SUCCESS) {
					mainClass = (LPTSTR)malloc(size);
					if (RegQueryValueEx(hKeyVer, REG_V_ENTRY_POINT, 0, &type, (unsigned char*)mainClass, &size) != 0) {
						free(mainClass);
						mainClass = NULL;
					}
				}

				RegCloseKey(hKeyVer);
				RegCloseKey(hKey);
				return JNI_TRUE;
			}
			RegCloseKey(hKeyVer);
		}
    }
	RegCloseKey(hKey);
	return JNI_FALSE;
}

/* Stop the running NT service */
void endService(char* msg) {
    logServiceMessage(msg);
	
    if (serviceStatusHandle != 0) {
		sendStatusToSCMgr(SERVICE_STOP_PENDING, GetLastError(), 0, 0);
    }
}


void unistallService(LPCTSTR serviceName) {
    
    SC_HANDLE hService = OpenService(scm,
		serviceName,
		SERVICE_ALL_ACCESS);
    
    if (hService == NULL) {
		DWORD err = GetLastError();
		char * msg = getErrorMsg(err, serviceName);
		if (msg == 0) {
			fprintf(stderr, 
				"Cannot open service %s: unrecognized error 0x%02x\n", 
				serviceName, err);
		} else {
			fprintf(stderr, "Cannot open service %s: %s\n", serviceName, msg);
		}
		return;
    }
	
    if((DeleteService(hService))) {
		printf("Removed service %s.\n", serviceName);
    } else {
		DWORD err = GetLastError();
		char *msg = getErrorMsg(err, serviceName);
		if (msg == 0) {
			fprintf(stderr, 
				"Cannot remove service %s: unrecognized error %dL\n", 
				serviceName, err);
		} else {
			fprintf(stderr, "Cannot remove service %s: %s\n", 
				serviceName, msg);
		}
    }
}


/* This is called when the service is installed */
#define _DEPENDENCY_STR_LEN  200
void installService(LPCTSTR serviceName, LPCTSTR displayName, LPCTSTR serviceExe,
                    LPCTSTR dependencies, int currentDependenciesLen,
                    LPCTSTR homeDir, LPCTSTR classPath, LPCTSTR jvmArgs, LPCSTR mainClass)  {
    LPCTSTR lpszBinaryPathName = serviceExe;
	char * allDependencies = new TCHAR[_DEPENDENCY_STR_LEN];
    char * ptr = allDependencies;
	int rest_len = _DEPENDENCY_STR_LEN;
	if (currentDependenciesLen > 0 && dependencies != NULL) {
		strcpy_s(ptr, rest_len, dependencies);
		ptr += currentDependenciesLen;
		rest_len -= currentDependenciesLen;
	}
	
    // add static dependencies
    strcpy_s(ptr, rest_len, NUP_SERVICE_NAME);
    ptr += sizeof(NUP_SERVICE_NAME);
	rest_len -= sizeof(NUP_SERVICE_NAME);
    strcpy_s(ptr, rest_len, TCPIP_SERVICE_NAME);
    ptr += sizeof(TCPIP_SERVICE_NAME);
	rest_len -= sizeof(TCPIP_SERVICE_NAME);
    strcpy_s(ptr, rest_len, AFD_SERVICE_NAME);
    ptr += sizeof(AFD_SERVICE_NAME);
    
    *ptr = '\0';
	BOOL needToFree = FALSE;
	if (strchr(lpszBinaryPathName, ' ') != NULL || TRUE) {
		int buf_len = strlen(lpszBinaryPathName)+3;
		char *quotedBinPath = new char[buf_len];
		
		sprintf_s(quotedBinPath, buf_len, "\"%s\"", lpszBinaryPathName);
		lpszBinaryPathName = quotedBinPath;
		needToFree = TRUE;
	}
	printf("Service %s.\n", lpszBinaryPathName);
	// TODO 1st store parameters, 2nd install service,, since if parameters failed, then service is not operable
    SC_HANDLE hService = CreateService(scm,                          // SCManager database
		serviceName,                 // name of service
		NULL,                 // name to display
		SERVICE_ALL_ACCESS,          // desired access
		SERVICE_WIN32_OWN_PROCESS,   // service type
		SERVICE_AUTO_START,          // start type
		SERVICE_ERROR_NORMAL,        // error control type
		lpszBinaryPathName,          // binary name
		NULL,                        // no load ordering group
		NULL,                        // no tag idenitifier
		allDependencies,                // we depend ???
		NULL,                        // loacalsystem account
		NULL);                       // no password
	delete allDependencies;
	if (needToFree)
		delete (void*)lpszBinaryPathName;
    if (hService != NULL) {
		printf("Added service %s (%s).\n", serviceName, displayName);
		// use to add description
		SERVICE_DESCRIPTION description;
		if (serviceName != displayName) {
			description.lpDescription = (LPSTR)displayName;
			ChangeServiceConfig2(hService, SERVICE_CONFIG_DESCRIPTION, &description);
		}
		HKEY hKey;	
		if(RegCreateKeyEx(HKEY_LOCAL_MACHINE,
			REG_ROOT,
			0,
			NULL,
			REG_OPTION_NON_VOLATILE,
			KEY_ALL_ACCESS,
			NULL,
			&hKey, NULL) == ERROR_SUCCESS) {
			if (RegSetValueEx(
				hKey,
				REG_K_CURRVER,
				0,
				REG_SZ,
				(const BYTE* )VERSION,
				sizeof VERSION 
				) == ERROR_SUCCESS) {
				HKEY hKeyVer;	
				if(RegCreateKeyEx(hKey,
					VERSION,
					0,
					NULL,
					REG_OPTION_NON_VOLATILE,
					KEY_ALL_ACCESS,
					NULL,
					&hKeyVer, NULL) == ERROR_SUCCESS) {
					if (RegSetValueEx(
						hKeyVer,
						REG_V_PATH,
						0,
						REG_SZ,
						(const BYTE* )homeDir,
						_tcslen(homeDir)+sizeof TCHAR 
						) == ERROR_SUCCESS) {
							printf("Set path %s.\n", homeDir);	
					}
					if (classPath) {
						if (RegSetValueEx(
							hKeyVer,
							REG_V_CP,
							0,
							REG_SZ,
							(const BYTE* )classPath,
							_tcslen(classPath)+sizeof TCHAR 
							) == ERROR_SUCCESS) {
							printf("Set class path %s.\n", classPath);	
						}
					}
					if (jvmArgs) {
						if (RegSetValueEx(
							hKeyVer,
							REG_V_JVM_ARGS,
							0,
							REG_SZ,
							(const BYTE* )jvmArgs,
							_tcslen(jvmArgs)+sizeof TCHAR 
							) == ERROR_SUCCESS) {
							printf("Set JVM args %s.\n", jvmArgs);	
						}
					}
					if (mainClass) {
						if (RegSetValueEx(
							hKeyVer,
							REG_V_ENTRY_POINT,
							0,
							REG_SZ,
							(const BYTE* )mainClass,
							_tcslen(mainClass)+sizeof TCHAR 
							) == ERROR_SUCCESS) {
							printf("Set main class to %s.\n", mainClass);	
						}
					} else {
						if (RegDeleteKey(hKeyVer,
							REG_V_ENTRY_POINT)  == ERROR_SUCCESS) 
							printf("Default main class used.");					
					}
					RegCloseKey(hKeyVer);
				}
				RegCloseKey(hKey);
			}
		} else {
			fprintf(stderr, "Cannot create config info in the Registry.");
		}
    } else {
		DWORD err = GetLastError();
		char *msg = getErrorMsg(err, serviceName);
		if (msg == 0) {
			fprintf(stderr, 
				"Cannot create service %s: unrecognized error %dL\n", 
				serviceName, err);
		} else {
			fprintf(stderr, "Cannot create service %s: %s\n", 
				serviceName, msg);
		}
		return;
    }
    CloseServiceHandle(hService);
}

char *argsToLine(int argc, char* argv[]) {
	// calculate memory
	int msize = 0;
	char **pc = argv;
	int c = argc;
	while(c) {
		msize += strlen(*pc)+sizeof(char);
		pc++;
		c--;
	}
	char * result = new char[msize+sizeof(char)];
	char *rp = result;
	while(argc) {
		strcpy(rp, *argv);
		rp += strlen(*argv);
		*rp = '\n';
		rp++;
		argv++;
		argc--;
	}
	*rp = 0;
	return result;
}

