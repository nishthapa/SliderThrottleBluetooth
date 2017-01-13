package bluetooth.control.motor.brushless.sliderthrottlebluetooth;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class SliderThrottle extends AppCompatActivity implements OnSeekBarChangeListener
{
    private SeekBar throttle_stick;
    private BluetoothAdapter BA;
    public OutputStream out;
    private Button sel_device,disconnect;
    private static final int ENABLE_BT_REQUEST_CODE = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    public String item_value,THROTTLE_VAL_STR;
    public ProgressDialog pgd;
    public BluetoothDevice remote_device;
    private final static UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Set<BluetoothDevice>pairedDevices;
    ListView lv;
    SendingThread SND;
    int THROTTLE_VALUE,i,THROTTLE_MIN = 1000,THROTTLE_MAX = 2000,STEP = 1;
    TextView throttle_val,zero_label,half_label,full_label,throttle_label;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slider_throttle);
        BA = BluetoothAdapter.getDefaultAdapter();
        lv = (ListView)findViewById(R.id.listView);
        throttle_stick = (SeekBar)findViewById(R.id.throttle_slider);
        throttle_stick.setMax((THROTTLE_MAX - THROTTLE_MIN) / STEP);
        throttle_val = (TextView)findViewById(R.id.throttle_label_val);
        throttle_label = (TextView)findViewById(R.id.throttle_label);
        zero_label = (TextView)findViewById(R.id.zero_label);
        half_label = (TextView)findViewById(R.id.half_label);
        full_label = (TextView)findViewById(R.id.full_label);
        pgd = new ProgressDialog(this);
        pgd.setCancelable(false);
        throttle_stick.setOnSeekBarChangeListener(this);
        sel_device = (Button)findViewById(R.id.sel_dev_button);
        disconnect = (Button)findViewById(R.id.disconnect_button);

        sel_device.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if (!BA.isEnabled())
                {
                    SwitchOn(BA);
                }
                else
                {
                    list();
                    sel_device.setVisibility(View.GONE);
                    lv.setVisibility(View.VISIBLE);
                }
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                item_value = (String) lv.getItemAtPosition(position);
                String MAC = item_value.substring(item_value.length() - 17);
                remote_device = BA.getRemoteDevice(MAC);
                lv.setVisibility(View.GONE);
                pgd.setMessage("Connecting to :\n" + item_value);
                pgd.show();
                ConnectingThread t = new ConnectingThread(remote_device);
                t.start();
            }
        });

        disconnect.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                for(i=0;i<20;i++)
                {
                    SendingThread sn = new SendingThread(String.valueOf(0)); //for safety so that the motor stops when
                    sn.start();                                              //disconnecting and doesnt keep on spinning
                }
                ConnectingThread t = new ConnectingThread(remote_device);
                t.cancel();
                Reinitialize();
            }
        });
    }

    public void list()
    {
        pairedDevices = BA.getBondedDevices();
        ArrayList list = new ArrayList();
        for (BluetoothDevice bt : pairedDevices)
            list.add(bt.getName()+"\n"+bt.getAddress());
        Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        //progress = (int)map(progress,0,100,0,9);
        //THROTTLE_VALUE = progress;
        THROTTLE_VALUE = THROTTLE_MIN + (progress * STEP);
        THROTTLE_VAL_STR = String.valueOf(THROTTLE_VALUE);
        //THROTTLE_VALUE = (int)map(THROTTLE_VALUE,0,100,0,9);
        //THROTTLE_VAL_STR = String.valueOf(THROTTLE_VALUE);
        SND = new SendingThread(THROTTLE_VAL_STR);
        SND.start();
        THROTTLE_VALUE = (int)map(THROTTLE_VALUE,1000,2000,0,100);
        THROTTLE_VAL_STR = String.valueOf(THROTTLE_VALUE);
        throttle_val.setText (THROTTLE_VAL_STR + " %");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {

    }

    public void SwitchOn(BluetoothAdapter ba)
    {
        ba=BA;
        if(!ba.isEnabled())
        {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn,REQUEST_ENABLE_BT);
        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == ENABLE_BT_REQUEST_CODE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                Toast.makeText(getApplicationContext(), "Bluetooth has been Enabled",Toast.LENGTH_SHORT).show();
                //ListeningThread t = new ListeningThread();
                //t.start();
            }
            if(resultCode == Activity.RESULT_CANCELED)
            {
                Toast.makeText(getApplicationContext(), "Please Enable Bluetooth first",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public class ConnectingThread extends Thread
    {
        public final BluetoothSocket bluetoothSocket;
        public final BluetoothDevice bluetoothDevice;
        public ConnectingThread(BluetoothDevice device)
        {
            BluetoothSocket temp = null;
            bluetoothDevice = device;
            try
            {
                temp = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            }
            catch(IOException e)
            {
                Toaster("Unable to initiate Bluetooth Device");
            }
            bluetoothSocket = temp;
        }
        public void run()
        {
            BA.cancelDiscovery();
            try
            {
                bluetoothSocket.connect();
                out = bluetoothSocket.getOutputStream();
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        Toast.makeText(getApplicationContext(), "Successfully Connected", Toast.LENGTH_SHORT).show();
                        ConnectedView();
                    }
                });
            }
            catch(IOException e)
            {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        Toast.makeText(getApplicationContext(), "Unable to connect !\nCheck that the device is turned ON", Toast.LENGTH_LONG).show();
                        Reinitialize();
                    }
                });
            }
        }
        public void cancel()
        {
            try
            {
                out.close();
                bluetoothSocket.close();
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        Toast.makeText(getApplicationContext(), "Disconnected !", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            catch(IOException e)
            {
                Toaster("Connection Termination Unsuccessful");
            }
        }
    }

    public class SendingThread extends Thread
    {
        byte buffer[];
        public SendingThread(String data)

        {
            buffer = data.getBytes();
        }
        public void run()
        {
            try
            {
                out.write(buffer);
                //Thread.sleep(50);
                //buffer1 = buffer;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void Toaster(String msg)
    {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public void ConnectedView()
    {
        pgd.hide();
        throttle_stick.setProgress(0);
        disconnect.setVisibility(View.VISIBLE);
        throttle_stick.setVisibility(View.VISIBLE);
        zero_label.setVisibility(View.VISIBLE);
        half_label.setVisibility(View.VISIBLE);
        full_label.setVisibility(View.VISIBLE);
        throttle_val.setVisibility(View.VISIBLE);
        throttle_label.setVisibility(View.VISIBLE);
    }

    public void Reinitialize()
    {
        pgd.hide();
        throttle_stick.setProgress(0);
        disconnect.setVisibility(View.GONE);
        throttle_stick.setVisibility(View.GONE);
        zero_label.setVisibility(View.GONE);
        half_label.setVisibility(View.GONE);
        full_label.setVisibility(View.GONE);
        throttle_val.setVisibility(View.GONE);
        throttle_label.setVisibility(View.GONE);
        sel_device.setVisibility(View.VISIBLE);
    }

    long map(long x, long in_min, long in_max, long out_min, long out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }
}
