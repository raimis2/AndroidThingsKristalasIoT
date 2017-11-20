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
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private static long intervalBetweenBlinksMs = 1000;

    private Handler mHandler = new Handler();
    private Gpio bcm3;
    private Gpio bcm6;
    private PeripheralManagerService service = new PeripheralManagerService();

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mDatabase = FirebaseDatabase.getInstance().getReference();


        try {
            bcm3 = service.openGpio("BCM4");
            bcm3.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            bcm6 = service.openGpio("BCM6");
            bcm6.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            //  mHandler.post(mBlinkRunnable);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }

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
            bcm3.close();
            bcm6.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        } finally {
            bcm3 = null;
        }
    }

    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit Runnable if the GPIO is already closed
            if (bcm3 == null) {
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
                    updateGPIO(ds);
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
        boolean state;

        switch (ds.getKey()) {
            case "BCM3":
                if (bcm3 != null) {
                    if (Integer.parseInt(ds.getValue().toString()) == 0) {
                        //mode = Gpio.DIRECTION_OUT_INITIALLY_LOW;
                        state = false;
                    } else if (Integer.parseInt(ds.getValue().toString()) == 1) {
                        //mode = Gpio.DIRECTION_OUT_INITIALLY_HIGH;
                        state = true;
                    } else {
                        //mode = Gpio.DIRECTION_OUT_INITIALLY_LOW;
                        state = false;
                    }
                    try {
                        bcm3.setValue(state);
                    } catch (IOException e) {
                        Log.e(TAG, "Error on PeripheralIO API", e);
                    }
                   // Log.d(TAG, "State set to " + "BCM3");
                }
                break;
            case "BCM6":
                if (bcm6 != null) {
                    if (Integer.parseInt(ds.getValue().toString()) == 0) {
                        //mode = Gpio.DIRECTION_OUT_INITIALLY_LOW;
                        state = false;
                    } else if (Integer.parseInt(ds.getValue().toString()) == 1) {
                        //mode = Gpio.DIRECTION_OUT_INITIALLY_HIGH;
                        state = true;
                    } else {
                        //mode = Gpio.DIRECTION_OUT_INITIALLY_LOW;
                        state = false;
                    }
                    try {
                        bcm6.setValue(state);
                    } catch (IOException e) {
                        Log.e(TAG, "Error on PeripheralIO API", e);
                    }
                   // Log.d(TAG, "State set to " + "BCM6");
                }
                break;


        }


    }
}
