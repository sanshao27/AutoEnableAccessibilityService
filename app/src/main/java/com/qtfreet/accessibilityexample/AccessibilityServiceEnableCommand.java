package com.qtfreet.accessibilityexample;

import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by qtfreet on 2016/12/1.
 */

public class AccessibilityServiceEnableCommand {
    private static String PACKAGE_NAME = "";          //这里包名需要自己设置
    private static boolean DEBUG = false;
    private static final String TAG = "qtfreet00";
    private static final String _LD_LIBRARY_PATH = "_LD_LIBRARY_PATH";
    public static final String LD_LIBRARY_PATH = "LD_LIBRARY_PATH";

    public static void main(String[] strArr) {
        String str;
        if (strArr != null && strArr.length > 1 && isSDKSupport()) {
            str = strArr[0];
            //str的格式为"包名/类名"，如 "com.qihoo.appstore/com.qihoo.appstore.accessibility.AppstoreAccessibility"
            DEBUG = strArr[1].equals("true");
            enableAccessibilityService(str);
        }
    }


    private static void enableAccessibilityService(String str) {
        if (isSDKSupport() && !enableAccessibilityServiceV1(str)) {
            enableAccessibilityServiceV2(str);
        }
    }


    private static boolean isSDKSupport() {
        return Build.VERSION.SDK_INT >= 16;
    }


    public static int execShP(File file, Map map, String... strArr) {
        String findShShellBin = findShShellBin();
        if (findShShellBin != null) {
            return execP(findShShellBin, file, map, strArr);
        }
        throw new RuntimeException("The devices(" + Build.MODEL + ") has no shell sh");
    }

    public static String findShShellBin() {
        String str = System.getenv("PATH");
        if (str != null && str.length() > 0) {
            for (String file : str.split(":")) {
                File file2 = new File(file, "sh");
                if (file2.exists()) {
                    return file2.getPath();
                }
            }
        }
        return null;
    }


    private static String getLdLibPath(String str) {
        String systemEnv = System.getenv(LD_LIBRARY_PATH);
        if (TextUtils.isEmpty(systemEnv)) {
            systemEnv = System.getenv(_LD_LIBRARY_PATH);
        }
        systemEnv = systemEnv + "/vendor/lib:/system/lib:/vendor/lib*:/system/lib*";
        if (!TextUtils.isEmpty(str)) {
            systemEnv = systemEnv + ":" + str;
        }
        systemEnv = systemEnv + ":.";
        List<String> arrayList = new ArrayList();
        String[] split = systemEnv.split(":");
        if (split.length <= 0) {
            return systemEnv;
        }
        for (String s : split) {
            if (!arrayList.contains(s)) {
                arrayList.add(s);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        int k = 1;
        for (String strArray : arrayList) {
            if (k == 0) {
                stringBuilder.append(":");
            }
            stringBuilder.append(strArray);
            k = 0;
        }
        return stringBuilder.toString();
    }


    private static void setupEnvs(Map map) {
        String ldLibPath;
        if (map == null) {
            map = new HashMap();
        }
        if (map.containsKey(LD_LIBRARY_PATH)) {
            ldLibPath = getLdLibPath((String) map.get(LD_LIBRARY_PATH));
            map.put(LD_LIBRARY_PATH, ldLibPath);
            map.put(_LD_LIBRARY_PATH, ldLibPath);
            return;
        }
        ldLibPath = getLdLibPath(null);
        map.put(LD_LIBRARY_PATH, ldLibPath);
        map.put(_LD_LIBRARY_PATH, ldLibPath);
    }


    public static int execP(String findshShell, File file, Map map, String... strArr) {
        String str;
        if (findshShell == null) {
            throw new RuntimeException("The devices(" + Build.MODEL + ") has not shell ");
        }
        ProcessBuilder processBuilder = new ProcessBuilder(new String[0]).command(new String[]{findshShell}).redirectErrorStream(true);
        if (file != null) {
            processBuilder.directory(file);
        }
        processBuilder.environment().putAll(System.getenv());
        setupEnvs(map);
        if (map != null && map.size() > 0) {
            processBuilder.environment().putAll(map);
        }
        try {
            Process process = processBuilder.start();
            OutputStream os = process.getOutputStream();
            int len = strArr.length;
            int i;
            for (i = 0; i < len; i++) {
                str = strArr[i];
                if (!str.endsWith("\n")) {
                    str = str + "\n";
                }
                os.write(str.getBytes());
                os.flush();
            }
            os.write("exit 0\n".getBytes());
            os.flush();
            Thread.sleep(1000);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuffer sb = new StringBuffer();
            while (br != null) {
                sb.append(br.readLine());
            }
            Log.e(TAG, sb.toString());
            br.close();
            process.destroy();
            process = null;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return 0;
    }


    private static boolean enableAccessibilityServiceV1(String str) {
        try {
            if (DEBUG) {
                Log.d(TAG, "enableAccessibilityServiceV1:" + str);
            }

            Bundle bundle = new Bundle();
            bundle.putInt("_user", 0);
            bundle.putString("value", str);
            call("settings", PACKAGE_NAME, "PUT_secure", "enabled_accessibility_services", bundle);
            bundle = new Bundle();
            bundle.putInt("_user", 0);
            bundle.putString("value", "1");
            call("settings", PACKAGE_NAME, "PUT_secure", "accessibility_enabled", bundle);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    private static boolean enableAccessibilityServiceV2(String str) {
        if (DEBUG) {
            Log.d(TAG, "enableAccessibilityServiceV2:" + str);
        }
        String format = String.format("pm enable %s &", new Object[]{str});
        String format2 = String.format("content call --uri %s --method %s --arg %s --extra _user:i:0 --extra value:s:%s", new Object[]{"content://settings/secure", "PUT_secure", "enabled_accessibility_services", str});
        execShP(new File("/"), null, format, format2);
        format = String.format("content call --uri %s --method %s --arg %s --extra _user:i:0 --extra value:s:%s", new Object[]{"content://settings/secure", "PUT_secure", "accessibility_enabled", Integer.valueOf(1)});
        execShP(new File("/"), null, format);
        return true;
    }


    public static Object getDefault() {
        try {
            return Class.forName("android.app.ActivityManagerNative").getMethod("getDefault", new Class[0]).invoke(null, new Object[0]);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    public static Object getContentProviderExternal(String str, int i, IBinder iBinder) {
        try {
            return Class.forName("android.app.ActivityManagerNative").getMethod("getContentProviderExternal", new Class[]{String.class, Integer.TYPE, IBinder.class}).invoke(getDefault(), new Object[]{str, Integer.valueOf(i), iBinder});
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeContentProviderExternal(String str, IBinder iBinder) {
        try {
            Class.forName("android.app.ActivityManagerNative").getMethod("removeContentProviderExternal", new Class[]{String.class, IBinder.class}).invoke(getDefault(), new Object[]{str, iBinder});
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void call(String str, String str2, String str3, String str4, Bundle bundle) {
        Object obj = null;
        IBinder binder = new Binder();
        try {
            Object contentProviderExternal = getContentProviderExternal(str, 0, binder);
            if (contentProviderExternal == null) {
                throw new IllegalStateException("Could not find provider: " + str);
            }
            obj = contentProviderExternal.getClass().getField("provider").get(contentProviderExternal);

            Class.forName("android.content.IContentProvider").getMethod("call", new Class[]{String.class, String.class, String.class, Bundle.class}).invoke(obj, new Object[]{str2, str3, str4, bundle});
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (obj != null) {
                removeContentProviderExternal(str, binder);
            }
        }
    }

}
