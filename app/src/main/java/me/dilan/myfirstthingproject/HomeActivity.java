package me.dilan.myfirstthingproject;

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

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();

    private Handler mHandler = new Handler();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        getDataInit();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove pending blink Runnable from the handler.
        mHandler.removeCallbacks(mBlinkRunnable);
        // Close the Gpio pin.
        Log.i(TAG, "Closing LED GPIO pin");
        try {
            bcm4.close();
            bcm6.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        } finally {
            bcm4 = null;
            bcm6 = null;
        }
    }

    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit Runnable if the GPIO is already closed
            if (bcm4 == null) {
                return;
            }
        /*    try {
                // Toggle the GPIO state
                mLedGpio.setValue(!mLedGpio.getValue());
                Log.d(TAG, "State set to " + mLedGpio.getValue());

                // Reschedule the same runnable in {#intervalBetweenBlinksMs} milliseconds
                mHandler.postDelayed(mBlinkRunnable, intervalBetweenBlinksMs);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }*/
        }
    };


    private void getDataInit() {

        mDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    //updateGPIO(ds);
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
            case "BCM4":
                try {
                    bcm4.setValue(getState(ds.getValue()));
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                // Log.d(TAG, "State set to " + "BCM3");
                break;
            case "BCM5":
                try {
                    bcm5.setValue(getState(ds.getValue()));
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                 Log.d(TAG, "State set to " + "BCM5");
                break;
            case "BCM6":
                try {
                    bcm6.setValue(getState(ds.getValue()));
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                //    Log.d(TAG, "State set to " + "BCM6");
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
            case "BCM4":
                try {
                    bcm4 = service.openGpio("BCM4");
                    bcm4.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM5":
                try {
                    bcm5 = service.openGpio("BCM5");
                    bcm5.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
            case "BCM6":
                try {
                    bcm6 = service.openGpio("BCM6");
                    bcm6.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                    //Log.d(TAG, "INIT " + "BCM6");
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
                break;
        }
    }

}
