/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova.inappbrowser;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.provider.Browser;
import android.app.DownloadManager;
import android.os.Environment;
import android.webkit.URLUtil;
import android.webkit.MimeTypeMap;
import android.database.Cursor;
import android.os.Handler;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.net.http.SslError;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.DownloadListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.Config;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaHttpAuthHandler;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.StringTokenizer;

@SuppressLint("SetJavaScriptEnabled")
public class InAppBrowser extends CordovaPlugin {

    private static final String NULL = "null";
    protected static final String LOG_TAG = "InAppBrowser";
    private static final String SELF = "_self";
    private static final String SYSTEM = "_system";
    private static final String EXIT_EVENT = "exit";
    private static final String LOCATION = "location";
    private static final String ZOOM = "zoom";
    private static final String FULL_SCREEN = "fullscreen";
    private static final String HIDDEN = "hidden";
    private static final String LOAD_START_EVENT = "loadstart";
    private static final String LOAD_STOP_EVENT = "loadstop";
    private static final String LOAD_ERROR_EVENT = "loaderror";
    private static final String MESSAGE_EVENT = "message";
    private static final String CLEAR_ALL_CACHE = "clearcache";
    private static final String CLEAR_SESSION_CACHE = "clearsessioncache";
    private static final String HARDWARE_BACK_BUTTON = "hardwareback";
    private static final String MEDIA_PLAYBACK_REQUIRES_USER_ACTION = "mediaPlaybackRequiresUserAction";
    private static final String SHOULD_PAUSE = "shouldPauseOnSuspend";
    private static final Boolean DEFAULT_HARDWARE_BACK = true;
    private static final String USER_WIDE_VIEW_PORT = "useWideViewPort";
    private static final String TOOLBAR_COLOR = "toolbarcolor";
    private static final String CLOSE_BUTTON_CAPTION = "closebuttoncaption";
    private static final String CLOSE_BUTTON_COLOR = "closebuttoncolor";
    private static final String LEFT_TO_RIGHT = "lefttoright";
    private static final String HIDE_NAVIGATION = "hidenavigationbuttons";
    private static final String NAVIGATION_COLOR = "navigationbuttoncolor";
    private static final String HIDE_URL = "hideurlbar";
    private static final String FOOTER = "footer";
    private static final String FOOTER_COLOR = "footercolor";
    private static final String BEFORELOAD = "beforeload";
    private static final String FULLSCREEN = "fullscreen";

    private static final int TOOLBAR_HEIGHT = 48;

    private static final List customizableOptions = Arrays.asList(CLOSE_BUTTON_CAPTION, TOOLBAR_COLOR, NAVIGATION_COLOR, CLOSE_BUTTON_COLOR, FOOTER_COLOR);

