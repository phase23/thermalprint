package com.dantsu.thermalprinter;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.thermalprinter.async.AsyncEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncEscPosPrinter;
import com.dantsu.thermalprinter.async.AsyncUsbEscPosPrint;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.security.Provider;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyService extends Service {
    Context mContext;
    private final int TW0_SECONDS = 2000;
    public Handler handler;
    public  String mydevice;
    String responseBody;
    String printout;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                //.setSmallIcon(R.mipmap.app_icon)
                .setContentTitle("My Awesome App")
                .setContentText("Doing some work...")
                .setContentIntent(pendingIntent).build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startMyOwnForeground();
        }else {
            startForeground(1337, notification);
        }

        mContext=this;


    }

    @Override
    public void onStart(Intent intent, int startid) {
        //Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();

         mydevice = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);


        Log.d(TAG, "Value is: " + mydevice);
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://axcessdrivers-default-rtdb.firebaseio.com/");
        DatabaseReference restaurant = FirebaseDatabase.getInstance().getReference(mydevice);

        restaurant.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                String alert = dataSnapshot.child("waitingstatus").getValue(String.class);
                //boolean isSeen = ds.child("isSeen").getValue(Boolean.class);
                Log.d(TAG, "Value is: " + alert);
                //Toast.makeText(getApplicationContext(), "Value is:" + value + " Alert: " + alert, Toast.LENGTH_LONG).show();
                //Toast.makeText(getApplicationContext(), "changed : " + value, Toast.LENGTH_LONG).show();

                if(alert.equals("print")){

                    Toast.makeText(getApplicationContext(), "Sending to print queue", Toast.LENGTH_SHORT).show();

                    String getresponse = checkprintqueue();

                    getresponse = getresponse.trim();

                   if(getresponse.equals("noprint")) {
        //do nothing
                   }else {
                       Intent intent = new Intent("intentKey");
                       intent.putExtra("key", getresponse);
                       LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                      // printUsb();

                   }


                }else {


                        }



            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Quickserver Welcome.", error.toException());
            }
        });




/*
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {


                try {
                    checkneworder("https://getquickserve.com/barapp/printserver.php?test=1&action=checkorder");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                handler.postDelayed(this, TW0_SECONDS);
            }
        }, TW0_SECONDS);

 */

    }



    public String checkprintqueue() {



        String url = "http://getquickserve.com/barapp/printserver.php?print=1&deviceid="+ mydevice;

        Log.i("action url",url);

        OkHttpClient client = new OkHttpClient();


        // String contentType = fileSource.toURL().openConnection().getContentType();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("senddevice",mydevice )
                .build();
        Request request = new Request.Builder()

                .url(url)//your webservice url
                .post(requestBody)
                .build();
        try {
            //String responseBody;
            okhttp3.Response response = client.newCall(request).execute();
            // Response response = client.newCall(request).execute();
            if (response.isSuccessful()){
                Log.i("SUCC",""+response.message());

            }
            String resp = response.message();
            responseBody =  response.body().string();
            Log.i("respBody",responseBody);



            Log.i("MSG",resp);
        } catch (IOException e) {
            e.printStackTrace();
        }


        Log.i("action url",responseBody);
return  responseBody;


    }


    void checkneworder(String url) throws IOException{
        System.out.println("url " + url);
        Request request = new Request.Builder()
                .url(url)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(final okhttp3.Call call, IOException e) {
                        // Error
                        handler = new Handler();
                        Thread thread = new Thread() {
                            //runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // For the example, you can show an error dialog or a toast
                                // on the main UI thread
                            }
                        };

                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {

                        String outputthis = response.body().string();

                        printout = outputthis.trim();



                        if(outputthis.equals("noprint")) {
                            System.out.println("url out: " + outputthis);

                        }else {
                            printUsb();

                        }

                    }//end void

                });



    }


 /*==============================================================================================
    ===========================================USB PART=============================================
    ==============================================================================================*/

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyService.ACTION_USB_PERMISSION.equals(action)) {
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
                new Intent(MyService.ACTION_USB_PERMISSION),
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );
        IntentFilter filter = new IntentFilter(MyService.ACTION_USB_PERMISSION);
        registerReceiver(this.usbReceiver, filter);
        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
    }

        /*==============================================================================================
    ===================================ESC/POS PRINTER PART=========================================
    ==============================================================================================*/

    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter getAsyncEscPosPrinter(DeviceConnection printerConnection) {
        SimpleDateFormat format = new SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss");
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);

        String[] allorders = printout.split(Pattern.quote("@"));

        String lastelememt = allorders[allorders.length-1].trim();
        Toast.makeText(getApplicationContext(), "Print:" +  lastelememt, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "argS: " + lastelememt);


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
        String datepart ="[C]<u type='double'>c" + dedate + " " + detime + "</u>\n\n";
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
                "[R]<font size='tall'>TOTAL PRICEp :[R]" + totalprice +"</font>\n\n\n";



        String allparts = toppart + datepart + whocash + pritstation + linepart + listall + printgst + totalpriceline;

        return printer.addTextToPrint(
                allparts
        );
    }





























    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "Print Server";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)

                .setContentTitle("Print Serve")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

}
