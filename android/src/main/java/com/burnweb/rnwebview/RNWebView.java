package com.burnweb.rnwebview;

import android.annotation.SuppressLint;

import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.graphics.Bitmap;
import android.os.Build;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.EventDispatcher;

class RNWebView extends WebView implements LifecycleEventListener {

    private final EventDispatcher mEventDispatcher;
    private final RNWebViewManager mViewManager;

    private String charset = "UTF-8";
    private String baseUrl = "file:///";
    private String injectedJavaScript = null;
    private boolean allowUrlRedirect = false;
    private String customSchemeForDeepLink = null;

    protected class EventWebClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url.startsWith("http://") || url.startsWith("https://")) {
              return false;
            } else {
              Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url)); 
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              view.getContext().startActivity(intent);   
              return true;   
            } 
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mEventDispatcher.dispatchEvent(new NavigationStateChangeEvent(getId(), SystemClock.uptimeMillis(), view.getTitle(), false, url, view.canGoBack(), view.canGoForward()));

            if(RNWebView.this.getInjectedJavaScript() != null) {
                view.loadUrl("javascript:(function() {\n" + RNWebView.this.getInjectedJavaScript() + ";\n})();");
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mEventDispatcher.dispatchEvent(new NavigationStateChangeEvent(getId(), SystemClock.uptimeMillis(), view.getTitle(), true, url, view.canGoBack(), view.canGoForward()));
        }
    }

    protected class CustomWebChromeClient extends WebChromeClient {

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            getModule().showAlert(url, message, result);
            return true;
        }

        // For Android 4.1+
        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            getModule().startFileChooserIntent(uploadMsg, acceptType);
        }

        // For Android 5.0+
        @SuppressLint("NewApi")
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            return getModule().startFileChooserIntent(filePathCallback, fileChooserParams.createIntent());
        }


    }

    protected class GeoWebChromeClient extends CustomWebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }
    }

    public RNWebView(RNWebViewManager viewManager, ThemedReactContext reactContext) {
        super(reactContext);

        mViewManager = viewManager;
        mEventDispatcher = reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();

        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setBuiltInZoomControls(false);
        this.getSettings().setDomStorageEnabled(true);
        this.getSettings().setGeolocationEnabled(false);
        this.getSettings().setPluginState(WebSettings.PluginState.ON);
        this.getSettings().setAllowFileAccess(true);
        this.getSettings().setAllowFileAccessFromFileURLs(true);
        this.getSettings().setAllowUniversalAccessFromFileURLs(true);
        this.getSettings().setLoadsImagesAutomatically(true);
        this.getSettings().setBlockNetworkImage(false);
        this.getSettings().setBlockNetworkLoads(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        this.setWebViewClient(new EventWebClient());

        this.setWebChromeClient(getCustomClient());
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return this.charset;
    }

    public void setAllowUrlRedirect(boolean a) {
        this.allowUrlRedirect = a;
    }

    public boolean getAllowUrlRedirect() {
        return this.allowUrlRedirect;
    }

    public void setInjectedJavaScript(String injectedJavaScript) {
        this.injectedJavaScript = injectedJavaScript;
    }

    public String getCustomSchemeForDeepLink() {
        return customSchemeForDeepLink;
    }

    public void setCustomSchemeForDeepLink(String customSchemeForDeepLink) {
        this.customSchemeForDeepLink = customSchemeForDeepLink;
    }

    public String getInjectedJavaScript() {
        return this.injectedJavaScript;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public CustomWebChromeClient getCustomClient() {
        return new CustomWebChromeClient();
    }

    public GeoWebChromeClient getGeoClient() {
        return new GeoWebChromeClient();
    }

    public RNWebViewModule getModule() {
        return mViewManager.getPackage().getModule();
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        destroy();
    }

    @Override
    public void onDetachedFromWindow() {
        this.loadDataWithBaseURL(this.getBaseUrl(), "<html></html>", "text/html", this.getCharset(), null);
        super.onDetachedFromWindow();
    }

}
