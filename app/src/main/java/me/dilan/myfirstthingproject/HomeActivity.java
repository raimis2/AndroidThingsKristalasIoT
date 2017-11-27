package me.dilan.myfirstthingproject;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private Handler mHandler = new Handler();
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
    private DatabaseReference refBCM17;
    private DatabaseReference refBCM27;
    private DatabaseReference refBCM22;
    private DatabaseReference refBCM23;
    private DatabaseReference refBCM24;
    private DatabaseReference refBCM25;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initFirebase();
        getDataInit();
        mHandler.post(mInputCheckRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mInputCheckRunnable);
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
    }

    private Runnable mInputCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (bcm17 == null || bcm27 == null || bcm22 == null) {
                return;
            }
                setInputGPIOState(bcm17,refBCM17);
                setInputGPIOState(bcm27,refBCM27);
                setInputGPIOState(bcm22,refBCM22);
                mHandler.postDelayed(mInputCheckRunnable, delayMillis);
        }
    };

    private void setInputGPIOState(Gpio gpio,DatabaseReference refBCM){
        try {
            if (gpio.getValue()){
                refBCM.setValue(1);
            } else {
                refBCM.setValue(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            case "delay":
                delayMillis = Integer.parseInt(ds.getValue().toString());
              //  Log.d(TAG, "BCM27 state " +  delayMillis);
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
            case "delay":
                delayMillis = Integer.parseInt(ds.getValue().toString());
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
                try {
                    bcm17 = service.openGpio("BCM17");
                    configureInput(bcm17);
                    mDatabase.child("GPIO").child("BCM17").setValue(0);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM27":
                try {
                    bcm27 = service.openGpio("BCM27");
                    configureInput(bcm27);
                    mDatabase.child("GPIO").child("BCM27").setValue(0);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM22":
                try {
                    bcm22 = service.openGpio("BCM22");
                    configureInput(bcm22);
                    mDatabase.child("GPIO").child("BCM22").setValue(0);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
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
        //gpio.setActiveType(Gpio.ACTIVE_HIGH);

    }
    private void initFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        DatabaseReference refonline = mDatabase.child("Config").child("online");
        refonline.setValue(1);
        refonline.onDisconnect().setValue(0);

        refBCM17 = mDatabase.child("GPIO").child("BCM17");
        refBCM17.onDisconnect().setValue(0);
        refBCM27 = mDatabase.child("GPIO").child("BCM27");
        refBCM27.onDisconnect().setValue(0);
        refBCM22 = mDatabase.child("GPIO").child("BCM22");
        refBCM22.onDisconnect().setValue(0);

        refBCM23 = mDatabase.child("GPIO").child("BCM23");
        refBCM23.onDisconnect().setValue(0);
        refBCM24 = mDatabase.child("GPIO").child("BCM24");
        refBCM24.onDisconnect().setValue(0);
        refBCM25 = mDatabase.child("GPIO").child("BCM25");
        refBCM25.onDisconnect().setValue(0);

    }

}
