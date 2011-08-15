package com.yochiyochi.android.UntouchableTimer;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

/***
 * アンタッチャブルタイマー
 * 
 * @author sakura_fish
 * 
 */
public class UntouchableTimerActivity extends Activity implements SensorEventListener {

    static final String TAG = "UntouchableTimerActivity";
    {
        Log.d( TAG, "@@@---start---@@@" );

        tv_text = "00:00:00";
    }

    static TextView tv;
    static TextView tv_message1;
    static TextView tv_sensor_message;
    static Context mContext;
    static String tv_text;

    SpeechRecognizer rec;
    SensorManager sensorMgr;
    boolean hasSensor;

    Vibrator vibrator;
    MediaPlayer sensorcatch;

    AudioManager am;
    SeekBar ringVolSeekBar;
    TextView ringVolText;

    String pref_version;
    String pref_sound;
    boolean pref_vibrator;
    float pref_Sensor_Sensitivity;

    @Override
    public void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );
        setContentView( R.layout.main );

        // 音声認識
        rec = SpeechRecognizer.createSpeechRecognizer( getApplicationContext() );
        rec.setRecognitionListener( new speechListenerAdp() );

        sensorMgr = ( SensorManager ) getSystemService( SENSOR_SERVICE );
        hasSensor = false;
        mContext = this;
        tv = ( TextView ) findViewById( R.id.CountdownTimer );
        tv.setTextColor( Color.BLACK );
        tv.setText( tv_text );

        tv_message1 = ( TextView ) findViewById( R.id.message1 );
        tv_message1.setText( "" );
        tv_sensor_message = ( TextView ) findViewById( R.id.sensor_message );
        vibrator = ( Vibrator ) getSystemService( VIBRATOR_SERVICE );
        sensorcatch = MediaPlayer.create( mContext, R.raw.sensorcatch );

        // インストール後に一度だけ表示する画面のためにプリファレンスの値を読み込む
        loadSetting();
        if ( pref_version == "" ) {
            showFirstLanchDialog();
        }

        // 自動的に画面ロックしないようにする
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

    }

    @Override
    protected void onResume() {
        super.onResume();
        // プリファレンスの値を読み込む
        loadSetting();

        // アラーム音量
        am = ( AudioManager ) getSystemService( Context.AUDIO_SERVICE );
        int ringVolume = am.getStreamVolume( AudioManager.STREAM_MUSIC ); // 音量の取得
        ringVolSeekBar = ( SeekBar ) findViewById( R.id.ringVolSeekBar ); // 音量シークバー
        ringVolSeekBar.setMax( am.getStreamMaxVolume( AudioManager.STREAM_MUSIC ) ); // 最大音量の設定

        ringVolText = ( TextView ) findViewById( R.id.ringVolText ); // 音量TextView
        ringVolText.setText( "Volume:" + ringVolume ); // TextViewに設定値を表示
        am.setStreamVolume( AudioManager.STREAM_MUSIC, ringVolume, 0 ); // 着信音量設定
        ringVolSeekBar.setProgress( ringVolume ); // 音量をSeekBarにセット

        ringVolSeekBar.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
            public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser ) {
                // TODO Auto-generated method stub
                ringVolText.setText( "Volume:" + progress ); // TextViewに設定値を表示
                am.setStreamVolume( AudioManager.STREAM_MUSIC, progress, 0 ); // 着信音量設定
                ringVolSeekBar.setProgress( progress ); // 音量をSeekBarにセット
            }

            public void onStartTrackingTouch( SeekBar seekBar ) {
                // TODO Auto-generated method stub

            }

            public void onStopTrackingTouch( SeekBar seekBar ) {
                // TODO Auto-generated method stub

            }
        } );

        // 画面メッセージ
        tv_sensor_message.setText( R.string.message_waiting );

        List< Sensor > sensors = sensorMgr.getSensorList( Sensor.TYPE_PROXIMITY );
        if ( sensors.size() > 0 ) {
            // センサーリスナー開始
            Sensor sensor = sensors.get( 0 );
            hasSensor = sensorMgr.registerListener( this, sensor, SensorManager.SENSOR_DELAY_NORMAL );
        } else {
            // 近接センサーがついていないので、Toastでメッセージを出し、アプリを終了
            Toast.makeText( UntouchableTimerActivity.this, getResources().getText( R.string.message_error_no_sensor ), Toast.LENGTH_SHORT ).show();
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
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        // センサーリスナー終了
        if ( hasSensor ) {
            sensorMgr.unregisterListener( this );
            hasSensor = false;
        }
        // サービスのストップ(カウントダウン中断)
        stopTimerService();

        // 自動的に画面ロックしないようにするのを解除
        getWindow().clearFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

        // MediaPlayerのリソースを開放
        sensorcatch.release();
        // バイブレーションを止める
        vibrator.cancel();
        // 音声認識開放
        rec.destroy();
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

        // 通常メッセージをセットしておく
        tv_sensor_message.setText( R.string.message_waiting );

        switch ( item.getItemId() ) {
            case R.id.menu_setting:
                Intent settingIntent = new Intent( this, SettingActivity.class );
                startActivity( settingIntent );
                break;
            case R.id.menu_help:
                Intent intent = new Intent( mContext, HelpActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                startActivity( intent );
                break;
            case R.id.menu_about:
                showAboutDialog();
                break;
            case R.id.menu_sensor_sensitivity:
                Intent sensorIntent = new Intent( mContext, SensorSensitivityActivity.class );
                sensorIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                startActivity( sensorIntent );
                break;
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onAccuracyChanged( Sensor sensor, int accuracy ) {
    }

    @Override
    // センサーを感知した時
    public void onSensorChanged( SensorEvent event ) {
        if ( event.sensor.getType() == Sensor.TYPE_PROXIMITY ) {
            // プリファレンスに設定されたセンサー感度以下の場合、近接センサー反応したとみなす
            if ( event.values[ 0 ] < pref_Sensor_Sensitivity ) {
                // 画面メッセージ
                tv_sensor_message.setText( R.string.message_wait_sensor_changed );
                // 音を出す
                sensorcatch.start();
                // 音が鳴り終わるのを待って次の処理に行く。
                sensorcatch.setOnCompletionListener( new OnCompletionListener() {

                    public void onCompletion( MediaPlayer mp ) {
                        // TODO Auto-generated method stub
                        // 音声認識スタート
                        startSpeechRecognizer();
                    }
                } );

            }
        }
    }

    /***
     * 音声認識スタート
     * 
     */
    private void startSpeechRecognizer() {

        // サービスのストップ(カウントダウン中断)
        stopTimerService();
        tv.setText( showTime( 0 ) );

        // SpeechRecognizer
        rec.startListening( RecognizerIntent.getVoiceDetailsIntent( getApplicationContext() ) );

        // ヴァイブレーションさせる
        vibrator.vibrate( 30 );
    }

    /***
     * タイマー処理サービスストップ
     * 
     */
    private void stopTimerService() {
        Intent intentTimer = new Intent( mContext, TimerService.class );
        mContext.stopService( intentTimer );
    }

    /***
     * プリファレンスの値を読み込む
     * 
     */
    private void loadSetting() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );

        // アプリのバージョン情報
        pref_version = pref.getString( "app_version", "" );
        Log.d( TAG, "loadSetting pref_version=" + pref_version );

        // メニュー設定項目（サウンド・バイブレーション）
        pref_sound = pref.getString( ( String ) getResources().getText( R.string.pref_key_sound ), "" );
        if ( pref_sound == null )
            pref_sound = "2";
        else if ( pref_sound.equals( "" ) )
            pref_sound = "2";
        // String[] sounds = getResources().getStringArray(R.array.entries);
        // //これが選ばれしサウンド
        // String selected_sound = sounds[Integer.parseInt(pref_sound) - 1];
        pref_vibrator = pref.getBoolean( ( String ) getResources().getText( R.string.pref_key_vibrator ), true );

        // 近接センサーの閾値
        String ps = pref.getString( ( String ) getResources().getText( R.string.pref_key_sensor_sensitivity ), "3.0" );
        pref_Sensor_Sensitivity = Float.parseFloat( ps );
    }

    /***
     * プリファレンスの値を保存する
     * 
     */
    private void saveSetting() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );
        SharedPreferences.Editor edt = pref.edit();

        // アプリのバージョン情報
        pref_version = getVersionNumber( "", this );
        Log.d( TAG, "saveSetting pref_version=" + pref_version );
        edt.putString( "app_version", pref_version );

        // メニュー設定項目（サウンド・バイブレーション）
        edt.putString( ( String ) getResources().getText( R.string.pref_key_sound ), pref_sound );
        edt.putBoolean( ( String ) getResources().getText( R.string.pref_key_vibrator ), pref_vibrator );

        // 近接センサーの閾値
        String ps = Float.toString( pref_Sensor_Sensitivity );
        edt.putString( ( String ) getResources().getText( R.string.pref_key_sensor_sensitivity ), ps );

        edt.commit();
    }

    /***
     * アプリのバージョン情報を取得
     * 
     * @param prefix
     * @param context
     * @return
     */
    public static String getVersionNumber( String prefix, Context context ) {
        String versionName = prefix;
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo( context.getPackageName(), PackageManager.GET_META_DATA );
            versionName += info.versionName;
        } catch ( NameNotFoundException e ) {
            versionName += "0";
        }
        return versionName;
    }

    /***
     * 画面のカウント更新
     * 
     * @param timeSeconds
     * @return
     */
    static String showTime( long timeSeconds ) {
        Log.d( TAG, "showTime timeSeconds=" + timeSeconds );
        int HH = ( int ) ( timeSeconds / 3600 );
        int MM = ( int ) ( timeSeconds % 3600 );
        int SS = MM % 60;
        MM /= 60;
        Log.d( TAG, "showTime HH=" + HH + " MM=" + MM + " SS=" + SS );

        tv_text = String.format( "%02d:%02d:%02d", HH, MM, SS );

        return tv_text;
    }

    /***
     * カウントダウン処理
     * 
     * @param counter
     */
    public static void onTimerChanged( long counter ) {
        tv.setText( showTime( counter ) );
    }

    /***
     * アプリの説明ダイアログ表示処理
     * 
     */
    private void showAboutDialog() {
        TextView t = new TextView( this );

        t.setAutoLinkMask( Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS );
        t.setText( String.format( ( String ) getResources().getText( R.string.app_dlg_format ), getResources().getText( R.string.app_version ), getResources().getText( R.string.app_release ), getResources().getText( R.string.app_author ), getResources().getText( R.string.app_author_mail ) ) );

        t.setTextSize( 18f );
        t.setTextColor( 0xFFCCCCCC );
        t.setLinkTextColor( 0xFF9999FF );
        t.setPadding( 20, 8, 20, 8 );
        AlertDialog.Builder dlg = new AlertDialog.Builder( this ).setTitle( getResources().getText( R.string.app_name ) ).setIcon( R.drawable.icon ).setView( t ).setCancelable( true );
        dlg.create().show();
    }

    /***
     * インストール後一度だけ表示する説明画面表示処理
     * 
     */
    private void showFirstLanchDialog() {
        Intent intent = new Intent( mContext, FirstLanchMessageActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        startActivity( intent );
    }

    /***
     * SpeechRecognizerのリスナークラス
     * 
     */
    private class speechListenerAdp implements RecognitionListener {

        @Override
        public void onResults( Bundle results ) {

            ArrayList< String > strList = results.getStringArrayList( SpeechRecognizer.RESULTS_RECOGNITION ); // 音声認識結果を取得
            Log.d( TAG, "onResult Arraylist:" + strList );

            int result = SpeechAnalyzer.speechToSecond( strList ); // 音声認識結果から秒数値を取得する、この値をタイマーにセットする

            Log.d( TAG, "onResult result:" + result );
            if ( result != 0 ) {
                tv_sensor_message.setText( R.string.message_timer_start );
                // タイマーサービススタート
                Intent intent = new Intent( mContext, TimerService.class );
                intent.putExtra( "counter", result );
                startService( intent );

                // 画面メッセージ
                tv_sensor_message.setText( R.string.message_cancel );

            } else {
                // 取得した文字列が数値じゃなければエラーメッセージ
                tv_sensor_message.setText( R.string.message_error_analyze );
                tv.setText( showTime( 0 ) );
                // 音を出す
                sensorcatch.start();
                sensorcatch.setOnCompletionListener( new OnCompletionListener() {

                    public void onCompletion( MediaPlayer mp ) {
                        // TODO Auto-generated method stub
                        // 音声認識スタート
                        tv_sensor_message.setText( R.string.message_talk_to_phone );
                        startSpeechRecognizer();
                    }
                } );

            }
        }

        @Override
        public void onBeginningOfSpeech() {
            // 画面メッセージ
            tv_sensor_message.setText( R.string.message_wait_recognize );
        }

        // 発声の終了
        @Override
        public void onEndOfSpeech() {
            // 実は、onResultで、音声認識の結果が返ってくるよりも先に、onEndOfSpeechが発生する
            // 画面メッセージ
            tv_sensor_message.setText( R.string.message_wait_analyze );
        }

        @Override
        public void onError( int error ) {
            // 画面メッセージ
            tv_sensor_message.setText( R.string.message_error_recognize );
            tv.setText( showTime( 0 ) );

            switch ( error ) {
                case SpeechRecognizer.ERROR_AUDIO: // このエラーが、どういうケースで発生するか、未確認
                    break;

                case SpeechRecognizer.ERROR_NO_MATCH: // このエラーが出ると、音声認識が停止してしまうので、再度
                                                      // startListening
                    // 音を出す
                    sensorcatch.start();
                    sensorcatch.setOnCompletionListener( new OnCompletionListener() {

                        public void onCompletion( MediaPlayer mp ) {
                            // TODO Auto-generated method stub
                            rec.startListening( RecognizerIntent.getVoiceDetailsIntent( getApplicationContext() ) );
                        }
                    } );
                    break;

                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: // かなり長い間放置すると発生する、対処は不要
                    break;

                case SpeechRecognizer.ERROR_NETWORK: // インターネットにつながっていない場合なので、Toastでメッセージを出し、アプリを終了
                    Toast.makeText( UntouchableTimerActivity.this, getResources().getText( R.string.message_error_network ), Toast.LENGTH_SHORT ).show();
                    finish();
                    break;

                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: // このエラーが、どういうケースで発生するか、未確認
                    break;

                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: // このエラーが、どういうケースで発生するか、未確認
                    break;

                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: // このエラーが、どういうケースで発生するか、未確認
                    break;

                case SpeechRecognizer.ERROR_CLIENT: // なんであれエラーが出た場合には、このエラーも発生する、対処は不要
                    break;

                case SpeechRecognizer.ERROR_SERVER: // このエラーが、どういうケースで発生するか、未確認、ときどき発生する、対処は不要
                    break;

                default: // これは起こらないはず
            }

        }

        @Override
        public void onReadyForSpeech( Bundle params ) {
            // 画面メッセージ
            tv_sensor_message.setText( R.string.message_talk_to_phone );
        }

        @Override
        public void onBufferReceived( byte[] arg0 ) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onEvent( int arg0, Bundle arg1 ) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPartialResults( Bundle arg0 ) { // 認識途中の部分的な結果が必要な場合に使うが、基本的に実装不要
            // TODO Auto-generated method stub

        }

        @Override
        public void onRmsChanged( float arg0 ) {
            // TODO Auto-generated method stub

        }

    }
}
