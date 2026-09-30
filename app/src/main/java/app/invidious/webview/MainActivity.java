package app.invidious.webview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * 极简 Invidious 客户端：用系统 WebView 加载任意 Invidious 实例。
 * 不依赖任何第三方库，纯 Android 框架实现。
 *
 * 用法：
 *  - 首次启动会弹出输入框，填写 Invidious 实例地址（默认填的是官方实例列表页）。
 *  - 在页面任意位置「长按」可随时修改实例地址（本地部署后填 http://电脑IP:3000）。
 *  - 返回键在网页历史内回退。
 */
public class MainActivity extends Activity {
    private static final String PREFS = "invidious_prefs";
    private static final String KEY_URL = "instance_url";
    // 默认指向 Invidious 官方「可用公共实例列表」，用户在列表里挑一个活的实例即可。
    private static final String DEFAULT_URL = "https://instances.invidious.io/";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

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
        webView.setWebViewClient(new WebViewClient());

        setContentView(webView);

        final SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String url = prefs.getString(KEY_URL, null);
        if (url == null || url.isEmpty()) {
            promptForUrl(prefs, DEFAULT_URL);
        } else {
            webView.loadUrl(url);
        }

        // 长按任意位置 -> 修改实例地址
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                promptForUrl(prefs, prefs.getString(KEY_URL, DEFAULT_URL));
                return true;
            }
        });
    }

    private void promptForUrl(final SharedPreferences prefs, String current) {
        final EditText input = new EditText(this);
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
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
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
