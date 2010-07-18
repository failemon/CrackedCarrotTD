package com.crackedcarrot.multiplayer;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.crackedcarrot.GameInit;
import com.crackedcarrot.menu.R;

/**
 * This Activity lists available and paired devices. When the user
 * chooses a device in the list, the MAC address of the device is sent back to the Client
 * in the result Intent.
 */
public class ScanDevices extends Activity {
   
    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    
    // The Universally Unique Identifier (UUID) for this application
    private static final UUID MY_UUID = UUID.fromString("9a8aa173-eaf0-4370-80e1-3a13ed5efae9");
    // The request codes for startActivity and onActivityResult
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_DISCOVERABLE = 3;
    
    // The client thread
    private ConnectThread mConnectThread;
    
    public BluetoothSocket mmClientSocket = null;

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    
    private final int DIFFICULTY = 1; //Default diff. for multiplayer is normal
    private final int MAP = 4; // Default map for multiplayer
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
        
    	// Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available on this device", 
            		Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.d("SCANDEVICES", "Add these from start: " + device.getName() + " " + device.getAddress());
            }
        } else {
            String noDevices = "No devices have been paired";
            mPairedDevicesArrayAdapter.add(noDevices);
            Log.d("SCANDEVICES", noDevices);
        }
    }
    
    /** When the activity first starts, do following */
    @Override
    public void onStart() {
        super.onStart();
        
        /** Request that the device will be discoverable for 300 seconds 
         *  Only need to do this for the server side of the connection */ /*
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE); */
        
        /** Request that Bluetooth will be activated if not on.
         *  setupClient() will then be called during onActivityResult */
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
            Log.d("CLIENT", "Request enable Bluetooth");
        } 
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
        Log.d("SCANDEVICES", "Unregister Receiver!!!");
    }
    
    
    /** This method is called after the startActivityForResult() is called with
    parameters containing activity id and user choice */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    switch (requestCode) {
	    case REQUEST_CONNECT_DEVICE:

	    		// CODE MOVED BELOW TO OnItemClickListener!
	    	
	        break;
	    case REQUEST_DISCOVERABLE:
	    	if (resultCode == 300) {
	            // The device is made discoverable and bluetooth is activated
	        } else {
	            // User did not accept the request or an error occured
	            Toast.makeText(this, "The device was not made discoverable. Leaving multiplayer"
	            		, Toast.LENGTH_SHORT).show();
	            finish();
	        }
	    	break;
	    case REQUEST_ENABLE_BLUETOOTH:
	        // When the request to enable Bluetooth returns
	        if (resultCode == Activity.RESULT_OK) {
	            // Bluetooth is now enabled, so do nothing
	        } else {
	            // User did not enable Bluetooth or an error occured
	            Toast.makeText(this, "Bluetooth was not enabled. Leaving multiplayer."
	            		, Toast.LENGTH_SHORT).show();
	            finish();
	        }
	    }
	}
    

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle("scanning for devices...");

        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
        	// Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            	/*
            	 * OLD CODE! This used to return with intents to Client.class,
            	 * now we handle the connection straight in here instead.
            	 *
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            Log.d("SCANDEVICES", info);
            Log.d("SCANDEVICES", address);
            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
            
            	*/
            
            String info = ((TextView) v).getText().toString();
            // Get the MAC address of the device
            String address = info.substring(info.length() - 17);
            // Get the BLuetoothDevice object
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            
            	// Show connecting-progress-dialog.
            showDialog(1);
            
            // Try to connect to the device
            connect(device);
            
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    Log.d("SCANDEVICES", "Add to NewDevicesArrayAdapter");
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("select a device to connect");
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "No devices found";
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
    
    
    /**
     * This synchronized method starts the thread that
     * does the actual connection with the available server
     * @param device
     */
    private synchronized void connect(BluetoothDevice device) {
    	if (mConnectThread == null) {
    		mConnectThread = new ConnectThread(device);
    		mConnectThread.start();
    		
    		toastFailed();
    		
    		Log.d("CLIENT", "Start connect thread");
    	}
    }
    
    
    private void startGame(){
    	Log.d("CLIENT", "Start game");
    	GameInit.setMultiplayer(mmClientSocket);
    	Intent StartGame = new Intent(this ,GameInit.class);
		StartGame.putExtra("com.crackedcarrot.menu.map", MAP);
		StartGame.putExtra("com.crackedcarrot.menu.difficulty", DIFFICULTY);
		startActivity(StartGame);
		// Cancel the thread that completed the connection
        mConnectThread = null;
		finish();
    }
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 1: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage("Connecting to server...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(true);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
                	
                	public void onCancel(DialogInterface dialog){
                		//nothing, activity will default to finish on connection error.
                	}
                });
                return dialog;
            }
        }
        return null;
    }
    
    
    private class ConnectThread extends Thread {

        public ConnectThread(BluetoothDevice device) {
        	// mmClientSocket is final so use a temporary object first
            BluetoothSocket tmp = null;
            Log.d("CLIENT", "Connect thread constructor");
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmClientSocket = tmp;
        }

        public void run() {
        	
        	Looper.prepare();
        	
        	Toast.makeText(getBaseContext(), "Connection to server failed...leaving"
            		, Toast.LENGTH_LONG).show();
        	
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();
            Log.d("CLIENT", "Connectthread runs");
            try {
                // Connect through the socket. This will block until it succeeds or throws an exception
            	Log.d("CLIENT", "Connectthread call connect");
                mmClientSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
            	
            	finish();
            	
                try {
                    mmClientSocket.close();
                } catch (IOException closeException) {
                	Log.e("CLIENT", "Can't close socket", closeException);
                }
                
            	return;
            }
            Log.d("CLIENT", "Ansluten!!!");
            startGame();
        }
    }
    
    void toastFailed() {
    	// Send a message that connection failed
    	Toast.makeText(this, "Connection to server failed...leaving"
        		, Toast.LENGTH_LONG).show();
    }

}
