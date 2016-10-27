/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

//import android.R;


//import com.example.R;

//import com.example.R;

//import com.example.R;

import java.util.Timer;
import java.util.TimerTask;

//import org.kreed.vanilla.R;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * This is the main Activity that displays the current chat session.
 */
@SuppressWarnings("unused")
@SuppressLint("HandlerLeak") 
public class BluetoothChat extends Activity implements SensorEventListener {
	  private SensorManager mSensorManager;
	  private Sensor mAccelerometer;

	
	  TextView title,tv,tv1,tv2;
	  private Sensor acc;
	    private SensorManager sm;
	    private TextView t1;
	    private double value;
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static int speed=0;
	public static int ready=0;
	public static int direction=0; //0-forward , 1 reverse
	public static float x1=0,y1=0,z1=0;
	

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	String message;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	Compass myCompass;
	// Layout Views
	private ListView mConversationView;
	private EditText mOutEditText;
	private Button mSendButton;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		
			    super.onCreate(savedInstanceState);
			    setContentView(R.layout.main);
			    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			 mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			
			    
			    //get textviews
			 
			    title=(TextView)findViewById(R.id.name);   
			    title.setText("hi");
			    tv=(TextView)findViewById(R.id.xval);
			    tv1=(TextView)findViewById(R.id.yval);
			    tv2=(TextView)findViewById(R.id.zval);
			    
			   	    
				
			

		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		setContentView(R.layout.main);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
	}
	  
	

	@Override
	public void onStart() {
		super.onStart();
		
		myCompass = (Compass)findViewById(R.id.mycompass1);
		
		
		 tv=(TextView)findViewById(R.id.xval); 
		 tv1=(TextView)findViewById(R.id.yval);
		 tv2=(TextView)findViewById(R.id.zval); 
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}


	public synchronized void onResume() {
		super.onResume();
		
		 mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME & SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM );
		
		 
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");

		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);

		// Initialize the compose field with a listener for the return key
		mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		mOutEditText.setOnEditorActionListener(mWriteListener);

		CompoundButton mToggleButton1 = (CompoundButton) findViewById(R.id.toggleButton1);
		mToggleButton1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        // do something, the isChecked will be
		        // true if the switch is in the On position
		    	final CompoundButton directionButton = (CompoundButton) findViewById(R.id.switch2);
				final CompoundButton directionButton1 = (CompoundButton) findViewById(R.id.switch1);
				if(isChecked)
		    	{
		    		//
		    		message="7\n";
					sendMessage(message);
					message="7\n";
					sendMessage(message);
				
		    	directionButton1.setChecked(false);
		    	directionButton.setChecked(false);
		    	}
		    	else
		    	{
		    		//
		    		message="7\n";
					sendMessage(message);
		    		 message="7\n";
					sendMessage(message);
					directionButton1.setChecked(false);
			    	directionButton.setChecked(false);
		    		
		    	}
		    }
		});
	/*	CompoundButton mToggleButton = (CompoundButton) findViewById(R.id.switch1);
		mToggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        // do something, the isChecked will be
		        // true if the switch is in the On position
		    	if(isChecked)
		    	{
		    		//LED on
		    		 message="30\n";
					sendMessage(message);
		    	}
		    	else
		    	{
		    		//LED off
		    		 message="31\n";
					sendMessage(message);
		    		
		    	}
		    }
		});
		*/
		final CompoundButton directionButton = (CompoundButton) findViewById(R.id.switch2);
		final CompoundButton directionButton1 = (CompoundButton) findViewById(R.id.switch1);
		directionButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        // do something, the isChecked will be
		        // true if the switch is in the On position
		    	if(isChecked)
		    	{
		    		//Forward
		    		message="5\n";
		    		sendMessage(message);
		    		message="5\n";
		    		sendMessage(message);
		    
		    		//direction=0;
		    		directionButton1.setChecked(false);
		    		
		    		
		    		
		    	}
		    	else
		    	{
		    		if(!directionButton1.isChecked())
		    		{
		    			message="7\n";
			    		sendMessage(message);
		    			
		    		}
		    		//Reverse
		    		
						//directionButton1.setChecked(true);
		    	//	direction=1;
		    	}
		    }
		});
		
		directionButton1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        // do something, the isChecked will be
		        // true if the switch is in the On position
		    	if(isChecked)
		    	{
		    		//rev
		    		 message="6\n";
						sendMessage(message);
						 message="6\n";
							sendMessage(message);
		    		directionButton.setChecked(false);
		    
		    		//direction=0;
		    		
		    		
		    		
		    	}
		    	else
		    	{

		    		if(!directionButton.isChecked())
		    		{
		    			message="7\n";
			    		sendMessage(message);
		    			
		    		}
		    	}
		    }
		});
		// Initialize the send button with a listener that for click events
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
			TextView view = (TextView) findViewById(R.id.edit_text_out);
				// String message = view.getText().toString();
				 message="5\n";
				sendMessage(message);
				 message="5\n";
				sendMessage(message);
			}
		}); 
		SeekBar mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
		mSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       

		    @Override       
		    public void onStopTrackingTouch(SeekBar seekBar) {      
		    	
		        // TODO Auto-generated method stub      
		    }       

		    @Override       
		    public void onStartTrackingTouch(SeekBar seekBar) {     
		        // TODO Auto-generated method stub      
		    }       

		    @Override       
		    public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {     
		        // TODO Auto-generated method stub      

		      //  t1.setTextSize(progress);
		     //   Toast.makeText(getApplicationContext(), String.valueOf(progress),Toast.LENGTH_LONG).show();
		    	if(progress==0)
		    	{
		    		//LOW
		    		speed=0;
		    	}
		    	else if(progress==1)
		    	{
		    		//MED
		    		speed=1;
		    	}
		    	else
		    	{
		    		//HIGH
		    		speed=2;
		    	}
		    }       
		});             
		mForwardButton = (Button) findViewById(R.id.button1);
		mForwardButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				// String message = view.getText().toString();
				//  tv.setText("X axis" +"\t\t");
				 tv=(TextView)findViewById(R.id.xval); 
				 tv1=(TextView)findViewById(R.id.yval);
				 tv2=(TextView)findViewById(R.id.zval); 
				 
			//	tv.setText("none");
				 message="21\n";
				sendMessage(message);
				 message="21\n";
					sendMessage(message);
				
			}
		}); 
		
		mReverseButton = (Button) findViewById(R.id.button4);
		mReverseButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				// String message = view.getText().toString();
				 message="22\n";
				sendMessage(message);
				 message="22\n";
					sendMessage(message);
			}
		}); 
		
		mRightButton = (Button) findViewById(R.id.button3);
		mRightButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				// String message = view.getText().toString();
				 message="24\n";
				sendMessage(message);
				 message="24\n";
					sendMessage(message);
			}
		}); 
		
		mLeftButton = (Button) findViewById(R.id.button2);
		mLeftButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				// String message = view.getText().toString();
				 message="23\n";
				sendMessage(message);
				 message="23\n";
				sendMessage(message);
			}
		}); 
		
		
			View.OnTouchListener btnTouch = new View.OnTouchListener() {
			     @Override
			    public boolean onTouch(View v, MotionEvent event) {
			        int action = event.getAction();
			        if (action == MotionEvent.ACTION_DOWN)
			        { 	
			        	message="8\n";
						sendMessage(message);
						
			        }
			        else if (action == MotionEvent.ACTION_UP)
			        {	
			        	message="9\n";
						sendMessage(message);
						message="9\n";
						sendMessage(message);
					
						
					
			        }
			        return false;   //  the listener has NOT consumed the event, pass it on
			    }
			};
		//	mStartButton = (Button) findViewById(R.id.button5);
			ImageButton mPlayPauseButton = (ImageButton)findViewById(R.id.play_pause);
			mPlayPauseButton.setOnTouchListener(btnTouch);
			//OnTouchListener btnTouch = null;
		//	mStartButton.setOnTouchListener(btnTouch);



		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	public void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			
			return;
		}
		

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			mOutEditText.setText(mOutStringBuffer);
		}
	}

	// The action listener for the EditText widget, to listen for the return key
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				sendMessage(message);
			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					setStatus(getString(R.string.title_connected_to,
							mConnectedDeviceName));
					mConversationArrayAdapter.clear();
					break;
				case BluetoothChatService.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					setStatus(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
						+ readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};
	private Button mForwardButton;
	private Button mReverseButton;
	private Button mRightButton;
	private Button mLeftButton;
	private Button mStartButton;
	private Button mStopButton;
	private SensorManager senSensorManager;
	private Sensor senAccelerometer;

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void connectDevice(Intent data) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}
	
	public void onSensorChanged(SensorEvent event) {
		// Many sensors return 3 values, one for each axis.
	    float x =  event.values[0];
	    float y =  event.values[1];
	    float z =  event.values[2];
	  
	    
	    //display values using TextView
	    title.setText(R.string.app_name);
	   tv.setText("X axis" +"\t\t"+x);
	    tv1.setText("Y axis" + "\t\t" +y);
	    
	    float[] matrixValues = new float[3];
	    if(x==0)
	    {
	    	//TODO code
	    	 matrixValues[0]=(float) 0.0;
	    	 myCompass.update(matrixValues[0]);
	    	
	    }
	    else if(x<0.19 & x>-0.19)
	    {
	    	 matrixValues[0]=(float) 0.0;
	    	 myCompass.update(matrixValues[0]);
	    }
	    
	    else if(x>6)
	    {
	    	x=(float) 6.0;
	    	 matrixValues[0]=(float) ((float) 0.175*(float)x+(float)6.3);
	    	 myCompass.update(matrixValues[0]);
	    }
	    else if(x<-6)
	    {
	    	x=(float) -6.0;
	    	 matrixValues[0]=(float) ((float) 0.175*(float)x+(float)6.3);
	    	 myCompass.update(matrixValues[0]);
	    }
	    else
	    {
	    	if(mod(x, x1)>0.2)
	    	{
	    		x1=x;
	    	 matrixValues[0]=(float) ((float) 0.175*(float)x+(float)6.3);
	    	 myCompass.update(matrixValues[0]);
	    }
	    }
	    
	  //  tv2.setText("Z axis" +"\t\t" +z);
	//    if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED)
	    if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
	    {

	    if(x>4)
	    {
	    	if(speed==0)
	    	{
		    		 message="24\n";
					sendMessage(message);
				
					message="30\n";
					sendMessage(message);
					
				
	    	}
	    	else if(speed==1)
	    	{
	    		 	message="27\n";
					sendMessage(message);
			
					message="31\n";
					sendMessage(message);
	    		
	    	}
	    	else
	    	{
	    		 	message="29\n";
					sendMessage(message);
				
					message="31\n";
					sendMessage(message);

	    	}
	    	
		//	 matrixValues[0]=(float) 1.575;
		//	 myCompass.update(matrixValues[0]);
	    	tv2.setText("left");
	    }
	    else if (x<-3)
	    {
	    	if(speed==0)
	    	{
		    		 message="34\n";
					sendMessage(message);
					
					message="20\n";
					sendMessage(message);
				
				
	    	}
	    	else if(speed==1)
	    	{
	    		 	message="37\n";
					sendMessage(message);
					
				
					message="21\n";
					sendMessage(message);

					
	    	}
	    	else
	    	{
	    		 	message="39\n";
					sendMessage(message);
				
					message="21\n";
					sendMessage(message);
				
	    		
	    	}
	    	
	    	tv2.setText("right");
	    }
	    else
	    {
	    	if(speed==0)
	    	{
		    		 message="32\n";
					sendMessage(message);
		    		
					message="22\n";
					sendMessage(message);
					
	    	}
	    	else if(speed==1)
	    	{
	    		 	message="35\n";
					sendMessage(message);
	    		 
					message="25\n";
					sendMessage(message);
	    		
	    	}
	    	else
	    	{
	    		 	message="37\n";
					sendMessage(message);
	    		
					
					message="27\n";
					sendMessage(message);
	    		
	    	}
	    	
	    }
	    }

	}
	 
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	 
	}
	@Override
	public void onBackPressed()
	{
		message="7\n";
		sendMessage(message);
	
	      moveTaskToBack(true);
	}
	private float mod(float x, float y)
	{
	    float result = x - y;
	    if (result < 0)
	        result = -1*result;
	    return result;
	}


}
