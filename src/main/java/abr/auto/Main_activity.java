package abr.auto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.lang.String;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

import static java.lang.Thread.sleep;

public class Main_activity extends Activity implements IOIOLooperProvider, SensorEventListener        // implements IOIOLooperProvider: from IOIOActivity
{
	private final IOIOAndroidApplicationHelper helper_ = new IOIOAndroidApplicationHelper(this, this);			// from IOIOActivity
	private TextView irLeftText;
	private TextView irCenterText;
	private TextView irRightText;
	private ToggleButton btnStartStop;

	//variables for compass
	private SensorManager mSensorManager;
	private Sensor mCompass, mAccelerometer;
	float[] mAcc;
	//variables for logging
	private Sensor mGyroscope;
	private Sensor mGravityS;
	float[] mGravity;
	float[] mGyro;
	float[] mGeomagnetic;
	float[] data = new float[1000];
	private float[] mR = new float[9];
	private float[] mOrientation = new float[3];
	private float mCurrentDegree = 0f;

	int counter = 0;

	float speedleft_ = .30f;
	float speedright_ = .30f;
	float leftspeed = speedleft_;
	float rightspeed = speedright_;
	boolean forward = true;
	int timer = 0;


	IOIO_thread_rover_tank m_ioio_thread;

	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		irLeftText = (TextView) findViewById(R.id.irLeft);
		irCenterText = (TextView) findViewById(R.id.irCenter);
		irRightText = (TextView) findViewById(R.id.irRight);
		btnStartStop = (ToggleButton) findViewById(R.id.buttonStartStop);

		//set up compass
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mCompass= mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mAccelerometer= mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mGravityS = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

		GraphView graph = (GraphView) findViewById(R.id.graph);
		LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
				new DataPoint(0, 1),
				new DataPoint(1, 5),
				new DataPoint(2, 3),
				new DataPoint(3, 2),
				new DataPoint(4, 6)
		});
		graph.addSeries(series);


		helper_.create();		// from IOIOActivity

		// enableUi(false);

	}

	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	public void counter(){
		counter = 0;
		for (int i = 1; i<data.length; i++){
			if (data[i] != 0.0){
				counter++;
			}
		}
	}


	public void updater(float x, boolean reverse){
		for (int i = 1;i < data.length; i++){
			if (data[i] == 0.0){
				data[i] = x;
				if (reverse == true) {
					float temp_x = x + 180;
					if (temp_x > 360) {
						temp_x = temp_x -360;
					}
					data[i] = temp_x;
				}

				Log.d("V",String.valueOf(x));
				break;


			}
		}
	}
	// Called whenever the value of a sensor changes
	// Should be called whenever a sensor changes.
	// Good time to get IR values and send move commands to the robot
	@Override
	public final void onSensorChanged(SensorEvent event) {
		if(m_ioio_thread != null){

			// Movement


			// ----------------
			setText(String.format("%.3f", m_ioio_thread.getIrLeftReading()), irLeftText);
			setText(String.format("%.3d", counter), irCenterText);
			//setText(String.format("%.3f", m_ioio_thread.getIrRightReading()), irRightText);
			if (btnStartStop.isChecked()) {
				//m_ioio_thread.move(0.5f,0.5f,true,true);
				Log.d("V","Button Pressed");
				counter();

				updater(mCurrentDegree, false);

				rightspeed = speedright_;
				leftspeed = speedleft_;

				float leftsense = m_ioio_thread.getIrLeftReading();
				float rightsense = m_ioio_thread.getIrRightReading();
				float centersense = m_ioio_thread.getIrCenterReading();


				if (leftsense > rightsense && leftsense >= 0.7){
					//rightspeed = (float)speedright_ - (float) leftsense/2f;
					rightspeed = (float) speedright_ - leftsense/2f;
				}
				if (centersense >= 1.2) {
					forward = false;
					updater(mCurrentDegree, true);
				}
				if (centersense < 1.2) {
					forward = true;
				}
				if (rightsense > leftsense && rightsense >= 0.7) {
					//leftspeed = (float)speedleft_ - (float) rightsense/2f;
					leftspeed = (float) speedleft_ - rightsense/2f;
				}

				m_ioio_thread.move(leftspeed, rightspeed, forward, forward);

			}
			else {
				m_ioio_thread.move(0.0f,0.0f,false,false);
			}
		}

		// sensors unused for the moment.  may want to implement later
		if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
			mGravity = event.values;
			// Log.d("robo", "got gravity");
		}
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)

			mGyro = event.values;
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mAcc = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			//Log.d("V","sensor change");
			mGeomagnetic = event.values;
			//String testing = String.format("%.3f", mGeomagnetic[0]);
			//Log.d("V",testing);
			//int leng = mGeomagnetic.length;
			//Log.d("V",String.valueOf(leng));

		if (mAcc != null && mGeomagnetic != null) {
			SensorManager.getRotationMatrix(mR, null, mAcc, mGeomagnetic);
			SensorManager.getOrientation(mR, mOrientation);
			float azimuthInRadians = mOrientation[0];
			float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
			mCurrentDegree = azimuthInDegress;
			setText(String.format("%.3f", mCurrentDegree), irRightText);
			Log.d("V",String.valueOf(mCurrentDegree));
		}




	}

	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				btnStartStop.setEnabled(enable);
			}
		});
	}

	//set the text of any text view in this application
	public void setText(final String str, final TextView tv)
	{
		// Log.d("robo", "setText");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tv.setText(str);
			}
		});
	}

	/****************************************************** functions from IOIOActivity *********************************************************************************/

	/**
	 * Create the  {@link IOIO_thread_pwm}. Called by the {@link IOIOAndroidApplicationHelper}. <br>
	 * Function copied from original IOIOActivity.
	 * */
	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) 
	{
		if(m_ioio_thread == null && connectionType.matches("ioio.lib.android.bluetooth.BluetoothIOIOConnection"))
		{
			// enableUi(true);
			m_ioio_thread = new IOIO_thread_rover_tank();



			return m_ioio_thread;
		}
		else
		{
			return null;
		}



	}

	//Called whenever activity resumes from pause
	@Override
	public void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mGravityS, SensorManager.SENSOR_DELAY_NORMAL);
		helper_.start();		// from IOIOActivity
		// Log.d("robo", "onResume");
	}

	//Called when activity pauses
	@Override
	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		// Log.d("robo", "onPause");
	}

	//Called when activity restarts. onCreate() will then be called
	@Override
	public void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onDestroy() 
	{
		helper_.destroy();
		super.onDestroy();
	}

	@Override
	protected void onStart() 
	{
		super.onStart();
		helper_.start();
	}

	@Override
	protected void onStop() 
	{
		helper_.stop();
		super.onStop();
	}

	@Override
	protected void onNewIntent(Intent intent) 
	{
		super.onNewIntent(intent);
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) 
		{
			helper_.restart();
		}
	}
}
