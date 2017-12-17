package me.dilan.myfirstthingproject;

/**
 * Created by Kristalas on 2017.12.10.
 */

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class History {

    public String sensor;
    public String time;
    public int value;

    public History(String sensor, String time, int value) {
        this.sensor = sensor;
        this.time = time;
        this.value = value;
    }

}
