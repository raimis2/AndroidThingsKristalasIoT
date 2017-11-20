/**
 * Created by Chatura Dilan Perera on 16/12/2016.
 */
package me.dilan.myfirstthingproject;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class GPIO {

    private int BCM6;

    public GPIO() {

    }

    public int getBCM6() {
        return BCM6;
    }
    public void setBCM6(int BCM6) {
        this.BCM6 = BCM6;
    }
}
