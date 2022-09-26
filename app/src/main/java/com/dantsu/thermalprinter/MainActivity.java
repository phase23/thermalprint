package com.dantsu.thermalprinter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.dantsu.thermalprinter.async.AsyncBluetoothEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncEscPosPrinter;
import com.dantsu.thermalprinter.async.AsyncTcpEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncUsbEscPosPrint;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity {
    TextView devicip;
    String printout;
    String thismydevice;
    Button start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       // Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));


       getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("intentKey"));








        devicip = (TextView)findViewById(R.id.device);
        thismydevice = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);


        devicip.setText(thismydevice);


        FirebaseDatabase database = FirebaseDatabase.getInstance("https://axcessdrivers-default-rtdb.firebaseio.com/");
        DatabaseReference restaurant = FirebaseDatabase.getInstance().getReference(thismydevice);
        restaurant.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild("waitingstatus")) {
                    // Exist! Do whatever.
                } else {
                    // Don't exist! Do something.
                    restaurant.child("waitingstatus").setValue("waiting");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed, how to handle?

            }

        });


        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();


            StrictMode.setThreadPolicy(policy);
            //your codes here

        }

        boolean connected = isConnected();

        /*
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
        } else {
            connected = false;
        }

         */



        if(!connected) {
            Toast.makeText(getApplicationContext(),"Check Internet & Restart App",Toast.LENGTH_LONG).show();
            Intent nointernet = new Intent(MainActivity.this, Nointernet.class);
            startActivity(nointernet);


        }else {

            Intent i = new Intent(this, MyService.class);
            this.startService(i);



            start = (Button)findViewById(R.id.start);

            start.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View view) {

                                                  Intent startup = new Intent(MainActivity.this, PosServer.class);
                                                  startActivity(startup);

                                              }
            });



            /*
            Button updateport = (Button) this.findViewById(R.id.updatedevice);
            EditText portno = (EditText) this.findViewById(R.id.devicenumber);

            updateport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String thisport = portno.getText().toString();


                    Toast.makeText(getApplicationContext(), "Device Updated ", Toast.LENGTH_SHORT).show();
                    portno.setText("");
                    String responseBody;
                    String url = "http://getquickserve.com/barapp/updateprinter.php?sentport=" + thisport + "&deviceid="+ thismydevice;

                    Log.i("action url",url);

                    OkHttpClient client = new OkHttpClient();


                     String contentType = fileSource.toURL().openConnection().getContentType();

                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("senddevice",thismydevice )
                            .build();
                    Request request = new Request.Builder()

                            .url(url)//your webservice url
                            .post(requestBody)
                            .build();
                    try {
                        String responseBody;
                        okhttp3.Response response = client.newCall(request).execute();
                         Response response = client.newCall(request).execute();
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








                }
            });




            Button button = (Button) this.findViewById(R.id.button_bluetooth_browse);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    browseBluetoothDevice();
                }
            });
            button = (Button) findViewById(R.id.button_bluetooth);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    printBluetooth();
                }
            });
            button = (Button) this.findViewById(R.id.button_usb);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    printUsb();
                }
            });
            button = (Button) this.findViewById(R.id.button_tcp);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    printTcp();
                }
            });


             */
        }

    }


    /*==============================================================================================
    ======================================BLUETOOTH PART============================================
    ==============================================================================================*/

    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case MainActivity.PERMISSION_BLUETOOTH:
                case MainActivity.PERMISSION_BLUETOOTH_ADMIN:
                case MainActivity.PERMISSION_BLUETOOTH_CONNECT:
                case MainActivity.PERMISSION_BLUETOOTH_SCAN:
                    //this.printBluetooth();
                    break;
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        // This registers messageReceiver to receive messages.

    }

    public boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        return ( networkInfo != null && networkInfo.isConnectedOrConnecting());
    }


    // Handling the received Intents for the "my-integer" event
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
             printout = intent.getStringExtra("key");
            System.out.println("url out: " + printout );
           // Toast.makeText(getApplicationContext(), "Over here:" + printout, Toast.LENGTH_LONG).show();

            printUsb();


        }

    };





    /*
    private BluetoothConnection selectedDevice;

    public void browseBluetoothDevice() {
        final BluetoothConnection[] bluetoothDevicesList = (new BluetoothPrintersConnections()).getList();

        if (bluetoothDevicesList != null) {
            final String[] items = new String[bluetoothDevicesList.length + 1];
            items[0] = "Default printer";
            int i = 0;
            for (BluetoothConnection device : bluetoothDevicesList) {
                items[++i] = device.getDevice().getName();
            }

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
            alertDialog.setTitle("Bluetooth printer selection");
            alertDialog.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    int index = i - 1;
                    if (index == -1) {
                        selectedDevice = null;
                    } else {
                        selectedDevice = bluetoothDevicesList[index];
                    }
                    Button button = (Button) findViewById(R.id.button_bluetooth_browse);
                    button.setText(items[i]);
                }
            });

            AlertDialog alert = alertDialog.create();
            alert.setCanceledOnTouchOutside(false);
            alert.show();

        }
    }






    /*==============================================================================================
    ===========================================USB PART=============================================
    ==============================================================================================*/

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MainActivity.ACTION_USB_PERMISSION.equals(action)) {
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
            new Intent(MainActivity.ACTION_USB_PERMISSION),
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );
        IntentFilter filter = new IntentFilter(MainActivity.ACTION_USB_PERMISSION);
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
        String lner = allorders[0];
        String[] linez = lner.split("~");
        String ordernumber = linez[2];
        String dedate = linez[3];
        String detime = linez[4];
        String totalprice = linez[7];
        String cashier = linez[0];
        String workstation = linez[11];
        String gst = linez[12];
        String subtt = linez[13];


        int itemcount = allorders.length;
        String eachline;
        String toppart = "[L]\n[C]<u><font size='big'>ORDER N°" + ordernumber + "</font></u>\n\n";
        String datepart ="[C]<u type='double'>" + dedate + " " + detime + "</u>\n\n";
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

        String linr = "===========";
        String absline = "[L]<b> </b>[R]" + linr + "\n\n";

        String printsubtt = "[L]<b>Subtotal </b>[R] <b>" + subtt + "</b>\n\n";
        String printgst = "[L]<b>GST </b>[R]" + gst + "\n\n";
        String totalpriceline = "\n[C]================================\n" +
                                  "[R]<font size='tall'>TOTAL PRICE :[R]" + totalprice +"</font>\n\n\n";



        String allparts = toppart + datepart + whocash + pritstation + linepart + listall + printsubtt + printgst + totalpriceline;

        return printer.addTextToPrint(
                allparts
        );
    }
}
