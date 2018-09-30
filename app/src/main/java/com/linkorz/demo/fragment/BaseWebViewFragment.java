package com.linkorz.demo.fragment;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.linkorz.demo.R;
import com.linkorz.demo.activity.MainActivity;
import com.linkorz.demo.net.JSInterfaceEntity;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by liangxl
 * Date: 17-8-11
 * Description:A placeholder fragment containing a simple view.
 * support webkit && chromium fullScreen-video-enable,JSInterface,textAutoSizing.
 * See the <a href="https://developer.chrome.com/multidevice/webview/overview"/>
 */

public class BaseWebViewFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static BaseWebViewFragment newInstance(int sectionNumber) {
        BaseWebViewFragment fragment = new BaseWebViewFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    private WebView mWebView;
    private View mCustomView;
    private int mOriginalSystemUiVisibility;
    private int mOriginalOrientation;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private Handler mHandler;

    public BaseWebViewFragment() {
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mWebView = (WebView) rootView.findViewById(R.id.fragment_main_webview);

        setUpWebViewDefaults(mWebView);

//        mWebView.loadUrl("file:///android_asset/www/index.html");
        mWebView.loadUrl("http://www.linkorz.xyz");
        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public Bitmap getDefaultVideoPoster() {
                if(getActivity() == null) {
                    return null;
                }

                return BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(),
                        R.drawable.video_poster);
            }

            @Override
            public void onShowCustomView(View view,
                                         WebChromeClient.CustomViewCallback callback) {
                // if a view already exists then immediately terminate the new one
                if (mCustomView != null) {
                    onHideCustomView();
                    return;
                }

                // 1. Stash the current state
                mCustomView = view;
                mOriginalSystemUiVisibility = getActivity().getWindow().getDecorView().getSystemUiVisibility();
                mOriginalOrientation = getActivity().getRequestedOrientation();

                // 2. Stash the custom view callback
                mCustomViewCallback = callback;

                // 3. Add the custom view to the view hierarchy
                FrameLayout decor = (FrameLayout) getActivity().getWindow().getDecorView();
                decor.addView(mCustomView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));


                // 4. Change the state of the window
                getActivity().getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_IMMERSIVE);
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                // 1. Remove the custom view
                FrameLayout decor = (FrameLayout) getActivity().getWindow().getDecorView();
                decor.removeView(mCustomView);
                mCustomView = null;

                // 2. Restore the state to it's original form
                getActivity().getWindow().getDecorView()
                        .setSystemUiVisibility(mOriginalSystemUiVisibility);
                getActivity().setRequestedOrientation(mOriginalOrientation);

                // 3. Call the custom view callback
                mCustomViewCallback.onCustomViewHidden();
                mCustomViewCallback = null;

            }

        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    /**
     * Convenience method to set some generic defaults for a
     * given WebView
     *
     * @param webView
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setUpWebViewDefaults(WebView webView) {

        mWebView.setBackgroundColor(Color.parseColor("#3498db"));

        mWebView.addJavascriptInterface(
                new JSInterfaceEntity(getActivity().getApplicationContext()),
                "SampleApp");

        WebSettings settings = webView.getSettings();

        // Enable Javascript
        settings.setJavaScriptEnabled(true);

        // Use WideViewport and Zoom out if there is no viewport defined
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // Enable pinch to zoom without the zoom buttons
        settings.setBuiltInZoomControls(true);

        // Allow use of Local Storage
        settings.setDomStorageEnabled(true);

        WebSettings.LayoutAlgorithm layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING;
        }
        settings.setLayoutAlgorithm(layoutAlgorithm);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            // Hide the zoom controls for HONEYCOMB+
            settings.setDisplayZoomControls(false);
        }

        // Enable remote debugging via chrome://inspect
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // Force links and redirects to open in the WebView instead of in a browser
        webView.setWebViewClient(new WebViewClient());

    }

    /**
     * This method is designed to hide how Javascript is injected into
     * the WebView.
     *
     * In KitKat the new evaluateJavascript method has the ability to
     * give you access to any return values via the ValueCallback object.
     *
     * The String passed into onReceiveValue() is a JSON string, so if you
     * execute a javascript method which return a javascript object, you can
     * parse it as valid JSON. If the method returns a primitive value, it
     * will be a valid JSON object, but you should use the setLenient method
     * to true and then you can use peek() to test what kind of object it is,
     *
     * @param javascript
     */
    public void loadJavascript(String javascript) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // In KitKat+ you should use the evaluateJavascript method
            mWebView.evaluateJavascript(javascript, new ValueCallback<String>() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onReceiveValue(String s) {
                    JsonReader reader = new JsonReader(new StringReader(s));

                    // Must set lenient to parse single values
                    reader.setLenient(true);

                    try {
                        if(reader.peek() != JsonToken.NULL) {
                            if(reader.peek() == JsonToken.STRING) {
                                String msg = reader.nextString();
                                if(msg != null) {
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            msg, Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.e("TAG", "MainActivity: IOException", e);
                    } finally {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                }
            });
        } else {
            /**
             * For pre-KitKat+ you should use loadUrl("javascript:<JS Code Here>");
             * To then call back to Java you would need to use addJavascriptInterface()
             * and have your JS call the interface
             **/
            mWebView.loadUrl("javascript:"+javascript);
        }
    }

    public boolean goBack() {
        if(!mWebView.canGoBack()) {
            return false;
        }

        mWebView.goBack();
        return true;
    }
}