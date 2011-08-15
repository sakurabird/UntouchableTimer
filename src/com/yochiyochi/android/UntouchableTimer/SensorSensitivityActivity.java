package com.yochiyochi.android.UntouchableTimer;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class SensorSensitivityActivity extends Activity implements SensorEventListener {
    static final String TAG = "SensorSensitivityActivity";
    {
        Log.d( TAG, "@@@---start---@@@" );
    }
    float pref_Sensor_Sensitivity;
    SensorManager sensorMgr;
    boolean hasSensor;
    static TextView tv_pref_Sensor_Sensitivity;
    static TextView tv_Sensor_Sensitivity;
    static TextView tv_message;

    Vibrator vibrator;

    // // TODO onResumeの定義を変更sensorを他のメソッドでも使いたいため
    // List< Sensor > sensors;

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.sensorsensitivity );

        sensorMgr = ( SensorManager ) getSystemService( SENSOR_SERVICE );
        hasSensor = false;
        tv_pref_Sensor_Sensitivity = ( TextView ) findViewById( R.id.pref_Sensor_Sensitivity ); // プリファレンスで設定した値を表示
        tv_Sensor_Sensitivity = ( TextView ) findViewById( R.id.Sensor_Sensitivity );
        tv_message = ( TextView ) findViewById( R.id.message );

        loadSetting();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // プリファレンスの値を読み込む
        loadSetting();

        // // TODO　ここで宣言せずクラス変数として定義
        List< Sensor > sensors = sensorMgr.getSensorList( Sensor.TYPE_PROXIMITY );
        if ( sensors.size() > 0 ) {
            // センサーリスナー開始
            Sensor sensor = sensors.get( 0 );
            hasSensor = sensorMgr.registerListener( this, sensor, SensorManager.SENSOR_DELAY_NORMAL );
        } else {
            // 近接センサーがついていないので、Toastでメッセージを出し、アプリを終了
            Toast.makeText( SensorSensitivityActivity.this, getResources().getText( R.string.message_error_no_sensor ), Toast.LENGTH_SHORT ).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // プリファレンスの値をセーブする
        saveSetting();

        // センサーリスナー終了
        if ( hasSensor ) {
            sensorMgr.unregisterListener( this );
            hasSensor = false;
        }
        tv_message.setText( "" );
    }

    // プリファレンスの値を読み込む
    private void loadSetting() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );

        Log.d( TAG, "loadSetting1" );
        String ps = pref.getString( ( String ) getResources().getText( R.string.pref_key_sensor_sensitivity ), "2.0" );
        pref_Sensor_Sensitivity = Float.parseFloat( ps );
        tv_pref_Sensor_Sensitivity.setText( Float.toString( pref_Sensor_Sensitivity ) ); // 近接センサーで「近い」
    }

    private void saveSetting() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );
        SharedPreferences.Editor edt = pref.edit();

        String ps = Float.toString( pref_Sensor_Sensitivity );
        edt.putString( ( String ) getResources().getText( R.string.pref_key_sensor_sensitivity ), ps );
        edt.commit();
    }

    @Override
    public void onAccuracyChanged( Sensor arg0, int arg1 ) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged( SensorEvent event ) {
        // TODO Auto-generated method stub
        if ( event.sensor.getType() == Sensor.TYPE_PROXIMITY ) {
            // この条件だと機種によってはセンサーの感度が悪く反応してくれないようなのでコメントアウトする
            Log.d( TAG, "onSensorChanged　event.values[ 0 ]=" + event.values[ 0 ] );
            if ( event.values[ 0 ] <= pref_Sensor_Sensitivity ) // 近接センサーで「近い」
            {
                tv_message.setText( getText( R.string.message_sensorcheck_ok ) );
            } else {
                tv_message.setText( getText( R.string.message_sensorcheck_ng ) );
            }

            // 手を近づけると0.0になる　遠ざけると7.になるIS03
            tv_Sensor_Sensitivity.setText( Float.toString( event.values[ 0 ] ) ); // 近接センサーで「近い」
        }

    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        // TODO Auto-generated method stub

        super.onCreateOptionsMenu( menu );
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.option_menu, menu );
        return true;
    }

    // メニューで何か選択された場合
    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        super.onOptionsItemSelected( item );
        switch ( item.getItemId() ) {
            case R.id.menu_setting:
                Intent settingIntent = new Intent( this, SettingActivity.class );
                startActivity( settingIntent );
                break;
        }
        return super.onOptionsItemSelected( item );
    }

}