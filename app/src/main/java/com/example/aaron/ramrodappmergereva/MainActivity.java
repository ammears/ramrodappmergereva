/*
* RAMROD
* Senior Design ELEG498/499
* 2017-18
* 2-8-18
* ramrodappmergereva
* Changes from last revision:  Merged code with A. Mani.
*                              Changed font colors to be readable with black background.
*                              Added image for camera reset button.
*                              Changed button sizes and position to provide more
*                              open screen area for video use.
*                               - A. Mears
*/

package com.example.aaron.ramrodappmergereva;


import android.media.Image;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.MediaController;
import android.net.Uri;
import android.widget.Toast;
import android.widget.VideoView;
import static android.R.attr.button;
import com.example.aaron.ramrodappmergereva.R;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    //Code below added from A.J. Mani
    EditText addrField;
    Button btnConnect;
    VideoView streamView;
    MediaController mediaController;
    //End Mani code

    Button btnReset;
    EditText txtAddress;
    Socket myAppSocket = null;
    public static String wifiModuleIp = "";
    public static int wifiModulePort = 0;
    public static String CMD = "0";
    double UpDown = 1.5;
    double LR = 1.5;
    int Run = 0;
    int heartbeat = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Mani code
        //addrField = (EditText)findViewById(R.id.addr);
        //btnConnect = (Button)findViewById(R.id.connect);
        //streamView = (VideoView)findViewById(R.id.streamview);

        //btnConnect.setOnClickListener(new View.OnClickListener(){

            //@Override
            //public void onClick(View v) {
                //String s = addrField.getEditableText().toString();
               // playStream(s);
           // }});
        //End Mani Code

        SensorManager sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        //variable declaration for sensor data
        final float[] mValuesMagnet = new float[3];
        final float[] mValuesAccel = new float[3];
        final float[] mValuesOrientation = new float[3];
        final float[] mRotationMatrix = new float[9];

        final TextView txt1 = (TextView) findViewById(R.id.textView1);
        final TextView txt2 = (TextView) findViewById(R.id.textView);
        final SensorEventListener mEventListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent event) {
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        System.arraycopy(event.values, 0, mValuesAccel, 0, 3);
                        break;

                    case Sensor.TYPE_MAGNETIC_FIELD:
                        System.arraycopy(event.values, 0, mValuesMagnet, 0, 3);
                        break;
                }

                SensorManager.getRotationMatrix(mRotationMatrix, null, mValuesAccel, mValuesMagnet);
                SensorManager.getOrientation(mRotationMatrix, mValuesOrientation);

                //Change from radians to degrees, inverting throttle
                double steering = mValuesOrientation[1] * (180 / Math.PI);
                double throttle = mValuesOrientation[2] * (-180 / Math.PI);

                //Formatting to two decimal places
                DecimalFormat precision = new DecimalFormat("0.00");

                final CharSequence steerpos;
                final CharSequence speeddemand;
                double speedpercent = 0;
                double steerrange = 0;

                //Specify throttle response
                if (throttle <= 60 && throttle >= 40)//forward scale
                {
                    speedpercent = (throttle - 60) * -5;
                } else if (throttle >= 85 && throttle <= 105)//reverse scale
                {
                    speedpercent = -((throttle - 85) * 2.5);
                } else if (throttle < 40 && throttle >= 30)//100% throttle window
                {
                    speedpercent = 100;
                } else if (throttle > 105 && throttle <= 140)//reverse max speed window
                {
                    speedpercent = -50;
                } else //0% throttle for everything else
                {
                    speedpercent = 0;
                }

                //Set steering range
                if (steering > 30) {
                    steerrange = 30;
                } else if (steering < -30) {
                    steerrange = -30;
                } else {
                    steerrange = steering;
                }

                //Send to text boxes
                steerpos = "Steering Angle(degrees): " + precision.format(steerrange);
                txt1.setText(steerpos);
                speeddemand = "Throttle %: " + precision.format(speedpercent);
                txt2.setText(speeddemand);

                //Convert to string to send to pi program
                String steerpi = String.valueOf(precision.format(steerrange));
                String speedpi = String.valueOf(precision.format(speedpercent));
                String UpDownLevel = String.valueOf(precision.format(UpDown));
                String LeftRightLevel = String.valueOf(precision.format(LR));
                String StartStop = String.valueOf(Run);
                String heart = String.valueOf(heartbeat);

                //Concatenate the strings and send to Pi to be separated
                String cardata = steerpi + "||" + speedpi + "||" + UpDownLevel + "||"
                        + LeftRightLevel + "||" + StartStop + "||" + heart;

                getIPandPort();
                CMD = cardata;
                Socket_AsyncTask cmd_increase_servo = new Socket_AsyncTask();
                cmd_increase_servo.execute();

                heartbeat = heartbeat + 1;
            }

            ;
        };

        setListners(sensorManager, mEventListener);

        txtAddress = (EditText) findViewById(R.id.ipAddress);

        //Start of Button Code
        //Up/Down Buttons
        ImageButton btnUp = (ImageButton) findViewById(R.id.btnUp);
        //Up Button
        btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UpDown < 2) {
                    UpDown = UpDown + 0.1; //Raise pulse width
                }
            }
        });

        ImageButton btnDown = (ImageButton) findViewById(R.id.btnDown);
        //Down Button
        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UpDown > 1) {
                    UpDown = UpDown - 0.1; //Lower pulse width
                }
            }
        });
        //End Up/Down Button code
        //Left/Right Button code
        ImageButton btnLeft = (ImageButton) findViewById(R.id.btnLeft);
        //Left Button
        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LR > 1) {
                    LR = LR - 0.1; //Lower pulse width
                }
            }
        });
        ImageButton btnRight = (ImageButton) findViewById(R.id.btnRight);
        //Right Button
        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LR < 2) {
                    LR = LR + 0.1; //Raise pulse width
                }
            }
        });
        //End Left/Right Button code
        //Camera Reset - Returns camera servos to center position regardless of current position
        ImageButton btnReset = (ImageButton) findViewById(R.id.btnReset);
        //Reset Button
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Changes pulse widths of L/R and U/D buttons back to center
                LR = 1.5;
                UpDown = 1.5;
            }
        });
        //Start/Stop of Car Motion
        ImageButton imageButton4 = (ImageButton) findViewById(R.id.imageButton4);
        //Reset Button

        imageButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (Run == 0)
                    {
                        Run = 1;
                    }
                else
                    {
                        Run = 0;
                    }

                ImageButton button = (ImageButton) v;
                int icon;
                if (paused)
                {
                    paused = false;
                    icon = R.drawable.redstop3;
                }
                else
                {
                    paused = true;
                    icon = R.drawable.greenstart3;
                }

                button.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), icon));

            }
        });
        //End of Button code
    }

    //Mani code
    //private void playStream(String src){
       // Uri UriSrc = Uri.parse(src);
       //if(UriSrc == null){
          //  Toast.makeText(MainActivity.this,
          //          "UriSrc == null", Toast.LENGTH_LONG).show();
       // }else{
          //  streamView.setVideoURI(UriSrc);
           // mediaController = new MediaController(this);
           // streamView.setMediaController(mediaController);
           // streamView.start();

           // Toast.makeText(MainActivity.this,
           //         "Connect: " + src,
               //     Toast.LENGTH_LONG).show();
       // }
    //}

    //@Override
    //protected void onDestroy() {
      //  super.onDestroy();
      //  streamView.stopPlayback();
    //}

    private boolean paused = true;
    public void buttonPressed(View v)
    {

        ImageButton button = (ImageButton) v;
        int icon;
        if (paused)
        {
            paused = false;
            icon = R.drawable.greenstart3;
        }
        else
        {
            paused = true;
            icon = R.drawable.redstop3;
        }

        button.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), icon));




    }
    //End Mani code

    //Sensor and socket calls
    public void setListners(SensorManager sensorManager, SensorEventListener mEventListener) {
        sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                125000);
        sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                125000);
        //Instead of using SensorManager.SENSOR_DELAY_NORMAL above, using specific sampling periods to optimize speed
        // SensorManager.SENSOR_DELAY_FASTEST (0uS) is too fast, causes hangups in data transfer
        //SensorManager.SENSOR_DELAY_NORMAL (200,000uS) is slow, causing choppiness
    }

    public void getIPandPort() {
        String iPandPort = txtAddress.getText().toString();
        Log.d("MYTEST", "IP String: " + iPandPort);
        String temp[] = iPandPort.split(":");
        wifiModuleIp = temp[0];
        wifiModulePort = Integer.valueOf(temp[1]);
        Log.d("MY TEST", "IP:" + wifiModuleIp);
        Log.d("MY TEST", "PORT:" + wifiModulePort);
    }

    public class Socket_AsyncTask extends AsyncTask<Void, Void, Void> {
        Socket socket;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                InetAddress inetAddress = InetAddress.getByName(MainActivity.wifiModuleIp);
                socket = new java.net.Socket(inetAddress, MainActivity.wifiModulePort);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeBytes(CMD);
                dataOutputStream.close();
                socket.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        }
    }
