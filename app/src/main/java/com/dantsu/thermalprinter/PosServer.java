package com.dantsu.thermalprinter;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.thermalprinter.async.AsyncEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncEscPosPrinter;
import com.dantsu.thermalprinter.async.AsyncUsbEscPosPrint;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class PosServer extends AppCompatActivity {
WebView webView;
    AlertDialog dialog;
    String printout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos_server);

        String deviceId = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        getSupportActionBar().hide();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);




        webView = (WebView) findViewById(R.id.web);
        webView.addJavascriptInterface(new WebAppInterface(this), "android");

        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAppCacheEnabled(false);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        //webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        //WebView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://getquickserve.com/barapp/index.php" );

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }
        });


        webView.setWebViewClient(new WebViewClient() {

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error){

                handler.proceed();

            }


            @Override
            public void onPageFinished(WebView view, String url) {
                //Toast.makeText(getApplicationContext(), url + "\n\n", Toast.LENGTH_LONG).show();
                Log.d("WebView", url);

                if (url.equals("https://getquickserve.com/barapp/index.php")) {
                    Toast.makeText(getApplicationContext(), "Preparing your dasboard", Toast.LENGTH_LONG).show();

                }



                super.onPageFinished(view, url);


            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                //Toast.makeText(getApplicationContext(), "AJAX" + url, Toast.LENGTH_LONG).show();
                return null;
            }


            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

        });





    }


    // Handling the received Intents for the "my-integer" event
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            printout = intent.getStringExtra("key");
            System.out.println("url out2: " + printout );
            // Toast.makeText(getApplicationContext(), "Over here:" + printout, Toast.LENGTH_LONG).show();

            //printUsb();


        }

    };

    @Override
    public void onBackPressed() {

    }


    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (PosServer.ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {
                            new AsyncUsbEscPosPrint(
                                    context,
                                    new AsyncEscPosPrint.OnPrintFinished() {
                                        @Override
                                        public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                                            Log.e("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : An error occurred !");
                                        }

                                        @Override
                                        public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                                            Log.i("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : Print is finished !");
                                        }
                                    }
                            )
                                    .execute(getAsyncEscPosPrinter(new UsbConnection(usbManager, usbDevice)));
                        }
                    }
                }
            }
        }
    };

    public void printUsb() {
        UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);

        if (usbConnection == null || usbManager == null) {
            new AlertDialog.Builder(this)
                    .setTitle("USB Connection")
                    .setMessage("No USB printer found.")
                    .show();
            return;
        }

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(PosServer.ACTION_USB_PERMISSION),
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );
        IntentFilter filter = new IntentFilter(PosServer.ACTION_USB_PERMISSION);
        registerReceiver(this.usbReceiver, filter);
        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
    }


    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter getAsyncEscPosPrinter(DeviceConnection printerConnection) {
        SimpleDateFormat format = new SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss");
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);




        String[] allorders = printout.split(Pattern.quote("@"));

        String lastelememt = allorders[allorders.length-1].trim();
        Toast.makeText(getApplicationContext(), "Print:" +  lastelememt, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "argP: " + lastelememt);


        String lner = allorders[0];
        String[] linez = lner.split("~");
        String ordernumber = linez[2];
        String dedate = linez[3];
        String detime = linez[4];
        String totalprice = linez[7];
        String cashier = linez[0];
        String workstation = linez[11];
        String gst = linez[12];


        int itemcount = allorders.length;
        String eachline;
        String toppart = "[L]\n[C]<u><font size='big'>ORDER N°" + ordernumber + "</font></u>\n\n";
        String datepart ="[C]<u type='double'>x" + dedate + " " + detime + "</u>\n\n";
        String whocash ="[L]<u type='double'>" + cashier + "</u\n";
        String pritstation ="[L]<u type='double'>" + workstation + " </u>\n";
        String linepart = "[C]================================\n\n";
        String listall = "";

        for (int i = 0; i < itemcount; i++) {
            eachline = allorders[i];
            String[] eachitem = eachline.split("~");
            String itemsold = eachitem[5];
            String itemprice = eachitem[6];

            listall = listall + "[L]<b>" + itemsold + "</b>[R]" + itemprice + "\n\n";


        }

        String printgst = "[L]<b>GST </b>[R]" + gst + "\n\n";

        String totalpriceline = "\n[C]================================\n" +
                "[R]<font size='tall'>TOTAL PRICEx :[R]" + totalprice +"</font>\n\n\n";



        String allparts = toppart + datepart + whocash + pritstation + linepart + listall + printgst + totalpriceline;

        return printer.addTextToPrint(
                allparts
        );
    }




    public class WebAppInterface {
        Context mContext;

        /**
         * Instantiate the interface and set the context
         */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /**
         * Show a toast from the web page
         */
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }
    }


}