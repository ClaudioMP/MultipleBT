package clmaldonado.multiplebt;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectionFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener{
    ListView lista;
    TextView tvConectados;
    Button btnReceive;
    Comunicador comunicador;
    ArrayAdapter<String> arrayAdapter;
    Set<BluetoothDevice> paired_devices;
    BluetoothAdapter btAdapter;
    String[] plist;
    ArrayList<BluetoothDevice> devices;
    ArrayList<BluetoothSocket> sockets;
    Handler connectionHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    BluetoothSocket tmp = (BluetoothSocket)msg.obj;
                    sockets.add(tmp);
                    tvConectados.append("- " + tmp.getRemoteDevice().getName() + "\n");
                    break;
                case 2:
                    Toast.makeText(getActivity().getBaseContext(),"\u26A0 "+msg.obj,Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(getActivity().getBaseContext(), (String) msg.obj,Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public ConnectionFragment() {
        // Required empty public constructor
    }

    public void GetAdapter(BluetoothAdapter a){
        btAdapter = a;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connection, container, false);
        comunicador = (Comunicador)getActivity();
        devices = new ArrayList<>();
        sockets = new ArrayList<>();
        lista = (ListView) view.findViewById(R.id.Lista);
        tvConectados = (TextView)view.findViewById(R.id.tvConectados);
        btnReceive = (Button)view.findViewById(R.id.btnNext);
        btnReceive.setOnClickListener(this);
        paired_devices = btAdapter.getBondedDevices();
        plist= new String[paired_devices.size()];
        int j = 0;
        for (BluetoothDevice dev: paired_devices){
            plist[j]=dev.getName()+"\n"+dev.getAddress();
            devices.add(j++,dev);
            System.out.println("Agregado: "+ dev.getName());
        }
        arrayAdapter = new ArrayAdapter<>(getActivity().getBaseContext(),android.R.layout.simple_list_item_1,plist);
        lista.setAdapter(arrayAdapter);
        lista.setOnItemClickListener(this);
        return view;

    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String aviso = "Conectando con "+devices.get(position).getName();
        Toast.makeText(getActivity().getBaseContext(), aviso, Toast.LENGTH_SHORT);
        new ConnectionThread(devices.get(position),connectionHandler,btAdapter).start();
    }

    @Override
    public void onClick(View v) {
        for(BluetoothSocket socket: sockets){
            if(socket.isConnected()){
                System.out.println(socket.getRemoteDevice().getName()+" Conectado");
            }
        }
        comunicador.PasaSockets(sockets);
    }

    private class ConnectionThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final BluetoothAdapter mmAdapter;
        private final Handler mHandler;
        // UUID for serial communication
        private final UUID spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        private ConnectionThread(BluetoothDevice dev,Handler handler,BluetoothAdapter adapter){
            mmDevice = dev;
            mHandler = handler;
            mmAdapter = adapter;
            BluetoothSocket sock = null;
            try {
                sock = dev.createRfcommSocketToServiceRecord(spp);
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.obtainMessage(2,"Error al crear el Socket").sendToTarget();
            }
            mmSocket = sock;
        }

        public void run(){
            mmAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                mHandler.obtainMessage(2,"Error al conectar").sendToTarget();
                e.printStackTrace();
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return;
            }
            System.out.println("Conectado con "+mmDevice.getName());
            mHandler.obtainMessage(3,"Conectado con "+mmDevice.getName()).sendToTarget();
            mHandler.obtainMessage(1,mmSocket).sendToTarget();
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
