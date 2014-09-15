package com.uarmy.art.bt_sync;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class BtSync extends ActionBarActivity {

    public static final String SERVICE_NAME = "BtSync";
    public static final UUID SERVICE_UUID = UUID.fromString("acfd3330-2af0-11e4-8c21-0800200c9a66");
    public static final String EXTRA_SYNC_DATA = "sync_data";

    public static final int ACTIVITY_RESULT_OK = 0;
    public static final int ACTIVITY_RESULT_FAILED = 1;
    public static final int ACTIVITY_RESULT_CANCEL = 2; // no sync was done
    public static final String RECV_DATA = "recv_data";

    private static final String TAG = "BtSync";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_STATE_BT = 2;

    public static final int MESSAGE_STATE_CHANGE = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;
    public static final int MESSAGE_DATA_READ = 5;
    public static final int MESSAGE_DATA_WRITTEN = 6;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";


    public static final int CONN_STATE_CONNECTED = 0;
    public static final int CONN_STATE_CONNECTING = 1;
    public static final int CONN_STATE_NONE = 2;
    public static final int CONN_STATE_LISTEN = 3;

    private static final int DATA_READER = 0;
    private static final int DATA_WRITER = 1;


    private int myState = CONN_STATE_NONE;

    private void enableTransferUI() {
        findViewById(R.id.sync_progress).setVisibility(View.VISIBLE);
    }
    private void disableTransferUI() {
        findViewById(R.id.sync_progress).setVisibility(View.INVISIBLE);
    }

    private TextView myTitle;
    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MESSAGE_STATE_CHANGE:
                    //if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case CONN_STATE_CONNECTED: {
                            //setupMessagingUI();
                            enableTransferUI();
                            Resources res = getResources();
                            //myTitle.setText(R.string.title_connected_to);
                            //myTitle.append(myConnectedDeviceName);
                            setTitle(res.getString(R.string.title_connected_to) + myConnectedDeviceName);
                            TextView status_view = (TextView) findViewById(R.id.status);
                            status_view.setText(res.getString(R.string.title_connected_to) + myConnectedDeviceName);

                        }   Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                            break;
                        case CONN_STATE_CONNECTING: {
                            setTitle(R.string.title_connecting);
                            TextView status_view = (TextView) findViewById(R.id.status);
                            status_view.setText(R.string.title_connecting);
                            //myTitle.setText(R.string.title_connecting);
                            Toast.makeText(getApplicationContext(), "Connecting", Toast.LENGTH_SHORT).show();
                        }
                            break;
                        case CONN_STATE_LISTEN:
                            //myTitle.setText(R.string.title_listen);
                            Toast.makeText(getApplicationContext(), "Listen", Toast.LENGTH_SHORT).show();
                        case CONN_STATE_NONE:
                            disableTransferUI();
                            setTitle(R.string.title_not_connected);
                            break;
                    }
                    break;

                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    myConversationArrayAdapter.add("Me: " + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    myConversationArrayAdapter.add(myConnectedDeviceName+": " + readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    myConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + myConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    String text = msg.getData().getString(TOAST);
                    Toast.makeText(getApplicationContext(), text,
                            Toast.LENGTH_SHORT).show();
                    TextView status_view = (TextView)findViewById(R.id.status);
                    status_view.setText(text);
                    disableTransferUI();
                    break;
                case MESSAGE_DATA_READ:
                    myDataRead = true;
                    myReceivedData = (String)msg.obj;
                    Log.d(TAG, "Data received, finishing activity with result - OK");
                    Intent intent = new Intent();
                    intent.putExtra(RECV_DATA, myReceivedData);
                    setResult(Activity.RESULT_OK, intent);
                    if(myDataWritten) {
                        exitApp();
                    }
                    break;
                case MESSAGE_DATA_WRITTEN:
                    myDataWritten = true;
                    if(myDataRead) {
                        exitApp();
                    }

            }
        }
    };

    private ArrayAdapter<BtDeviceDesc> myArrayAdapter;
    private ArrayAdapter<String> myConversationArrayAdapter;

    private BtBroadcastReceiver myReceiver;
    private BluetoothAdapter myBtAdapter;

    private String myConnectedDeviceName = null;

    private BtServer myServer = null;
    private BtClient myClient = null;
    //private BtConnected myConnectedThread = null;
    private String myReceivedData = null;
    private String myDataToSend = null;

    private BtConnected myConnectedReaderThread = null;
    private BtConnected myConnectedWriterThread = null;

    private boolean myDataWritten = false;
    private boolean myDataRead = false;


    private int getState() { return myState; }

    private OnItemClickListener myMessageClickedHandler = new OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            BtDeviceDesc desc = myArrayAdapter.getItem(position);
            createClient(desc.getDevice());
        }
    };

    private void createDeviceListUI() {
        myArrayAdapter = new ArrayAdapter<BtDeviceDesc>(this,
                android.R.layout.simple_list_item_1);
        ListView listView = (ListView) findViewById(R.id.device_listview);
        listView.setOnItemClickListener(myMessageClickedHandler);
        listView.setAdapter(myArrayAdapter);
    }

    private void setupMessagingUI() {

        Log.d(TAG, "setting up messaging GUI");

        setContentView(R.layout.messaging);

        // Initialize the array adapter for the conversation thread
        myConversationArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView convList = (ListView) findViewById(R.id.in);
        convList.setAdapter(myConversationArrayAdapter);
        myConversationArrayAdapter.clear();

        // Initialize the compose field with a listener for the return key
        EditText editText = (EditText) findViewById(R.id.edit_text_out);
        editText.setOnEditorActionListener(myWriteListener);

        // Initialize the send button with a listener that for click events
        Button sendButton = (Button) findViewById(R.id.button_send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                if(sendMessage(message)) {
                    view.setText("");
                }
            }
        });

        // Initialize the buffer for outgoing messages
        //mOutStringBuffer = new StringBuffer("");
    }

    private boolean sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (getState() != CONN_STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            write(send);
            return true;
        }

        return false;
    }


    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener myWriteListener =
        new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                // If the action is a key-up event on the return key, send the message
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                    String message = view.getText().toString();
                    if(sendMessage(message)) {
                        view.setText("");
                    }
                }
                Log.d(TAG, "END onEditorAction");
                return true;
            }
        };


    private void fillBtDevicesList(BluetoothAdapter bt_adapter) {
        // check for paired devices first
        Set<BluetoothDevice> pairedDevices = bt_adapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                myArrayAdapter.add( new BtDeviceDesc(device, true));
            }
        }
    }

    private void registerBtBroadcastReceiver() {
        myReceiver = new BtBroadcastReceiver();
        myReceiver.setAdapter(myArrayAdapter);
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(myReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_bt_sync);
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        setTitle(R.string.app_name);

        //myTitle = (TextView) findViewById(R.id.title_left_text);
        //myTitle.setText(R.string.app_name);
        //myTitle = (TextView) findViewById(R.id.title_right_text);

        createDeviceListUI();
        myBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (myBtAdapter == null) {
            // Device does not support Bluetooth
            Log.d(TAG, "Device does not support Bluetooth");
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            // FIXME: really just exitApp() ???
            exitApp();
            return;
        }

        myDataToSend = getIntent().getStringExtra(EXTRA_SYNC_DATA);
        if(myDataToSend==null)
            myDataToSend = "";

    }

    @Override
    public void onStart() {
        super.onStart();

        if (!myBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Log.d(TAG, "Starting Bluetooth");
            //Intent stateBtIntent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
            //startActivityForResult(stateBtIntent, REQUEST_STATE_BT);
        }
        else {
            Log.d(TAG, "Bluetooth is already started");
            whenBtDeviceReady(myBtAdapter);
        }

    }

    private void whenBtDeviceReady(BluetoothAdapter bt_adapter) {
        fillBtDevicesList(bt_adapter);
        registerBtBroadcastReceiver();
        bt_adapter.startDiscovery();
        createServer();
    }

    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                switch(resultCode) {
                    case RESULT_CANCELED:
                        Log.d(TAG, "RESULT_CANCELED");
                        break;
                    case RESULT_OK:
                        Log.d(TAG, "RESULT_OK");
                        whenBtDeviceReady(myBtAdapter);
                        break;
                    default:
                        Log.d(TAG, "bt enabling result is" + resultCode);
                }
                break;
            case REQUEST_STATE_BT:
                switch(resultCode) {
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth ON");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "Bluetooth OFF");
                        break;
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bt_sync, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void stopAll() {
        if(myClient!=null) {
            Log.d(TAG, "Stopping client");
            myClient.cancel();
            myClient = null;
            Log.d(TAG, "DONE");
        }
        if(myServer!=null) {
            Log.d(TAG, "Stopping server");
            myServer.cancel();
            myServer = null;
            Log.d(TAG, "DONE");
        }
    }
    private void stopReadWrite() {
        if(myConnectedReaderThread!=null) {
            Log.d(TAG, "Stopping reader");
            myConnectedReaderThread.cancel();
            myConnectedReaderThread = null;
            Log.d(TAG, "DONE");
        }
        if(myConnectedWriterThread!=null) {
            Log.d(TAG, "Stopping writer");
            myConnectedWriterThread.cancel();
            myConnectedWriterThread = null;
            Log.d(TAG, "DONE");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopAll();

        // Make sure we're not doing discovery anymore
        if (myBtAdapter != null) {
            myBtAdapter.cancelDiscovery();
        }
        // Unregister broadcast listeners
        this.unregisterReceiver(myReceiver);

        Log.d(TAG, "onDestroy");
    }

    private void enableDiscovery() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }
    /// only for server!
    public void onEnableDiscoverableBtn(View view) {
        enableDiscovery();
    }
    public void onDiscoverBtn(View view) {
        myArrayAdapter.clear();
        myBtAdapter.startDiscovery();
        Log.d(TAG, "discovering bt adapters...");
    }


    public void onReturnBtn(View  view) {
        Log.d(TAG, "onReturnBtn");
        Intent intent = new Intent();
        if(myReceivedData==null) {
            setResult(Activity.RESULT_CANCELED);
        } else {
            intent.putExtra(RECV_DATA, myReceivedData);
            setResult(RESULT_OK, intent);
        }
        stopAll();
        stopReadWrite();
        finish();
    }

    public void onStartServerBtn(View view) {
        createServer();
    }

    private void createServer() {
        enableDiscovery();
        stopAll();
        setState(CONN_STATE_LISTEN);

        myServer = new BtServer(myBtAdapter);
        myServer.start();
    }

    public void createClient(BluetoothDevice bt_device) {
        stopAll();
        setState(CONN_STATE_CONNECTING);
        myClient = new BtClient(bt_device, myBtAdapter);
        myClient.start();
    }

    public void onSyncFinished(byte[] data) {
        byte[] myData = data;
        myData.toString();

    }

    private void exitApp() {
        stopAll();
        stopReadWrite();
        finish();
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + myState + " -> " + state);
        myState = state;

        // Give the new state to the Handler so the UI Activity can update
        myHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public void write(byte[] out) {

        Log.d(TAG, "Write disabled!");

/*
        // Create temporary object
        BtConnected r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (myState != CONN_STATE_CONNECTED) return;
            r = myConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
*/
    }


    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, boolean is_server/*, final String socketType*/) {

        stopAll();
        if (myConnectedWriterThread != null) {myConnectedWriterThread.cancel(); myConnectedWriterThread = null;}
        if (myConnectedReaderThread != null) {myConnectedReaderThread.cancel(); myConnectedReaderThread = null;}

        // Start the thread to manage the connection and perform transmissions
        myConnectedReaderThread = new BtConnected(socket, DATA_READER, is_server);
        myConnectedReaderThread.start();

        myConnectedWriterThread = new BtConnected(socket, DATA_WRITER, is_server);
        myConnectedWriterThread.start();


        Log.d(TAG, "Started connected thread");

        // Send the name of the connected device back to the UI Activity
        Message msg = myHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        myHandler.sendMessage(msg);

        setState(CONN_STATE_CONNECTED);


    }

    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = myHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        myHandler.sendMessage(msg);

        Log.d(TAG, "Connection failed");
        // Start the service over to restart listening mode
        //this.start();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = myHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Device connection was lost");
        msg.setData(bundle);
        myHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        //this.start();
    }

    private void toast(String str) {
        Message msg = myHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, str);
        msg.setData(bundle);
        myHandler.sendMessage(msg);
    }



    /************************************************************************************
     * Server connection thread
     ***********************************************************************************/
    private class BtServer extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        private static final String TAG = "BtServer";

        public BtServer(BluetoothAdapter bt_adapter) {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = bt_adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID);
            } catch (IOException e) {
                Log.d(TAG, "Failed to create RFCOMM for listen");
            }
            Log.d(TAG, "Created");
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            Log.d(TAG, "Listening");


            while (CONN_STATE_CONNECTED != myState) { // if connected cancel will be called in manageConnectedSocket()
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.d(TAG, "Exception during accept");
                    e.printStackTrace();
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    synchronized (this) {
                        switch (myState) {
                            case CONN_STATE_LISTEN:
                            case CONN_STATE_CONNECTING:
                                manageConnectedSocket(socket);
                                break;
                            case CONN_STATE_NONE:
                            case CONN_STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.d(TAG, "Exception when closing server socket");
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }
            }
            Log.d(TAG, "Exiting Server thread");
        }


        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Exception when closing server socket");
                e.printStackTrace();
            }
            Log.d(TAG, "Closing server socked from main thread");
        }

        private void manageConnectedSocket(BluetoothSocket bt_socket) {
            // do stuff in a new thread
            Log.d(TAG, "Client accepted");
            connected(bt_socket, bt_socket.getRemoteDevice(), true);
        }
    }


    /************************************************************************************
     * Client connection thread
     ***********************************************************************************/
    private class BtClient extends Thread {
        private final BluetoothSocket mySocket;
        private final BluetoothDevice myDevice; // remove device to connect to
        private final BluetoothAdapter myAdapter;

        private static final String TAG = "BtClient";

        public BtClient(BluetoothDevice bt_device, BluetoothAdapter bt_adapter) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            myDevice = bt_device;
            myAdapter = bt_adapter;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = bt_device.createRfcommSocketToServiceRecord(SERVICE_UUID);
            } catch (IOException e) {
                // popup dialog here
                Log.d(TAG, "Failed to create RFCOMM socket");
            }
            mySocket = tmp;
            Log.d(TAG, "Created");
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            myAdapter.cancelDiscovery();

            if(mySocket == null) {
                toast("Socket is null");
                return;
            }

            Log.d(TAG, "Connecting...");

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mySocket.connect();
            } catch (IOException connectException) {
                Log.d(TAG, "Unable to connect; close the socket and get out");
                connectException.printStackTrace();
                try {
                    mySocket.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (this) {
                myClient = null;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mySocket);
        }


        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mySocket.close();
            } catch (IOException e) { }
            Log.d(TAG, "Closing socket from main thread");
        }

        private void manageConnectedSocket(BluetoothSocket bt_socket) {
            //... do stuff in a separate thread
            Log.d(TAG, "Connected to server!");
            connected(bt_socket, myDevice, false);
        }
    }


    /************************************************************************************
     * Connected thread
     ***********************************************************************************/
    private class BtConnected extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int myType;
        private final boolean myIsServer;
        private final String TAG;

        public BtConnected(BluetoothSocket socket, int type, boolean is_server) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            myType = type;
            myIsServer = is_server;
            TAG = "BtConn" + (is_server? "_SRV_" : "_CLI_") + (type==DATA_READER?"reader":"writer");

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();

            } catch (IOException e) {
                Log.d(TAG, "Exception when trying to get Stream");
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            if(myType == DATA_READER)
                startReader();
            else
                startWriter();
        }

        void startWriter() {
            //byte[] buffer = new byte[1024];  // buffer store for the stream
            //int bytes; // bytes returned from read()

            //byte[] msg = {'D', 'T', 'A', '0', 0, 0, 0, 6, 'l','a','l','a','l', 'a', 'E','D','T','A' };
            byte[] msg = BtStreamParser.encode(myDataToSend);
            boolean b_finished = false;

            // Keep listening to the InputStream until an exception occurs
            while (!b_finished) {
                try {
                    Log.d(TAG, "write");

                    boolean b_debug = true;
                    if(b_debug) {
                        int len = msg.length;
                        int chunk_size = 10;
                        int steps = (len + chunk_size - 1) / chunk_size;
                        int offset = 0;
                        for (int i = 0; i < steps; ++i) {
                            mmOutStream.write(msg, offset, Math.min(chunk_size, len - offset));
                            offset += chunk_size;
                            sleep(250);
                        }
                    } else {
                        mmOutStream.write(msg);
                    }
                    b_finished = true;
                    myHandler.obtainMessage(MESSAGE_DATA_WRITTEN, -1, -1).sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "write disconnected", e);
                    connectionLost();
                    e.printStackTrace();
                    break;
                } catch(Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        void startReader() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            BtStreamParser parser = new BtStreamParser();
            boolean b_finished = false;

            // Keep listening to the InputStream until an exception occurs
            /*while (true)*/ {
                try {

                    Log.d(TAG, "read");
                    // Read from the InputStream
                    do {
                        bytes = mmInStream.read(buffer);
                        int rv = parser.parse(buffer, bytes);
                        b_finished = rv==0;
                        if(rv==-1) {
                            Log.d(TAG, "Error during parsing received data, resetting and restarting");
                            parser.reset();
                            b_finished = false;
                        }
                    } while(!b_finished);

                    // Send the obtained bytes to the UI activity
                    //myHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                    String res = "";
                    if(parser.IsComplete())
                        res = parser.getDataAsString();
                    myHandler.obtainMessage(MESSAGE_DATA_READ, bytes, -1, res).sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "read disconnected", e);
                    connectionLost();
                    e.printStackTrace();
                    //break;
                } catch(Exception e) {
                    e.printStackTrace();
                    //break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                // Share the sent message back to the UI Activity
                myHandler.obtainMessage(MESSAGE_WRITE, -1, -1, bytes).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
