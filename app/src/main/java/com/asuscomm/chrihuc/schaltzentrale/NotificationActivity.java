package com.asuscomm.chrihuc.schaltzentrale;

import android.app.Activity;
import android.os.Bundle;

public class NotificationActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Now finish, which will drop the user in to the activity that was at the top
        //  of the task stack
        finish();
    }
}
