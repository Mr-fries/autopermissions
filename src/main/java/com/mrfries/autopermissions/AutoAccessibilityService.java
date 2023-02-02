package com.mrfries.autopermissions;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mr.fries
 * Created on 2023/1/11
 * desc: A service use to monitor permissions required, and perform to auto-click. See README document for details.
 */
@SuppressLint("LongLogTag")
public class AutoAccessibilityService extends AccessibilityService {
    /**
     * log tag
     */
    private static final String TAG = "AutoAccessibilityService";

    // Bundle key tag constants
    /**
     * take APP PACKAGE NAME in (necessary)
     */
    public static String APP_PKG_TAG = "app_pkg";
    /**
     * take SYSTEM SETTINGS PACKAGE NAME IN (unnecessary)
     */
    public static String SPECIAL_SETTING_PKG_TAG = "special_pkg";
    /**
     * take SYSTEM INSTALLER PACKAGE NAME IN (unnecessary)
     */
    public static String NORMAL_SETTING_PKG_TAG = "normal_pkg";
    /**
     * change default click delay (unnecessary)
     */
    public static String CLICK_DELAY_TAG = "click_delay";

    // Variables
    private String APP_PKG = null;
    private String SPECIAL_SETTINGS_PKG = "com.android.settings";
    private String NORMAL_SETTINGS_PKG = "com.android.packageinstaller";
    private int CLICK_DELAY = 500;

    // Constants
    private final String SWITCH_BUTTON = "android.widget.Switch";
    private final String TEXT_VIEW = "android.widget.TextView";
    private final String BUTTON = "android.widget.Button";

    // This service cannot be stopped by some unknown reasons, so we force to use a var to control the service`s operations.
    private boolean isInterrupt = false;
    // Has not used yet.
    private Map<Integer, Boolean> handledMap = new HashMap();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get variables from intent

        if (intent.hasExtra(APP_PKG_TAG)) {
            APP_PKG = intent.getStringExtra(APP_PKG_TAG);
            if (APP_PKG == null) {
                // this service may cannot stop without APP PACKAGE NAME
                Log.e(TAG, "AutoAccessibilityService running without APP PACKAGE NAME, it may cannot NORMALLY END");
            }
        } else {
            // this service may cannot stop without APP PACKAGE NAME
            Log.e(TAG, "AutoAccessibilityService running without APP PACKAGE NAME, it may cannot NORMALLY END");
        }

        if (intent.hasExtra(NORMAL_SETTING_PKG_TAG)) {
            // set NORMAL_SETTINGS_PKG
            NORMAL_SETTINGS_PKG = intent.getStringExtra(NORMAL_SETTING_PKG_TAG);
        }

        if (intent.hasExtra(SPECIAL_SETTING_PKG_TAG)) {
            // set SPECIAL_SETTINGS_PKG
            SPECIAL_SETTINGS_PKG = intent.getStringExtra(SPECIAL_SETTING_PKG_TAG);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo info = getServiceInfo();
        if (APP_PKG == null) {
            info.packageNames = new String[]{NORMAL_SETTINGS_PKG, SPECIAL_SETTINGS_PKG};
        } else {
            info.packageNames = new String[]{APP_PKG, NORMAL_SETTINGS_PKG, SPECIAL_SETTINGS_PKG};
        }

        Log.i(TAG, "Package names in monitoring: " + Arrays.toString(info.packageNames));
        setServiceInfo(info);
    }



    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (isInterrupt) {
            return;
        }
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo != null) {
            int eventType = event.getEventType();
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
                if (handledMap.get(event.getWindowId()) == null) {
                    if (nodeInfo.getPackageName() != null) {
                        if (nodeInfo.getPackageName().toString().equals(NORMAL_SETTINGS_PKG)) {
                            boolean handled = iterateNodesAndHandle(nodeInfo);
                            if (handled) {
//                                handledMap.put(event.getWindowId(), true);
                            }
                        } else if (nodeInfo.getPackageName().toString().equals(SPECIAL_SETTINGS_PKG)) {
                            boolean handled = iterateNodesAndHandleSettings(nodeInfo);
                            if (handled) {

                            }
                        } else if (APP_PKG!=null && APP_PKG.equals(nodeInfo.getPackageName().toString())){
                            Log.i(TAG, "Main App has been detected! Service end.");
                            isInterrupt = true;
                            stopSelf();
                        } else {
                            Log.e(TAG, "other pkg Activity at the top: " + nodeInfo.getPackageName().toString());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
    }

    /**
     * When requiring special permissions, click the switch button
     */
    private boolean iterateNodesAndHandleSettings(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            int childCount = nodeInfo.getChildCount();

            String nodeType = nodeInfo.getClassName().toString();

            if (nodeType.equals(SWITCH_BUTTON)) {
                if (nodeInfo.getText() != null) {
                    if (nodeInfo.getText().toString().equals("OFF")) {
                        // tick the Switch Button
                        new Handler().postDelayed(() -> {
                            nodeInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.e(TAG, "Switch Button Click!");

                            // back
                            execShell("input keyevent 4");
                        }, CLICK_DELAY);
                        return true;
                    }
                }
            }

            // iterator node`s child nodes
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
                if (iterateNodesAndHandleSettings(childNodeInfo)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * When requiring normal permissions, click the button
     */
    private boolean iterateNodesAndHandle(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            String nodeContent;
            int childCount = nodeInfo.getChildCount();

            if (nodeInfo.getText() != null) {
                nodeContent = nodeInfo.getText().toString();
                if (BUTTON.equals(nodeInfo.getClassName())) {
                    if ("允许".equals(nodeContent)
                            || "始终允许".equals(nodeContent)) {
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return true;
                    }
                } else if (TEXT_VIEW.equals(nodeInfo.getClassName())) {
                    if ("允许".equals(nodeContent)) {
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return true;
                    }
                }
            }


            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
                if (iterateNodesAndHandle(childNodeInfo)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Init auto-click service
     * @param appPackageName app`s main package name
     */
    public static void initAuto(String appPackageName) {
        // Get automatic clicking-barrier-free permission
        execShell("settings put secure enabled_accessibility_services " + appPackageName
                + "/com.mrfries.autopermissions.AutoAccessibilityService");
        execShell("settings put secure accessibility_enabled 1");
    }

    private static boolean execShell(String command) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            if (exitValue == 0) {
                Log.i(TAG, command + ",root and exec success:");
                return true;
            } else {
                Log.e(TAG, command + ",root and exec fail:" + exitValue);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "root and exec cmd Unexpected error - Here is what I know: " + e.toString());
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

    }
}
