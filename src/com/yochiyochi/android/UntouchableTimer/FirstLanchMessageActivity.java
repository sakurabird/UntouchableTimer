package com.yochiyochi.android.UntouchableTimer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class FirstLanchMessageActivity extends Activity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        // TODO Auto-generated method stub
        super.onCreate( savedInstanceState );

        setContentView( R.layout.firstlanchmessage );
    }

    public void showNext( View view ) {
        finish();
    }

}