    private InAppBrowserDialog dialog;
    private WebView inAppWebView;
    private EditText edittext;
    private CallbackContext callbackContext;
    private boolean showLocationBar = true;
    private boolean showZoomControls = true;
    private boolean openWindowHidden = false;
    private boolean clearAllCache = false;
    private boolean clearSessionCache = false;
    private boolean hadwareBackButton = true;
    private boolean mediaPlaybackRequiresUserGesture = false;
    private boolean fullScreenFeature = false;
    private boolean shouldPauseInAppBrowser = false;
    private boolean useWideViewPort = true;
    private ValueCallback<Uri[]> mUploadCallback;
    private final static int FILECHOOSER_REQUESTCODE = 1;
    private String closeButtonCaption = "";
    private String closeButtonColor = "";
    private boolean leftToRight = false;
    private int toolbarColor = android.graphics.Color.LTGRAY;
    private boolean hideNavigationButtons = false;
    private String navigationButtonColor = "";
    private boolean hideUrlBar = false;
    private boolean showFooter = false;
    private String footerColor = "";
    private String beforeload = "";
    private boolean fullscreen = true;
    private String[] allowedSchemes;
    private InAppBrowserClient currentClient;
    private String downloadInProgress = "Download in progress...";
    private String downloadCompleted = "Download completed successfully";

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action the action to execute.
     * @param args JSONArry of arguments for the plugin.
     * @param callbackContext the callbackContext used when calling back into JavaScript.
     * @return A PluginResult object with a status and message.
     */
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("open")) {
            this.callbackContext = callbackContext;
            final String url = args.getString(0);
            String t = args.optString(1);
            if (t == null || t.equals("") || t.equals(NULL)) {
                t = SELF;
            }
            final String target = t;
            final HashMap<String, String> features = parseFeature(args.optString(2));

            LOG.d(LOG_TAG, "target = " + target);

            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String result = "";
                    // SELF
                    if (SELF.equals(target)) {
                        LOG.d(LOG_TAG, "in self");
                        /* This code exists for compatibility between 3.x and 4.x versions of Cordova.
                         * Previously the Config class had a static method, isUrlWhitelisted(). That
                         * responsibility has been moved to the plugins, with an aggregating method in
                         * PluginManager.
                         */
                        Boolean shouldAllowNavigation = null;
                        if (url.startsWith("javascript:")) {
                            shouldAllowNavigation = true;
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                Method iuw = Config.class.getMethod("isUrlWhiteListed", String.class);
                                shouldAllowNavigation = (Boolean)iuw.invoke(null, url);
                            } catch (NoSuchMethodException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (IllegalAccessException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (InvocationTargetException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            }
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                Method gpm = webView.getClass().getMethod("getPluginManager");
                                PluginManager pm = (PluginManager)gpm.invoke(webView);
                                Method san = pm.getClass().getMethod("shouldAllowNavigation", String.class);
                                shouldAllowNavigation = (Boolean)san.invoke(pm, url);
                            } catch (NoSuchMethodException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (IllegalAccessException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (InvocationTargetException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            }
                        }
                        // load in webview
                        if (Boolean.TRUE.equals(shouldAllowNavigation)) {
                            LOG.d(LOG_TAG, "loading in webview");
                            webView.loadUrl(url);
                        }
                        //Load the dialer
                        else if (url.startsWith(WebView.SCHEME_TEL))
                        {
                            try {
                                LOG.d(LOG_TAG, "loading in dialer");
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse(url));
                                cordova.getActivity().startActivity(intent);
                            } catch (android.content.ActivityNotFoundException e) {
                                LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                            }
                        }
                        // load in InAppBrowser
                        else {
                            LOG.d(LOG_TAG, "loading in InAppBrowser");
                            result = showWebPage(url, features);
                        }
                    }
                    // SYSTEM
                    else if (SYSTEM.equals(target)) {
                        LOG.d(LOG_TAG, "in system");
                        result = openExternal(url);
                    }
                    // BLANK - or anything else
                    else {
                        LOG.d(LOG_TAG, "in blank");
                        result = showWebPage(url, features);
                    }

                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                }
            });
        }
        else if (action.equals("close")) {
            closeDialog();
        }
        else if (action.equals("loadAfterBeforeload")) {
            if (beforeload == null) {
                LOG.e(LOG_TAG, "unexpected loadAfterBeforeload called without feature beforeload=yes");
            }
            final String url = args.getString(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                    inAppWebView.loadUrl(url);
                }
            });
        }
        else if (action.equals("injectScriptCode")) {
            String jsWrapper = null;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(){prompt(JSON.stringify([eval(%%s)]), 'gap-iab://%s')})()", callbackContext.getCallbackId());
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectScriptFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('script'); c.src = %%s; c.onload = function() { prompt('', 'gap-iab://%s'); }; d.body.appendChild(c); })(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('script'); c.src = %s; d.body.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleCode")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('style'); c.innerHTML = %%s; d.body.appendChild(c); prompt('', 'gap-iab://%s');})(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('style'); c.innerHTML = %s; d.body.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %%s; d.head.appendChild(c); prompt('', 'gap-iab://%s');})(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %s; d.head.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("show")) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null && !cordova.getActivity().isFinishing()) {
                        dialog.show();
                    }
                }
            });
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
        }
        else if (action.equals("hide")) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null && !cordova.getActivity().isFinishing()) {
                        dialog.hide();
                    }
                }
            });
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
        }
        else {
            return false;
        }
        return true;
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        closeDialog();
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     */
    @Override
    public void onPause(boolean multitasking) {
        if (shouldPauseInAppBrowser) {
            inAppWebView.onPause();
        }
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    public void onResume(boolean multitasking) {
        if (shouldPauseInAppBrowser) {
            inAppWebView.onResume();
        }
    }

    /**
     * Called by AccelBroker when listener is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        closeDialog();
    }

    /**
     * Inject an object (script or style) into the InAppBrowser WebView.
     *
     * This is a helper method for the inject{Script|Style}{Code|File} API calls, which
     * provides a consistent method for injecting JavaScript code into the document.
     *
     * If a wrapper string is supplied, then the source string will be JSON-encoded (adding
     * quotes) and wrapped using string formatting. (The wrapper string should have a single
     * '%s' marker)
     *
     * @param source      The source object (filename or script/style text) to inject into
     *                    the document.
     * @param jsWrapper   A JavaScript string to wrap the source string in, so that the object
     *                    is properly injected, or null if the source string is JavaScript text
     *                    which should be executed directly.
     */
    private void injectDeferredObject(String source, String jsWrapper) {
        if (inAppWebView!=null) {
            String scriptToInject;
            if (jsWrapper != null) {
                org.json.JSONArray jsonEsc = new org.json.JSONArray();
                jsonEsc.put(source);
                String jsonRepr = jsonEsc.toString();
                String jsonSourceString = jsonRepr.substring(1, jsonRepr.length()-1);
                scriptToInject = String.format(jsWrapper, jsonSourceString);
            } else {
                scriptToInject = source;
            }
            final String finalScriptToInject = scriptToInject;
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                    inAppWebView.evaluateJavascript(finalScriptToInject, null);
                }
            });
        } else {
            LOG.d(LOG_TAG, "Can't inject code into the system browser");
        }
    }

    /**
     * Put the list of features into a hash map
     *
     * @param optString
     * @return
     */
    private HashMap<String, String> parseFeature(String optString) {
        if (optString.equals(NULL)) {
            return null;
        } else {
            HashMap<String, String> map = new HashMap<String, String>();
            StringTokenizer features = new StringTokenizer(optString, ",");
            StringTokenizer option;
            while(features.hasMoreElements()) {
                option = new StringTokenizer(features.nextToken(), "=");
                if (option.hasMoreElements()) {
                    String key = option.nextToken();
                    String value = option.nextToken();
                    if (!customizableOptions.contains(key)) {
                        value = value.equals("yes") || value.equals("no") ? value : "yes";
                    }
                    map.put(key, value);
                }
            }
            return map;
        }
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url the url to load.
     * @return "" if ok, or error message.
     */
    public String openExternal(String url) {
        try {
            Intent intent = null;
            intent = new Intent(Intent.ACTION_VIEW);
            // Omitting the MIME type for file: URLs causes "No Activity found to handle Intent".
            // Adding the MIME type to http: URLs causes them to not be handled by the downloader.
            Uri uri = Uri.parse(url);
            if ("file".equals(uri.getScheme())) {
                intent.setDataAndType(uri, webView.getResourceApi().getMimeType(uri));
            } else {
                intent.setData(uri);
            }
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, cordova.getActivity().getPackageName());
            // CB-10795: Avoid circular loops by preventing it from opening in the current app
            this.openExternalExcludeCurrentApp(intent);
            return "";
            // not catching FileUriExposedException explicitly because buildtools<24 doesn't know about it
        } catch (java.lang.RuntimeException e) {
            LOG.d(LOG_TAG, "InAppBrowser: Error loading url "+url+":"+ e.toString());
            return e.toString();
        }
    }

    /**
     * Opens the intent, providing a chooser that excludes the current app to avoid
     * circular loops.
     */
    private void openExternalExcludeCurrentApp(Intent intent) {
        String currentPackage = cordova.getActivity().getPackageName();
        boolean hasCurrentPackage = false;

        PackageManager pm = cordova.getActivity().getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        ArrayList<Intent> targetIntents = new ArrayList<Intent>();

        for (ResolveInfo ri : activities) {
            if (!currentPackage.equals(ri.activityInfo.packageName)) {
                Intent targetIntent = (Intent)intent.clone();
                targetIntent.setPackage(ri.activityInfo.packageName);
                targetIntents.add(targetIntent);
            }
            else {
                hasCurrentPackage = true;
            }
        }

        // If the current app package isn't a target for this URL, then use
        // the normal launch behavior
        if (hasCurrentPackage == false || targetIntents.size() == 0) {
            this.cordova.getActivity().startActivity(intent);
        }
        // If there's only one possible intent, launch it directly
        else if (targetIntents.size() == 1) {
            this.cordova.getActivity().startActivity(targetIntents.get(0));
        }
        // Otherwise, show a custom chooser without the current app listed
        else if (targetIntents.size() > 0) {
            Intent chooser = Intent.createChooser(targetIntents.remove(targetIntents.size()-1), null);
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toArray(new Parcelable[] {}));
            this.cordova.getActivity().startActivity(chooser);
        }
    }

    /**
     * Closes the dialog
     */
    public void closeDialog() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WebView childView = inAppWebView;
                // The JS protects against multiple calls, so this should happen only when
                // closeDialog() is called by other native code.
                if (childView == null) {
                    return;
                }

                childView.setWebViewClient(new WebViewClient() {
                    // NB: wait for about:blank before dismissing
                    public void onPageFinished(WebView view, String url) {
                        if (dialog != null && !cordova.getActivity().isFinishing()) {
                            dialog.dismiss();
                            dialog = null;
                        }
                    }
                });
                // NB: From SDK 19: "If you call methods on WebView from any thread
                // other than your app's UI thread, it can cause unexpected results."
                // http://developer.android.com/guide/webapps/migrating.html#Threads
                childView.loadUrl("about:blank");

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("type", EXIT_EVENT);
                    sendUpdate(obj, false);
                } catch (JSONException ex) {
                    LOG.d(LOG_TAG, "Should never happen");
                }
            }
        });
    }

    /**
     * Checks to see if it is possible to go back one page in history, then does so.
     */
    public void goBack() {
        if (this.inAppWebView.canGoBack()) {
            this.inAppWebView.goBack();
        }
    }

    /**
     * Can the web browser go back?
     * @return boolean
     */
    public boolean canGoBack() {
        return this.inAppWebView.canGoBack();
    }

    /**
     * Has the user set the hardware back button to go back
     * @return boolean
     */
    public boolean hardwareBack() {
        return hadwareBackButton;
    }

    /**
     * Checks to see if it is possible to go forward one page in history, then does so.
     */
    private void goForward() {
        if (this.inAppWebView.canGoForward()) {
            this.inAppWebView.goForward();
        }
    }

    /**
     * Navigate to the new page
     *
     * @param url to load
     */
    private void navigate(String url) {
        InputMethodManager imm = (InputMethodManager)this.cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);

        if (!url.startsWith("http") && !url.startsWith("file:")) {
            this.inAppWebView.loadUrl("http://" + url);
        } else {
            this.inAppWebView.loadUrl(url);
        }
        this.inAppWebView.requestFocus();
    }


    /**
     * Should we show the location bar?
     *
     * @return boolean
     */
    private boolean getShowLocationBar() {
        return this.showLocationBar;
    }

    /**
     * Check if downloads are allowed based on page URL parameter
     *
     * @return boolean
     */
    private boolean areDownloadsAllowed() {
        try {
            // Get the current page URL from the WebView
            String currentUrl = inAppWebView.getUrl();
              
            if (currentUrl == null) {
                return false;
            }
            
            Uri uri = Uri.parse(currentUrl);
            String allowDownloads = uri.getQueryParameter("AllowDownloadsIAB");
            boolean allowed = "true".equalsIgnoreCase(allowDownloads);
                  
            return allowed;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get custom download progress text from URL parameter or use default
     *
     * @return String
     */
    private String getDownloadInProgressText() {
        try {
            // Get the current page URL from the WebView
            String currentUrl = inAppWebView.getUrl();
              
            if (currentUrl == null) {
                return downloadInProgress;
            }
            
            Uri uri = Uri.parse(currentUrl);
            String customText = uri.getQueryParameter("downloadInProgress");
            return customText != null ? customText : downloadInProgress;
        } catch (Exception e) {
            return downloadInProgress;
        }
    }

    /**
     * Get custom download completed text from URL parameter or use default
     *
     * @return String
     */
    private String getDownloadCompletedText() {
        try {
            // Get the current page URL from the WebView
            String currentUrl = inAppWebView.getUrl();
              
            if (currentUrl == null) {
                return downloadCompleted;
            }
            
            Uri uri = Uri.parse(currentUrl);
            String customText = uri.getQueryParameter("downloadCompleted");
            return customText != null ? customText : downloadCompleted;
        } catch (Exception e) {
            return downloadCompleted;
        }
    }

    /**
     * Get localized error message for download failure reasons
     * This will use the device's OS language settings
     *
     * @param reason DownloadManager failure reason
     * @return String localized error message
     */
    /**
     * Get no storage message from URL parameter
     */
    private String getNoStorageMessage() {
        try {
            String url = webView.getUrl();
            if (url != null && url.contains("noStorageMessage=")) {
                String[] parts = url.split("noStorageMessage=");
                if (parts.length > 1) {
                    String message = parts[1].split("&")[0];
                    return java.net.URLDecoder.decode(message, "UTF-8");
                }
            }
        } catch (Exception e) {
        }
        return "Not enough storage space";
    }

    /**
     * Get request failed message from URL parameter
     */
    private String getRequestFailedMessage() {
        try {
            String url = webView.getUrl();
            if (url != null && url.contains("requestFailed=")) {
                String[] parts = url.split("requestFailed=");
                if (parts.length > 1) {
                    String message = parts[1].split("&")[0];
                    return java.net.URLDecoder.decode(message, "UTF-8");
                }
            }
        } catch (Exception e) {
        }
        return "Request failed";
    }

    /**
     * Check if the URL is a downloadable file
     *
     * @param url the URL to check
     * @return boolean
     */
    private boolean isDownloadableFile(String url) {        
        // First check if downloads are allowed for the current page
        if (!areDownloadsAllowed()) {
            return false;
        }
        
        // Common downloadable file extensions
        String[] downloadableExtensions = {
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".rar", ".7z", ".tar", ".gz",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg",
            ".mp3", ".mp4", ".avi", ".mov", ".wmv", ".flv",
            ".txt", ".csv", ".xml", ".json",
            ".apk", ".exe", ".dmg", ".pkg"
        };
        
        String lowerUrl = url.toLowerCase();
        
        // Check file extensions
        for (String extension : downloadableExtensions) {
            if (lowerUrl.endsWith(extension)) {
                return true;
            }
            // Also check if extension appears before query parameters
            if (lowerUrl.contains(extension + "?") || lowerUrl.contains(extension + "#")) {
                return true;
            }
        }
        
        // Also check if it's a direct download link pattern
        if (lowerUrl.contains("download") || lowerUrl.contains("attachment")) {
            return true;
        }
        
        return false;
    }

    /**
     * Handle file download using Android's DownloadManager
     *
     * @param url the URL to download
     * @param userAgent the user agent string
     * @param contentDisposition the content disposition
     * @param mimeType the MIME type
     */
    private void handleDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        try {
            // Check if DownloadManager is available
            DownloadManager manager = (DownloadManager) cordova.getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) {
                showDownloadError("Download service not available");
                return;
            }
            
            // Guess filename with better fallback
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
            if (filename == null || filename.isEmpty()) {
                // Try to extract filename from URL
                try {
                    Uri uri = Uri.parse(url);
                    String path = uri.getPath();
                    if (path != null && path.contains("/")) {
                        filename = path.substring(path.lastIndexOf("/") + 1);
                    }
                    if (filename == null || filename.isEmpty()) {
                        filename = "download_" + System.currentTimeMillis();
                        // Add extension based on MIME type
                        if (mimeType != null) {
                            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                            if (extension != null) {
                                filename += "." + extension;
                            }
                        }
                    }
                } catch (Exception e) {
                    filename = "download_" + System.currentTimeMillis();
                }
            }
                        
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            
            // Set request properties
            if (mimeType != null && !mimeType.isEmpty()) {
                request.setMimeType(mimeType);
            }
            
            // Add cookies if available
            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null && !cookies.isEmpty()) {
                request.addRequestHeader("Cookie", cookies);
            }
            
            // Set user agent
            if (userAgent != null && !userAgent.isEmpty()) {
                request.addRequestHeader("User-Agent", userAgent);
            }
            
            // Set download properties
            request.setDescription("Downloading " + filename);
            request.setTitle(filename);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            // Save to public Downloads directory (accessible via My Files app)
            try {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            } catch (SecurityException e) {
                // If permission denied, fall back to app-specific Documents directory
                request.setDestinationInExternalFilesDir(cordova.getActivity(), Environment.DIRECTORY_DOCUMENTS, filename);
            } catch (Exception e) {
                // Ultimate fallback to app-specific Documents directory
                request.setDestinationInExternalFilesDir(cordova.getActivity(), Environment.DIRECTORY_DOCUMENTS, filename);
            }
            
            
            // Make final copy for use in inner classes
            final String finalFilename = filename;
            
            // Enqueue download
            long downloadId;
            try {
                downloadId = manager.enqueue(request);
                
                // Show persistent progress dialog
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDownloadProgress();
                    }
                });
                
                // Register download completion listener
                registerDownloadCompleteListener(downloadId, finalFilename);
            } catch (Exception e) {
                // Handle cases where enqueue fails (e.g., no network)
                showDownloadError(getRequestFailedMessage());
                return;
            }
            
            // Also add a backup polling mechanism in case broadcast doesn't work
            startDownloadPolling(downloadId, finalFilename, manager);
            
        } catch (SecurityException e) {
            showDownloadError(getRequestFailedMessage());
        } catch (Exception e) {
            showDownloadError(getRequestFailedMessage());
        }
    }
    
    // Progress dialog reference
    private android.app.ProgressDialog progressDialog = null;
    
    /**
     * Show persistent download progress dialog
     */
    private void showDownloadProgress() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            
            progressDialog = new android.app.ProgressDialog(cordova.getActivity());
            progressDialog.setTitle(getDownloadInProgressText());
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        } catch (Exception e) {
        }
    }
    
    /**
     * Dismiss progress dialog and show success toast
     */
    private void showDownloadSuccess(String filename, String filePath) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // First dismiss progress dialog
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    
                    // Show simple success toast
                    android.widget.Toast.makeText(
                        cordova.getActivity(), 
                        getDownloadCompletedText(), 
                        android.widget.Toast.LENGTH_LONG
                    ).show();
                    
                    // Also send event to JavaScript if needed
                    JSONObject obj = new JSONObject();
                    obj.put("type", "downloadcomplete");
                    obj.put("filename", filename);
                    obj.put("filepath", filePath);
                    sendUpdate(obj, true);
                } catch (Exception e) {
                }
            }
        });
    }
    
    /**
     * Dismiss progress dialog and show download error message to user
     * Shows localized error message and auto-dismisses after 5 seconds
     */
    private void showDownloadError(String message) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // First dismiss progress dialog
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    
                    // Create an AlertDialog that auto-dismisses after 5 seconds
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(cordova.getActivity());
                      builder.setTitle(message)
                           .setCancelable(true);
                    
                    final android.app.AlertDialog errorDialog = builder.create();
                    errorDialog.show();
                    
                    // Auto dismiss after 5 seconds
                    android.os.Handler handler = new android.os.Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (errorDialog.isShowing()) {
                                errorDialog.dismiss();
                            }
                        }
                    }, 5000); // 5 seconds
                    
                    // Also send event to JavaScript if needed
                    JSONObject obj = new JSONObject();
                    obj.put("type", "downloaderror");
                    obj.put("message", message);
                    sendUpdate(obj, true);
                } catch (Exception e) {
                }
            }
        });
    }
    
    /**
     * Register a broadcast receiver to listen for download completion
     */
    private void registerDownloadCompleteListener(long downloadId, String filename) {        
        // Create a broadcast receiver for download completion
        android.content.BroadcastReceiver downloadReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                
                if (id == downloadId) {                    
                    // Download completed, get the file info
                    DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    if (manager != null) {
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(downloadId);
                        
                        Cursor cursor = null;
                        try {
                            cursor = manager.query(query);
                            if (cursor != null && cursor.moveToFirst()) {
                                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                                int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                                int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                                int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                                
                                if (statusIndex >= 0) {
                                    int status = cursor.getInt(statusIndex);
                                    
                                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                        String localUri = (uriIndex >= 0) ? cursor.getString(uriIndex) : null;
                                        String title = (titleIndex >= 0) ? cursor.getString(titleIndex) : filename;
                                                                                
                                        if (localUri != null) {
                                            // Show the enhanced dialog with options
                                            showDownloadSuccess(title, localUri);
                                        } else {
                                            // Show simple success message
                                            cordova.getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    android.widget.Toast.makeText(
                                                        cordova.getActivity(), 
                                                        getDownloadCompletedText() + ": " + title, 
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show();
                                                }
                                            });
                                        }
                                    } else if (status == DownloadManager.STATUS_FAILED) {
                                        int reason = (reasonIndex >= 0) ? cursor.getInt(reasonIndex) : -1;
                                        String errorMessage;
                                        if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) {
                                            errorMessage = getNoStorageMessage();
                                        } else {
                                            errorMessage = getRequestFailedMessage();
                                        }
                                        showDownloadError(errorMessage);
                                    }
                                }
                            }
                        } catch (Exception e) {
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                    
                    // Unregister the receiver
                    try {
                        context.unregisterReceiver(this);
                    } catch (Exception e) {
                    }
                }
            }
        };
        
        // Register the receiver
        try {
            android.content.IntentFilter filter = new android.content.IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            cordova.getActivity().registerReceiver(downloadReceiver, filter);
        } catch (Exception e) {
        }
    }
    
    /**
     * Backup polling mechanism to check download status
     */
    private void startDownloadPolling(long downloadId, String filename, DownloadManager manager) {
        Handler handler = new Handler();
        Runnable pollingRunnable = new Runnable() {
            int pollCount = 0;
            final int MAX_POLLS = 30; // Poll for max 30 seconds
            
            @Override
            public void run() {
                pollCount++;
                
                try {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    
                    Cursor cursor = manager.query(query);
                    if (cursor != null && cursor.moveToFirst()) {
                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (statusIndex >= 0) {
                            int status = cursor.getInt(statusIndex);
                            
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                LOG.d(LOG_TAG, "Polling detected successful download");
                                String localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                                cursor.close();
                                
                                // Small delay to ensure broadcast receiver had a chance to run
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (localUri != null) {
                                            showDownloadSuccess(filename, localUri);
                                        }
                                    }
                                }, 500);
                                return;
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                LOG.e(LOG_TAG, "Polling detected failed download");
                                cursor.close();
                                showDownloadError(getRequestFailedMessage());
                                return;
                            }
                        }
                        cursor.close();
                    }
                    
                    // Continue polling if not complete and under max polls
                    if (pollCount < MAX_POLLS) {
                        handler.postDelayed(this, 1000); // Poll every second
                    } else {
                        LOG.d(LOG_TAG, "Download polling timeout");
                        // Show error message when download times out (likely network issue)
                        cordova.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showDownloadError(getRequestFailedMessage());
                            }
                        });
                    }
                } catch (Exception e) {
                    LOG.e(LOG_TAG, "Error in download polling: " + e.getMessage());
                }
            }
        };
        
        // Start polling after 2 seconds
        handler.postDelayed(pollingRunnable, 2000);
    }

    private InAppBrowser getInAppBrowser() {
        return this;
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url the url to load.
     * @param features jsonObject
     */
    public String showWebPage(final String url, HashMap<String, String> features) {
        // Determine if we should hide the location bar.
        showLocationBar = true;
        showZoomControls = true;
        openWindowHidden = false;
        mediaPlaybackRequiresUserGesture = false;

        if (features != null) {
            String show = features.get(LOCATION);
            if (show != null) {
                showLocationBar = show.equals("yes") ? true : false;
            }
            if(showLocationBar) {
                String hideNavigation = features.get(HIDE_NAVIGATION);
                String hideUrl = features.get(HIDE_URL);
                if(hideNavigation != null) hideNavigationButtons = hideNavigation.equals("yes") ? true : false;
                if(hideUrl != null) hideUrlBar = hideUrl.equals("yes") ? true : false;
            }
            String zoom = features.get(ZOOM);
            if (zoom != null) {
                showZoomControls = zoom.equals("yes") ? true : false;
            }
            String hidden = features.get(HIDDEN);
            if (hidden != null) {
                openWindowHidden = hidden.equals("yes") ? true : false;
            }
            String hardwareBack = features.get(HARDWARE_BACK_BUTTON);
            if (hardwareBack != null) {
                hadwareBackButton = hardwareBack.equals("yes") ? true : false;
            } else {
                hadwareBackButton = DEFAULT_HARDWARE_BACK;
            }
            String mediaPlayback = features.get(MEDIA_PLAYBACK_REQUIRES_USER_ACTION);
            if (mediaPlayback != null) {
                mediaPlaybackRequiresUserGesture = mediaPlayback.equals("yes") ? true : false;
            }
            String fullScreen = features.get(FULL_SCREEN);
            if (fullScreen != null) {
                fullScreenFeature = fullScreen.equals("yes") ? true : false;
            }
            String cache = features.get(CLEAR_ALL_CACHE);
            if (cache != null) {
                clearAllCache = cache.equals("yes") ? true : false;
            } else {
                cache = features.get(CLEAR_SESSION_CACHE);
                if (cache != null) {
                    clearSessionCache = cache.equals("yes") ? true : false;
                }
            }
            String shouldPause = features.get(SHOULD_PAUSE);
            if (shouldPause != null) {
                shouldPauseInAppBrowser = shouldPause.equals("yes") ? true : false;
            }
            String wideViewPort = features.get(USER_WIDE_VIEW_PORT);
            if (wideViewPort != null ) {
                useWideViewPort = wideViewPort.equals("yes") ? true : false;
            }
            String closeButtonCaptionSet = features.get(CLOSE_BUTTON_CAPTION);
            if (closeButtonCaptionSet != null) {
                closeButtonCaption = closeButtonCaptionSet;
            }
            String closeButtonColorSet = features.get(CLOSE_BUTTON_COLOR);
            if (closeButtonColorSet != null) {
                closeButtonColor = closeButtonColorSet;
            }
            String leftToRightSet = features.get(LEFT_TO_RIGHT);
            leftToRight = leftToRightSet != null && leftToRightSet.equals("yes");

            String toolbarColorSet = features.get(TOOLBAR_COLOR);
            if (toolbarColorSet != null) {
                toolbarColor = android.graphics.Color.parseColor(toolbarColorSet);
            }
            String navigationButtonColorSet = features.get(NAVIGATION_COLOR);
            if (navigationButtonColorSet != null) {
                navigationButtonColor = navigationButtonColorSet;
            }
            String showFooterSet = features.get(FOOTER);
            if (showFooterSet != null) {
                showFooter = showFooterSet.equals("yes") ? true : false;
            }
            String footerColorSet = features.get(FOOTER_COLOR);
            if (footerColorSet != null) {
                footerColor = footerColorSet;
            }
            if (features.get(BEFORELOAD) != null) {
                beforeload = features.get(BEFORELOAD);
            }
            String fullscreenSet = features.get(FULLSCREEN);
            if (fullscreenSet != null) {
                fullscreen = fullscreenSet.equals("yes") ? true : false;
            }
        }

        final CordovaWebView thatWebView = this.webView;

        // Create dialog in new thread
        Runnable runnable = new Runnable() {

            private boolean inFullScreen = false;

            /**
             * Convert our DIP units to Pixels
             *
             * @return int
             */
            private int dpToPixels(int dipValue) {
                int value = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP,
                        (float) dipValue,
                        cordova.getActivity().getResources().getDisplayMetrics()
                );

                return value;
            }
            
            private void enableFullScreen(Activity activity, Window window) {
                inFullScreen = true;
                WindowManager.LayoutParams attrs = window.getAttributes();
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                window.setAttributes(attrs);
                if (android.os.Build.VERSION.SDK_INT >= 14)
                {
                    //noinspection all
                    int flags = View.SYSTEM_UI_FLAG_LOW_PROFILE;
                    if (android.os.Build.VERSION.SDK_INT >= 16)
                    {
                        flags = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;
                    }
                    window.getDecorView().setSystemUiVisibility(flags);
                }

            }

            private void disableFullScreen(Activity activity, Window window) {
                inFullScreen = false;
                WindowManager.LayoutParams attrs = window.getAttributes();
                attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                window.setAttributes(attrs);
                if (android.os.Build.VERSION.SDK_INT >= 14)
                {
                    //noinspection all
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }
            }

            private View createCloseButton(int id) {
                View _close;
                Resources activityRes = cordova.getActivity().getResources();

                if (closeButtonCaption != "") {
                    // Use TextView for text
                    TextView close = new TextView(cordova.getActivity());
                    close.setText(closeButtonCaption);
                    close.setTextSize(14);
                    if (closeButtonColor != "") close.setTextColor(android.graphics.Color.parseColor(closeButtonColor));
                    close.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    close.setPadding(this.dpToPixels(10), 0, this.dpToPixels(10), 0);
                    _close = close;
                }
                else {
                    ImageButton close = new ImageButton(cordova.getActivity());
                    int closeResId = activityRes.getIdentifier("ic_action_remove", "drawable", cordova.getActivity().getPackageName());
                    Drawable closeIcon = activityRes.getDrawable(closeResId);
                    if (closeButtonColor != "") close.setColorFilter(android.graphics.Color.parseColor(closeButtonColor));
                    close.setImageDrawable(closeIcon);
                    close.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    close.getAdjustViewBounds();

                    _close = close;
                }

                RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
                if (leftToRight) closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                else closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                _close.setLayoutParams(closeLayoutParams);
                _close.setBackground(null);

                _close.setContentDescription("Close Button");
                _close.setId(Integer.valueOf(id));
                _close.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        closeDialog();
                    }
                });

                return _close;
            }

            @SuppressLint("NewApi")
            public void run() {

                // CB-6702 InAppBrowser hangs when opening more than one instance
                if (dialog != null) {
                    dialog.dismiss();
                };

                // Let's create the main dialog
                dialog = new InAppBrowserDialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar_Fullscreen);
                dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    final View decor = dialog.getWindow().getDecorView();
                    decor.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
                        public void onSystemUiVisibilityChange(int visibility) {
                            new Handler().postDelayed(new Runnable() {
                                public void run(){
                                    if (inFullScreen)
                                    {
                                        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
                                    }
                                }
                            }, 3000);
                        }
                    });
                }
                
                if (fullScreenFeature)
                {
                    enableFullScreen(cordova.getActivity(), dialog.getWindow());
                }

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                if (fullscreen) {
                    dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                dialog.setCancelable(true);
                dialog.setInAppBroswer(getInAppBrowser());

                // Main container layout
                RelativeLayout main = new RelativeLayout(cordova.getActivity());
                RelativeLayout fullScreenMain = new RelativeLayout(cordova.getActivity());
                fullScreenMain.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                LinearLayout browserMain = new LinearLayout(cordova.getActivity());
                browserMain.setOrientation(LinearLayout.VERTICAL);
                browserMain.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

                // Toolbar layout
                RelativeLayout toolbar = new RelativeLayout(cordova.getActivity());
                //Please, no more black!
                toolbar.setBackgroundColor(toolbarColor);
                toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(TOOLBAR_HEIGHT)));
                toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2));
                if (leftToRight) {
                    toolbar.setHorizontalGravity(Gravity.LEFT);
                } else {
                    toolbar.setHorizontalGravity(Gravity.RIGHT);
                }
                toolbar.setVerticalGravity(Gravity.TOP);

                // Action Button Container layout
                RelativeLayout actionButtonContainer = new RelativeLayout(cordova.getActivity());
                RelativeLayout.LayoutParams actionButtonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                if (leftToRight) actionButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                else actionButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                actionButtonContainer.setLayoutParams(actionButtonLayoutParams);
                actionButtonContainer.setHorizontalGravity(Gravity.LEFT);
                actionButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);
                actionButtonContainer.setId(leftToRight ? Integer.valueOf(5) : Integer.valueOf(1));

                // Back button
                ImageButton back = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams backLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
                backLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
                back.setLayoutParams(backLayoutParams);
                back.setContentDescription("Back Button");
                back.setId(Integer.valueOf(2));
                Resources activityRes = cordova.getActivity().getResources();
                int backResId = activityRes.getIdentifier("ic_action_previous_item", "drawable", cordova.getActivity().getPackageName());
                Drawable backIcon = activityRes.getDrawable(backResId);
                if (navigationButtonColor != "") back.setColorFilter(android.graphics.Color.parseColor(navigationButtonColor));
                back.setBackground(null);
                back.setImageDrawable(backIcon);
                back.setScaleType(ImageView.ScaleType.FIT_CENTER);
                back.setPadding(0, this.dpToPixels(10), 0, this.dpToPixels(10));
                back.getAdjustViewBounds();

                back.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        goBack();
                    }
                });

                // Forward button
                ImageButton forward = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams forwardLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
                forwardLayoutParams.addRule(RelativeLayout.RIGHT_OF, 2);
                forward.setLayoutParams(forwardLayoutParams);
                forward.setContentDescription("Forward Button");
                forward.setId(Integer.valueOf(3));
                int fwdResId = activityRes.getIdentifier("ic_action_next_item", "drawable", cordova.getActivity().getPackageName());
                Drawable fwdIcon = activityRes.getDrawable(fwdResId);
                if (navigationButtonColor != "") forward.setColorFilter(android.graphics.Color.parseColor(navigationButtonColor));
                forward.setBackground(null);
                forward.setImageDrawable(fwdIcon);
                forward.setScaleType(ImageView.ScaleType.FIT_CENTER);
                forward.setPadding(0, this.dpToPixels(10), 0, this.dpToPixels(10));
                forward.getAdjustViewBounds();

                forward.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        goForward();
                    }
                });

                // Edit Text Box
                edittext = new EditText(cordova.getActivity());
                RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                textLayoutParams.addRule(RelativeLayout.RIGHT_OF, 1);
                textLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
                edittext.setLayoutParams(textLayoutParams);
                edittext.setId(Integer.valueOf(4));
                edittext.setSingleLine(true);
                edittext.setText(url);
                edittext.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                edittext.setImeOptions(EditorInfo.IME_ACTION_GO);
                edittext.setInputType(InputType.TYPE_NULL); // Will not except input... Makes the text NON-EDITABLE
                edittext.setOnKeyListener(new View.OnKeyListener() {
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        // If the event is a key-down event on the "enter" button
                        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                            navigate(edittext.getText().toString());
                            return true;
                        }
                        return false;
                    }
                });


                // Header Close/Done button
                int closeButtonId = leftToRight ? 1 : 5;
                View close = createCloseButton(closeButtonId);
                toolbar.addView(close);

                // Footer
                RelativeLayout footer = new RelativeLayout(cordova.getActivity());
                int _footerColor;
                if(footerColor != "") {
                    _footerColor = Color.parseColor(footerColor);
                } else {
                    _footerColor = android.graphics.Color.LTGRAY;
                }
                footer.setBackgroundColor(_footerColor);
                RelativeLayout.LayoutParams footerLayout = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(TOOLBAR_HEIGHT));
                footerLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                footer.setLayoutParams(footerLayout);
                if (closeButtonCaption != "") footer.setPadding(this.dpToPixels(8), this.dpToPixels(8), this.dpToPixels(8), this.dpToPixels(8));
                footer.setHorizontalGravity(Gravity.LEFT);
                footer.setVerticalGravity(Gravity.BOTTOM);

                View footerClose = createCloseButton(7);
                footer.addView(footerClose);


                // WebView
                inAppWebView = new VideoEnabledWebView(cordova.getActivity());
                inAppWebView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                inAppWebView.setId(Integer.valueOf(6));
                // File Chooser Implemented ChromeClient
                InAppChromeClient inAppChromeClient = new InAppChromeClient(thatWebView, browserMain, fullScreenMain) {
                    // For Android 5.0+
                    public boolean onShowFileChooser (WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams)
                    {
                        LOG.d(LOG_TAG, "File Chooser 5.0+");
                        // If callback exists, finish it.
                        if(mUploadCallback != null) {
                            mUploadCallback.onReceiveValue(null);
                        }
                        mUploadCallback = filePathCallback;

                        // Create File Chooser Intent
                        Intent content = new Intent(Intent.ACTION_GET_CONTENT);
                        content.addCategory(Intent.CATEGORY_OPENABLE);
                        content.setType("*/*");

                        // Run cordova startActivityForResult
                        cordova.startActivityForResult(InAppBrowser.this, Intent.createChooser(content, "Select File"), FILECHOOSER_REQUESTCODE);
                        return true;
                    }

                    // For Android 4.1+
                    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
                    {
                        LOG.d(LOG_TAG, "File Chooser 4.1+");
                        // Call file chooser for Android 3.0+
                        openFileChooser(uploadMsg, acceptType);
                    }

                    // For Android 3.0+
                    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType)
                    {
                        LOG.d(LOG_TAG, "File Chooser 3.0+");
                        Intent content = new Intent(Intent.ACTION_GET_CONTENT);
                        content.addCategory(Intent.CATEGORY_OPENABLE);

                        // run startActivityForResult
                        cordova.startActivityForResult(InAppBrowser.this, Intent.createChooser(content, "Select File"), FILECHOOSER_REQUESTCODE);
                    }

                };

                inAppChromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback() {
                    @Override
                    public void toggledFullscreen(boolean fullscreen)
                    {
                        // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:

                        Activity activity = cordova.getActivity();
                        Window window = dialog.getWindow();
                        if (fullscreen)
                        {
                            enableFullScreen(activity, window);
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                        }
                        else
                        {
                            if (!fullScreenFeature)
                            {
                                disableFullScreen(activity, window);
                            }
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                        }
                    }
                });
                inAppWebView.setWebChromeClient(inAppChromeClient);

                currentClient = new InAppBrowserClient(thatWebView, edittext, beforeload);
                inAppWebView.setWebViewClient(currentClient);
                
                // Set download listener for files that trigger download directly
                inAppWebView.setDownloadListener(new DownloadListener() {
                    @Override
                    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                        LOG.d(LOG_TAG, "DownloadListener triggered for: " + url);
                        LOG.d(LOG_TAG, "UserAgent: " + userAgent);
                        LOG.d(LOG_TAG, "ContentDisposition: " + contentDisposition);
                        LOG.d(LOG_TAG, "MimeType: " + mimeType);
                        LOG.d(LOG_TAG, "ContentLength: " + contentLength);
                        
                        // Check if downloads are allowed before handling
                        if (!areDownloadsAllowed()) {
                            LOG.d(LOG_TAG, "Download blocked by permission check in DownloadListener");
                            return;
                        }
                        
                        handleDownload(url, userAgent, contentDisposition, mimeType);
                    }
                });
                WebSettings settings = inAppWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);
                settings.setBuiltInZoomControls(showZoomControls);
                settings.setPluginState(android.webkit.WebSettings.PluginState.ON);

                // Add postMessage interface
                class JsObject {
                    @JavascriptInterface
                    public void postMessage(String data) {
                        try {
                            JSONObject obj = new JSONObject();
                            obj.put("type", MESSAGE_EVENT);
                            obj.put("data", new JSONObject(data));
                            sendUpdate(obj, true);
                        } catch (JSONException ex) {
                            LOG.e(LOG_TAG, "data object passed to postMessage has caused a JSON error.");
                        }
                    }
                }

                settings.setMediaPlaybackRequiresUserGesture(mediaPlaybackRequiresUserGesture);
                inAppWebView.addJavascriptInterface(new JsObject(), "cordova_iab");

                String overrideUserAgent = preferences.getString("OverrideUserAgent", null);
                String appendUserAgent = preferences.getString("AppendUserAgent", null);

                if (overrideUserAgent != null) {
                    settings.setUserAgentString(overrideUserAgent);
                }
                if (appendUserAgent != null) {
                    settings.setUserAgentString(settings.getUserAgentString() + " " + appendUserAgent);
                }

                //Toggle whether this is enabled or not!
                Bundle appSettings = cordova.getActivity().getIntent().getExtras();
                boolean enableDatabase = appSettings == null ? true : appSettings.getBoolean("InAppBrowserStorageEnabled", true);
                if (enableDatabase) {
                    String databasePath = cordova.getActivity().getApplicationContext().getDir("inAppBrowserDB", Context.MODE_PRIVATE).getPath();
                    settings.setDatabasePath(databasePath);
                    settings.setDatabaseEnabled(true);
                }
                settings.setDomStorageEnabled(true);

                if (clearAllCache) {
                    CookieManager.getInstance().removeAllCookie();
                } else if (clearSessionCache) {
                    CookieManager.getInstance().removeSessionCookie();
                }

                // Enable Thirdparty Cookies
                CookieManager.getInstance().setAcceptThirdPartyCookies(inAppWebView,true);

                inAppWebView.loadUrl(url);
                inAppWebView.setId(Integer.valueOf(6));
                inAppWebView.getSettings().setLoadWithOverviewMode(true);
                inAppWebView.getSettings().setUseWideViewPort(useWideViewPort);
                // Multiple Windows set to true to mitigate Chromium security bug.
                //  See: https://bugs.chromium.org/p/chromium/issues/detail?id=1083819
                inAppWebView.getSettings().setSupportMultipleWindows(true);
                inAppWebView.requestFocus();
                inAppWebView.requestFocusFromTouch();

                // Add the back and forward buttons to our action button container layout
                actionButtonContainer.addView(back);
                actionButtonContainer.addView(forward);

                // Add the views to our toolbar if they haven't been disabled
                if (!hideNavigationButtons) toolbar.addView(actionButtonContainer);
                if (!hideUrlBar) toolbar.addView(edittext);

                // Don't add the toolbar if its been disabled
                if (getShowLocationBar()) {
                    // Add our toolbar to our main view/layout
                    browserMain.addView(toolbar);
                }

                // Add our webview to our main view/layout
                RelativeLayout webViewLayout = new RelativeLayout(cordova.getActivity());
                webViewLayout.addView(inAppWebView);
                browserMain.addView(webViewLayout);

                // Don't add the footer unless it's been enabled
                if (showFooter) {
                    webViewLayout.addView(footer);
                }

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.MATCH_PARENT;

                main.addView(browserMain);
                main.addView(fullScreenMain);

                if (dialog != null) {
                    dialog.setContentView(main);
                    dialog.show();
                    dialog.getWindow().setAttributes(lp);
                }
                // the goal of openhidden is to load the url and not display it
                // Show() needs to be called to cause the URL to be loaded
                if (openWindowHidden && dialog != null) {
                    dialog.hide();
                }
            }
        };
        this.cordova.getActivity().runOnUiThread(runnable);
        return "";
    }

    /**
     * Create a new plugin success result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback) {
        sendUpdate(obj, keepCallback, PluginResult.Status.OK);
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     * @param status the status code to return to the JavaScript environment
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback, PluginResult.Status status) {
        if (callbackContext != null) {
            PluginResult result = new PluginResult(status, obj);
            result.setKeepCallback(keepCallback);
            callbackContext.sendPluginResult(result);
            if (!keepCallback) {
                callbackContext = null;
            }
        }
    }

    /**
     * Receive File Data from File Chooser
     *
     * @param requestCode the requested code from chromeclient
     * @param resultCode the result code returned from android system
     * @param intent the data from android file chooser
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        LOG.d(LOG_TAG, "onActivityResult");
        // If RequestCode or Callback is Invalid
        if(requestCode != FILECHOOSER_REQUESTCODE || mUploadCallback == null) {
            super.onActivityResult(requestCode, resultCode, intent);
            return;
        }
        mUploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
        mUploadCallback = null;
    }

    /**
     * The webview client receives notifications about appView
     */
    public class InAppBrowserClient extends WebViewClient {
        EditText edittext;
        CordovaWebView webView;
        String beforeload;

        /**
         * Constructor.
         *
         * @param webView
         * @param mEditText
         */
        public InAppBrowserClient(CordovaWebView webView, EditText mEditText, String beforeload) {
            this.webView = webView;
            this.edittext = mEditText;
            this.beforeload = beforeload;
        }

        /**
         * Override the URL that should be loaded
         *
         * Legacy (deprecated in API 24)
         * For Android 6 and below.
         *
         * @param webView
         * @param url
         */
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            return shouldOverrideUrlLoading(url, null);
        }

        /**
         * Override the URL that should be loaded
         *
         * New (added in API 24)
         * For Android 7 and above.
         *
         * @param webView
         * @param request
         */
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
            return shouldOverrideUrlLoading(request.getUrl().toString(), request.getMethod());
        }

        /**
         * Override the URL that should be loaded
         *
         * This handles a small subset of all the URIs that would be encountered.
         *
         * @param url
         * @param method
         */
        public boolean shouldOverrideUrlLoading(String url, String method) {
            LOG.d(LOG_TAG, "shouldOverrideUrlLoading called with URL: " + url + ", method: " + method);
            
            boolean override = false;
            boolean useBeforeload = false;
            String errorMessage = null;

            // Check if this is a downloadable file
            if (isDownloadableFile(url)) {
                LOG.d(LOG_TAG, "Detected download link: " + url);
                // Get the WebView's user agent and handle download
                String userAgent = inAppWebView.getSettings().getUserAgentString();
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(url));
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                LOG.d(LOG_TAG, "Starting download with mimeType: " + mimeType);
                handleDownload(url, userAgent, null, mimeType);
                return true; // Override the URL loading
            }

            if (beforeload.equals("yes") && method == null) {
                useBeforeload = true;
            } else if(beforeload.equals("yes")
                    //TODO handle POST requests then this condition can be removed:
                    && !method.equals("POST"))
            {
                useBeforeload = true;
            } else if(beforeload.equals("get") && (method == null || method.equals("GET"))) {
                useBeforeload = true;
            } else if(beforeload.equals("post") && (method == null || method.equals("POST"))) {
                //TODO handle POST requests
                errorMessage = "beforeload doesn't yet support POST requests";
            }

            // On first URL change, initiate JS callback. Only after the beforeload event, continue.
            if (useBeforeload) {
                if(sendBeforeLoad(url, method)) {
                    return true;
                }
            }

            if(errorMessage != null) {
                try {
                    LOG.e(LOG_TAG, errorMessage);
                    JSONObject obj = new JSONObject();
                    obj.put("type", LOAD_ERROR_EVENT);
                    obj.put("url", url);
                    obj.put("code", -1);
                    obj.put("message", errorMessage);
                    sendUpdate(obj, true, PluginResult.Status.ERROR);
                } catch(Exception e) {
                    LOG.e(LOG_TAG, "Error sending loaderror for " + url + ": " + e.toString());
                }
            }

            if (url.startsWith(WebView.SCHEME_TEL)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                    override = true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                }
            } else if (url.startsWith("geo:") || url.startsWith(WebView.SCHEME_MAILTO) || url.startsWith("market:") || url.startsWith("intent:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                    override = true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error with " + url + ": " + e.toString());
                }
            }
            // If sms:5551212?body=This is the message
            else if (url.startsWith("sms:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    // Get address
                    String address = null;
                    int parmIndex = url.indexOf('?');
                    if (parmIndex == -1) {
                        address = url.substring(4);
                    } else {
                        address = url.substring(4, parmIndex);

                        // If body, then set sms body
                        Uri uri = Uri.parse(url);
                        String query = uri.getQuery();
                        if (query != null) {
                            if (query.startsWith("body=")) {
                                intent.putExtra("sms_body", query.substring(5));
                            }
                        }
                    }
                    intent.setData(Uri.parse("sms:" + address));
                    intent.putExtra("address", address);
                    intent.setType("vnd.android-dir/mms-sms");
                    cordova.getActivity().startActivity(intent);
                    override = true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error sending sms " + url + ":" + e.toString());
                }
            }
            // Test for whitelisted custom scheme names like mycoolapp:// or twitteroauthresponse:// (Twitter Oauth Response)
            else if (!url.startsWith("http:") && !url.startsWith("https:") && url.matches("^[A-Za-z0-9+.-]*://.*?$")) {
                if (allowedSchemes == null) {
                    String allowed = preferences.getString("AllowedSchemes", null);
                    if(allowed != null) {
                        allowedSchemes = allowed.split(",");
                    }
                }
                if (allowedSchemes != null) {
                    for (String scheme : allowedSchemes) {
                        if (url.startsWith(scheme)) {
                            try {
                                JSONObject obj = new JSONObject();
                                obj.put("type", "customscheme");
                                obj.put("url", url);
                                sendUpdate(obj, true);
                                override = true;
                            } catch (JSONException ex) {
                                LOG.e(LOG_TAG, "Custom Scheme URI passed in has caused a JSON error.");
                            }
                        }
                    }
                }
            }

            return override;
        }

        private boolean sendBeforeLoad(String url, String method) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", BEFORELOAD);
                obj.put("url", url);
                if(method != null) {
                    obj.put("method", method);
                }
                sendUpdate(obj, true);
                return true;
            } catch (JSONException ex) {
                LOG.e(LOG_TAG, "URI passed in has caused a JSON error.");
            }
            return false;
        }

        /**
         * New (added in API 21)
         * For Android 5.0 and above.
         *
         * @param view
         * @param request
         */
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return shouldInterceptRequest(request.getUrl().toString(), super.shouldInterceptRequest(view, request), request.getMethod());
        }

        public WebResourceResponse shouldInterceptRequest(String url, WebResourceResponse response, String method) {
            return response;
        }

        /*
         * onPageStarted fires the LOAD_START_EVENT
         *
         * @param view
         * @param url
         * @param favicon
         */
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            String newloc = "";
            if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
                newloc = url;
            }
            else
            {
                // Assume that everything is HTTP at this point, because if we don't specify,
                // it really should be.  Complain loudly about this!!!
                LOG.e(LOG_TAG, "Possible Uncaught/Unknown URI");
                newloc = "http://" + url;
            }

            // Update the UI if we haven't already
            if (!newloc.equals(edittext.getText().toString())) {
                edittext.setText(newloc);
            }

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_START_EVENT);
                obj.put("url", newloc);
                sendUpdate(obj, true);
            } catch (JSONException ex) {
                LOG.e(LOG_TAG, "URI passed in has caused a JSON error.");
            }
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // Set the namespace for postMessage()
            injectDeferredObject("window.webkit={messageHandlers:{cordova_iab:cordova_iab}}", null);

            // CB-10395 InAppBrowser's WebView not storing cookies reliable to local device storage
            CookieManager.getInstance().flush();

            // https://issues.apache.org/jira/browse/CB-11248
            view.clearFocus();
            view.requestFocus();

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_STOP_EVENT);
                obj.put("url", url);

                sendUpdate(obj, true);
            } catch (JSONException ex) {
                LOG.d(LOG_TAG, "Should never happen");
            }
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_ERROR_EVENT);
                obj.put("url", failingUrl);
                obj.put("code", errorCode);
                obj.put("message", description);

                sendUpdate(obj, true, PluginResult.Status.ERROR);
            } catch (JSONException ex) {
                LOG.d(LOG_TAG, "Should never happen");
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_ERROR_EVENT);
                obj.put("url", error.getUrl());
                obj.put("code", 0);
                obj.put("sslerror", error.getPrimaryError());
                String message;
                switch (error.getPrimaryError()) {
                case SslError.SSL_DATE_INVALID:
                    message = "The date of the certificate is invalid";
                    break;
                case SslError.SSL_EXPIRED:
                    message = "The certificate has expired";
                    break;
                case SslError.SSL_IDMISMATCH:
                    message = "Hostname mismatch";
                    break;
                default:
                case SslError.SSL_INVALID:
                    message = "A generic error occurred";
                    break;
                case SslError.SSL_NOTYETVALID:
                    message = "The certificate is not yet valid";
                    break;
                case SslError.SSL_UNTRUSTED:
                    message = "The certificate authority is not trusted";
                    break;
                }
                obj.put("message", message);

                sendUpdate(obj, true, PluginResult.Status.ERROR);
            } catch (JSONException ex) {
                LOG.d(LOG_TAG, "Should never happen");
            }
            handler.cancel();
        }

        /**
         * On received http auth request.
         */
        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

            // Check if there is some plugin which can resolve this auth challenge
            PluginManager pluginManager = null;
            try {
                Method gpm = webView.getClass().getMethod("getPluginManager");
                pluginManager = (PluginManager)gpm.invoke(webView);
            } catch (NoSuchMethodException e) {
                LOG.d(LOG_TAG, e.getLocalizedMessage());
            } catch (IllegalAccessException e) {
                LOG.d(LOG_TAG, e.getLocalizedMessage());
            } catch (InvocationTargetException e) {
                LOG.d(LOG_TAG, e.getLocalizedMessage());
            }

            if (pluginManager == null) {
                try {
                    Field pmf = webView.getClass().getField("pluginManager");
                    pluginManager = (PluginManager)pmf.get(webView);
                } catch (NoSuchFieldException e) {
                    LOG.d(LOG_TAG, e.getLocalizedMessage());
                } catch (IllegalAccessException e) {
                    LOG.d(LOG_TAG, e.getLocalizedMessage());
                }
            }

            if (pluginManager != null && pluginManager.onReceivedHttpAuthRequest(webView, new CordovaHttpAuthHandler(handler), host, realm)) {
                return;
            }

            // By default handle 401 like we'd normally do!
            super.onReceivedHttpAuthRequest(view, handler, host, realm);
        }
    }
}
