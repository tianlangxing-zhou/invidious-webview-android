package app.invidious.webview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 极简 Invidious 客户端：用系统 WebView 加载任意 Invidious 实例。
 * 界面：顶部操作栏（标题 + 实例/后退/前进/刷新）+ 加载进度条 + 错误提示面板。
 * 纯 Android 框架实现，无第三方依赖。
 *
 * 用法：
 *  - 首次启动弹出输入框，填写 Invidious 实例地址（默认填官方实例列表页）。
 *  - 顶部「实例」按钮或长按页面，可随时修改实例地址。
 *  - 本地部署后填 http://电脑局域网IP:3000。
 *  - 返回键在网页历史内回退。
 */
public class MainActivity extends Activity {
    private static final String PREFS = "invidious_prefs";
    private static final String KEY_URL = "instance_url";
    private static final String DEFAULT_URL = "https://instances.invidious.io/";

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorPanel;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        errorPanel = findViewById(R.id.errorPanel);
        tvError = findViewById(R.id.tvError);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);          // Invidious 是 Web 应用，必须开 JS
        ws.setDomStorageEnabled(true);          // 登录态 / 订阅依赖 localStorage
        ws.setDatabaseEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        ws.setMediaPlaybackRequiresUserGesture(false); // 允许网页内直接播视频

        // 让页面内链接在 WebView 内打开，而不是跳系统浏览器
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                errorPanel.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }

            // 仅主帧错误才提示，避免图片等子资源 404 误报
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    showError("无法加载页面\n错误码 " + error.getErrorCode());
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (request.isForMainFrame()) {
                    showError("页面返回错误码 " + errorResponse.getStatusCode() + "\n请检查实例地址");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        // 顶部栏按钮
        Button btnInstance = findViewById(R.id.btnInstance);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnForward = findViewById(R.id.btnForward);
        Button btnRefresh = findViewById(R.id.btnRefresh);
        Button btnRetry = findViewById(R.id.btnRetry);

        btnInstance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { promptForUrl(); }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { if (webView.canGoBack()) webView.goBack(); }
        });
        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { if (webView.canGoForward()) webView.goForward(); }
        });
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { loadCurrent(); }
        });
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { loadCurrent(); }
        });

        // 长按页面也能改地址
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) { promptForUrl(); return true; }
        });

        // 首次启动：没有保存地址则弹框
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String url = prefs.getString(KEY_URL, null);
        if (url == null || url.isEmpty()) {
            promptForUrl();
        } else {
            webView.loadUrl(url);
        }
    }

    private void loadCurrent() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String url = prefs.getString(KEY_URL, null);
        if (url != null && !url.isEmpty()) {
            errorPanel.setVisibility(View.GONE);
            webView.loadUrl(url);
        } else {
            promptForUrl();
        }
    }

    private void showError(String msg) {
        progressBar.setVisibility(View.GONE);
        errorPanel.setVisibility(View.VISIBLE);
        tvError.setText(msg);
    }

    private void promptForUrl() {
        final SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        final EditText input = new EditText(this);
        String current = prefs.getString(KEY_URL, DEFAULT_URL);
        input.setText(current);
        input.setSelection(current.length());

        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(40, 20, 40, 20);
        wrap.addView(input);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Invidious 实例地址");
        builder.setMessage("填写 Invidious 服务端地址。\n公共实例：从 https://instances.invidious.io/ 选一个；\n本地部署：http://电脑局域网IP:3000");
        builder.setView(wrap);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String u = input.getText().toString().trim();
                if (!u.isEmpty()) {
                    prefs.edit().putString(KEY_URL, u).apply();
                    webView.loadUrl(u);
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
        });
        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
