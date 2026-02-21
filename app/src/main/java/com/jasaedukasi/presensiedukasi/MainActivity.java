package com.jasaedukasi.presensiedukasi;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WebAppPrefs";
    private static final String KEY_URL = "url";

    private WebView webView;
    private ProgressBar progressBar;
    private boolean isDesktopMode = true; // Default to desktop mode
    private boolean isLandscapeMode = true; // Default to landscape
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        webView = (WebView) findViewById(R.id.webview);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Ini akan mengabaikan galat SSL. Diperlukan untuk beberapa sertifikat (seperti Let's Encrypt)
                // pada perangkat Android yang lebih lama. Gunakan dengan hati-hati.
                handler.proceed();
            }
        });

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Set initial mode
        updateWebViewMode();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            String url = prefs.getString(KEY_URL, getString(R.string.app_url));
            webView.loadUrl(url);
            toggleFullscreen();
        }
    }

    private void updateWebViewMode() {
        WebSettings webSettings = webView.getSettings();
        if (isDesktopMode) {
            webSettings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
            webSettings.setUseWideViewPort(false);
            webSettings.setLoadWithOverviewMode(false);
        } else {
            // A null user agent string will cause the WebView to use the system default.
            webSettings.setUserAgentString(null);
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem desktopModeItem = menu.findItem(R.id.action_desktop_mode);
        if (desktopModeItem != null) {
            desktopModeItem.setChecked(isDesktopMode);
        }

        MenuItem landscapeModeItem = menu.findItem(R.id.action_landscape_mode);
        if (landscapeModeItem != null) {
            landscapeModeItem.setChecked(isLandscapeMode);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_reload) {
            webView.reload();
            return true;
        } else if (item.getItemId() == R.id.action_restart) {
            restartApp();
            return true;
        } else if (item.getItemId() == R.id.action_close) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_fullscreen) {
            toggleFullscreen();
            return true;
        } else if (item.getItemId() == R.id.action_check_connection) {
            checkConnection();
            return true;
        } else if (item.getItemId() == R.id.action_check_network) {
            checkNetwork();
            return true;
        } else if (item.getItemId() == R.id.action_check_resolution) {
            checkResolution();
            return true;
        } else if (item.getItemId() == R.id.action_set_url) {
            showSetUrlDialog();
            return true;
        } else if (item.getItemId() == R.id.action_desktop_mode) {
            isDesktopMode = !isDesktopMode;
            item.setChecked(isDesktopMode);
            updateWebViewMode();
            webView.reload(); // Reload the page to apply the new settings
            return true;
        } else if (item.getItemId() == R.id.action_landscape_mode) {
            isLandscapeMode = !isLandscapeMode;
            item.setChecked(isLandscapeMode);
            setRequestedOrientation(isLandscapeMode ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void showSetUrlDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set URL");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(webView.getUrl());
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url = input.getText().toString();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_URL, url);
                editor.apply();
                webView.loadUrl(url);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


    private void restartApp() {
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }

    private void toggleFullscreen() {
        if ((getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
        }
    }

    private void checkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        String message = isConnected ? "Koneksi internet tersedia" : "Tidak ada koneksi internet";
        new AlertDialog.Builder(this)
            .setTitle("Cek Koneksi")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void checkNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        String message;
        if (activeNetwork != null) {
            String typeName = activeNetwork.getTypeName();
            message = "Jaringan terhubung: " + typeName;
        } else {
            message = "Tidak ada jaringan terhubung";
        }
        new AlertDialog.Builder(this)
            .setTitle("Cek Jaringan")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void checkResolution() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        String message = "Resolusi: " + width + "x" + height;
        new AlertDialog.Builder(this)
            .setTitle("Cek Resolusi")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        boolean isFullscreen = (getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
        if (isFullscreen) {
            toggleFullscreen();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Keluar Aplikasi")
                    .setMessage("Apakah Anda yakin ingin menutup aplikasi?")
                    .setPositiveButton("Ya", (dialog, which) -> finish())
                    .setNegativeButton("Tidak", null)
                    .show();
        }
    }
}
