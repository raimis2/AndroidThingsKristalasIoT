package me.dilan.myfirstthingproject;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private Handler mHandler = new Handler();
    private MCP3008 mMCP3008;
    private long delayMillis;
    private Gpio bcm4;
    private Gpio bcm5;
    private Gpio bcm6;
    private Gpio bcm12;
    private Gpio bcm16;
    private Gpio bcm17;
    private Gpio bcm22;
    private Gpio bcm23;
    private Gpio bcm24;
    private Gpio bcm25;
    private Gpio bcm26;
    private Gpio bcm27;

    private PeripheralManagerService service = new PeripheralManagerService();

    private DatabaseReference mDatabase;
    private DatabaseReference refAuto;
    private long malfunctionOffset;
    private DatabaseReference refMalfunction;
    private DatabaseReference refBCM17;
    private DatabaseReference refBCM27;
    private DatabaseReference refBCM22;
    private DatabaseReference refBCM23;
    private DatabaseReference refBCM24;
    private DatabaseReference refBCM25;

    private int h1channel = 0x0;
    private int h2channel = 0x1;
    private int h3channel = 0x2;
    private int h4channel = 0x3;
    private int h5channel = 0x4;
    private int h6channel = 0x5;
    private int h7channel = 0x6;
    private int h8channel = 0x7;

    private int h1Desired = 0;
    private int h2Desired = 0;
    private int h3Desired = 0;

    private DatabaseReference refSMH1;
    private DatabaseReference refSMH2;
    private DatabaseReference refSMH3;
    boolean isRunning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initFirebase();
        initMCP3008();
        getDataInit();
        mHandler.post(mInputCheckRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mInputCheckRunnable);
        mHandler.removeCallbacks(mAutoModeRunnable);
        // Close the Gpio pin.
        Log.i(TAG, "Closing GPIO pin");
        try {
            bcm4.close();
            bcm5.close();
            bcm6.close();
            bcm24.close();
            bcm25.close();
            bcm23.close();
            bcm17.close();
            bcm27.close();
            bcm22.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        } finally {
            bcm4 = null;
            bcm5 = null;
            bcm6 = null;
            bcm24 = null;
            bcm25 = null;
            bcm23 = null;
            bcm17 = null;
            bcm27 = null;
            bcm22 = null;
        }
        if (mMCP3008 != null) {
            mMCP3008.unregister();
        }
    }


    private void plantMonitor(int analogChannel, int desiredHumidity){
        long tStart = 0; // = System.currentTimeMillis();
        boolean watering = false;
        try {
            if (getADCPercentage(mMCP3008.readAdc(analogChannel)) <= desiredHumidity) {
                watering = true;
                // it is dry, open switch
               // activateSwitch1();

                //activate Pump
                tStart = System.currentTimeMillis();
                activatePump();

            } else {
                watering = false;
                //switch off switch & pump
                deactivatePump();
                deactivateSwitch1();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (watering) {
            long tEnd = System.currentTimeMillis();
            long tDelta = tEnd - tStart;
            //check time elapsed since start of the watering
            if (tDelta >= malfunctionOffset) {
                refMalfunction.setValue(1);
            }
            // double elapsedSeconds = tDelta / 1000.0;
        }

    }
    private Runnable mInputCheckRunnable = new Runnable() {
        String formattedDate;
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        Calendar calendar = Calendar.getInstance();
        ;

        @Override
        public void run() {
            AlarmManager am = (AlarmManager)getContext().getSystemService(Context.ALARM_SERVICE);
            am.setTimeZone("Europe/Riga");
            formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
            updateFirebaseHumidity(h1channel, refSMH1);
            updateFirebaseHumidity(h2channel, refSMH2);
            updateFirebaseHumidity(h3channel, refSMH3);
            mDatabase.child("SM").child("timestamp").setValue(formattedDate);


            mHandler.postDelayed(mInputCheckRunnable, delayMillis);
        }
    };



    private Runnable mAutoModeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                isRunning = true;
                if (bcm23 == null || bcm24 == null || bcm25 == null) {
                    refMalfunction.setValue(1);
                    return;
                }
                plantMonitor(h1channel, h1Desired);
                //plantMonitor(h2channel, h2Desired);
              //  plantMonitor(h2channel, h3Desired);

            } catch (Exception e) {
                e.printStackTrace();
                isRunning = false;
            }

            mHandler.postDelayed(mAutoModeRunnable, delayMillis);
        }
    };

    private void getDataInit() {
        mDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    initGPIO(ds);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    updateGPIO(ds);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void updateGPIO(DataSnapshot ds) {
        switch (ds.getKey()) {
            case "auto":
                if (getState(ds.getValue())) {
                    if (!isRunning) {
                        mHandler.post(mAutoModeRunnable);
                        //  Log.d(TAG, "Pradedam Begt");
                    }
                } else {
                    mHandler.removeCallbacks(mAutoModeRunnable);
                    isRunning = false;
                    //Log.d(TAG, "PAgavo Miantai !!!!!!!!!!!!!!!!!!!!!!!");
                }
                break;
            case "malfunction":
                if (getState(ds.getValue())) {
                    finish();
                    System.exit(0);// alt + f4 + table_flip
                }
                break;
            case "malfunc_offset":
                malfunctionOffset = Integer.parseInt(ds.getValue().toString());
                break;
            case "delay":
                delayMillis = Integer.parseInt(ds.getValue().toString());
                //  Log.d(TAG, "BCM27 state " +  delayMillis);
                break;
            case "h1_desired":
                h1Desired = Integer.parseInt(ds.getValue().toString());
                break;
            case "h2_desired":
                h2Desired = Integer.parseInt(ds.getValue().toString());
                break;
            case "h3_desired":
                h3Desired = Integer.parseInt(ds.getValue().toString());
                break;
            case "BCM4":
                try {
                    bcm4.setValue(getState(ds.getValue()));
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM5":
                try {
                    bcm5.setValue(getState(ds.getValue()));
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                //     Log.d(TAG, "State set to " + "BCM5");
                break;
            case "BCM6":
                try {
                    bcm6.setValue(getState(ds.getValue()));
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM24":
                try {
                    bcm24.setValue(getState(ds.getValue()));
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM25":
                try {
                    bcm25.setValue(getState(ds.getValue()));
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM23":
                try {
                    bcm23.setValue(getState(ds.getValue()));
                    Log.d(TAG, "Pradedam Begt " + getState(ds.getValue()));
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
        }
    }

    private boolean getState(Object gpioState) {
        if (Integer.parseInt(gpioState.toString()) == 0) {
            return false;
        } else if (Integer.parseInt(gpioState.toString()) == 1) {
            return true;
        } else {
            return false;
        }
    }

    private void initGPIO(DataSnapshot ds) {
        switch (ds.getKey()) {
            case "auto":
                if (getState(ds.getValue())) {
                    if (!isRunning) {
                        mHandler.post(mAutoModeRunnable);
                        //  Log.d(TAG, "Pradedam Begt");
                    }
                } else {
                    mHandler.removeCallbacks(mAutoModeRunnable);
                    isRunning = false;
                    //Log.d(TAG, "PAgavo Miantai !!!!!!!!!!!!!!!!!!!!!!!");
                }
                break;
            case "malfunction":
                if (getState(ds.getValue())) {
                    finish();
                    System.exit(0);
                }
                break;
            case "malfunc_offset":
                malfunctionOffset = Integer.parseInt(ds.getValue().toString());
                break;
            case "delay":
                delayMillis = Integer.parseInt(ds.getValue().toString());
                break;
            case "h1_desired":
                h1Desired = Integer.parseInt(ds.getValue().toString());
                break;
            case "h2_desired":
                h2Desired = Integer.parseInt(ds.getValue().toString());
                break;
            case "h3_desired":
                h3Desired = Integer.parseInt(ds.getValue().toString());
                break;
            case "BCM4":
                try {
                    bcm4 = service.openGpio("BCM4");
                    configureOutput(bcm4);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM5":
                try {
                    bcm5 = service.openGpio("BCM5");
                    configureOutput(bcm5);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM6":
                try {
                    bcm6 = service.openGpio("BCM6");
                    configureOutput(bcm6);
                    //Log.d(TAG, "INIT " + "BCM6");
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM24":
                try {
                    bcm24 = service.openGpio("BCM24");
                    configureOutput(bcm24);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM25":
                try {
                    bcm25 = service.openGpio("BCM25");
                    configureOutput(bcm25);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM23":
                try {
                    bcm23 = service.openGpio("BCM23");
                    configureOutput(bcm23);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM17":
              /*  try {
                    bcm17 = service.openGpio("BCM17");
                    configureInput(bcm17);
                    mDatabase.child("GPIO").child("BCM17").setValue(0);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }*/
                break;
            case "BCM27":
              /*  try {
                    bcm27 = service.openGpio("BCM27");
                    configureInput(bcm27);
                    mDatabase.child("GPIO").child("BCM27").setValue(0);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }*/
                break;
            case "BCM22":
              /*  try {
                    bcm22 = service.openGpio("BCM22");
                    configureInput(bcm22);
                    mDatabase.child("GPIO").child("BCM22").setValue(0);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }*/
                break;
        }
    }

    public void configureInput(Gpio gpio) throws IOException {
        // Initialize the pin as an input
        gpio.setDirection(Gpio.DIRECTION_IN);
    }

    public void configureOutput(Gpio gpio) throws IOException {
        // Initialize the pin as an input
        gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
// High voltage is considered active
        gpio.setActiveType(Gpio.ACTIVE_LOW);
    }

    private void initFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        DatabaseReference refonline = mDatabase.child("Config").child("online");
        refonline.setValue(1);
        refonline.onDisconnect().setValue(0);

        refAuto = mDatabase.child("Config").child("auto");
        refAuto.onDisconnect().setValue(0);

        refMalfunction = mDatabase.child("Config").child("malfunction");

        refBCM23 = mDatabase.child("GPIO").child("BCM23");
        refBCM23.onDisconnect().setValue(0);
        refBCM24 = mDatabase.child("GPIO").child("BCM24");
        refBCM24.onDisconnect().setValue(0);
        refBCM25 = mDatabase.child("GPIO").child("BCM25");
        refBCM25.onDisconnect().setValue(0);

        refSMH1 = mDatabase.child("SM").child("H1");
        refSMH1.onDisconnect().setValue(0);
        refSMH2 = mDatabase.child("SM").child("H2");
        refSMH2.onDisconnect().setValue(0);
        refSMH3 = mDatabase.child("SM").child("H3");
        refSMH3.onDisconnect().setValue(0);
    }

    private void initMCP3008() {
        try {
            mMCP3008 = new MCP3008("BCM12", "BCM16", "BCM20", "BCM21");
            mMCP3008.register();
        } catch (IOException e) {
            Log.e("MCP3008", "MCP initialization exception occurred: " + e.getMessage());
        }
    }
    private void activateSwitch1(){
        try {
            if (!bcm24.getValue()) {
                bcm24.setValue(true);
                //mark Firebase, that switch is active
                refBCM24.setValue(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void activateSwitch2(){
        try {
            if (!bcm25.getValue()) {
                bcm25.setValue(true);
                //mark Firebase, that switch is active
                refBCM25.setValue(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void activatePump(){
        try {
            if (!bcm23.getValue()) {
                bcm23.setValue(true);
                //tStart = System.currentTimeMillis();
                //mark Firebase, that pump is active
                refBCM23.setValue(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deactivateSwitch1(){
        try {
            if (bcm24.getValue()) bcm24.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        refBCM24.setValue(0);
    }
    private void deactivateSwitch2(){
        try {
            if (bcm25.getValue()) bcm25.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        refBCM25.setValue(0);
    }
    private void deactivatePump(){
        try {
            if (bcm23.getValue()) bcm23.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        refBCM23.setValue(0);
    }
    private int getADCPercentage(int valueADC){
        int analogValue = 0;
        final int maxPercentage = 100;
        final int sensorOffset = 200;
        final int sensorMaxValue = 1023;
        int percentageValue;
        if (valueADC <= sensorOffset) {
            percentageValue = maxPercentage;
        } else {
            percentageValue = maxPercentage - (Math.round(analogValue * maxPercentage / (sensorMaxValue - sensorOffset)));
        }
        return  percentageValue;
    }
    private void updateFirebaseHumidity(int channel, DatabaseReference refSMH) {
        int analogValue = 0;
        final int maxPercentage = 100;
        final int sensorOffset = 200;
        final int sensorMaxValue = 1023;
        int percentageValue;
        try {
            analogValue = mMCP3008.readAdc(channel) - sensorOffset;
            Log.d(TAG, "Procentai " + mMCP3008.readAdc(channel));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (analogValue <= sensorOffset) {
            percentageValue = maxPercentage;
        } else {
            percentageValue = maxPercentage - (Math.round(analogValue * maxPercentage / (sensorMaxValue - sensorOffset)));
            Log.d(TAG, "Procentai " + percentageValue);
        }
//        try {
        //          Log.d(TAG, "Procentai" + percentageValue + " " + mMCP3008.readAdc(channel));
        //    } catch (IOException e) {
        //      e.printStackTrace();
        //}
        refSMH.setValue(percentageValue);
    }

}
