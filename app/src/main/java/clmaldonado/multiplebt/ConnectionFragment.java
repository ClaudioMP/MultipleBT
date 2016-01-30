package clmaldonado.multiplebt;


import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectionFragment extends Fragment implements AdapterView.OnItemClickListener{
    ListView lista;
    Comunicador comunicador;
    CustomAdapter arrayAdapter;
    Set<BluetoothDevice> paired_devices;
    BluetoothAdapter btAdapter;
    ArrayList<Dispositivo> plist;
    ArrayList<BluetoothDevice> devices;
    ArrayList<BluetoothSocket> sockets;
    ArrayList<Limpieza> cleaning = new ArrayList<>();
    // To the ListView of Connected Devices
    ListView connecteddevices;
    CustomAdapter customAdapterconnected;
    ArrayList<Dispositivo> conectados;

    public String name = "Default";
    Handler connectionHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    BluetoothSocket tmp = (BluetoothSocket)msg.obj;
                    sockets.add(tmp);
                    conectados.add(new Dispositivo(tmp.getRemoteDevice().getName(),tmp.getRemoteDevice().getAddress()));
                    customAdapterconnected.notifyDataSetChanged();
                    cleaning.add(new Limpieza(tmp));
                    cleaning.get(cleaning.size()-1).start();
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
        paired_devices = btAdapter.getBondedDevices();
        int j = 0;
        plist = new ArrayList<>();
        for (BluetoothDevice dev: paired_devices){
            plist.add(new Dispositivo(dev.getName(),dev.getAddress()));
            devices.add(j++,dev);
            System.out.println("Agregado: "+ dev.getName());
        }
        // Lista de dispositivos conectados
        connecteddevices = (ListView)view.findViewById(R.id.ListaConectados);
        conectados = new ArrayList<>();
        customAdapterconnected = new CustomAdapter(getActivity().getApplicationContext(),conectados,R.layout.custom_list_connected);
        connecteddevices.setAdapter(customAdapterconnected);
        // TODO: Modificar el ListView de dispositivos disponibles
        arrayAdapter = new CustomAdapter(getActivity().getApplicationContext(),plist,R.layout.custom_list_available);
        lista.setAdapter(arrayAdapter);
        lista.setOnItemClickListener(this);
        return view;

    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String aviso = "Conectando con "+devices.get(position).getName();
        Toast.makeText(getActivity().getBaseContext(), aviso, Toast.LENGTH_SHORT).show();
        new ConnectionThread(devices.get(position), connectionHandler, btAdapter).start();

    }

    public void PasarALosGraficos(){
        for(BluetoothSocket socket: sockets){
            if(socket.isConnected()){
                System.out.println(socket.getRemoteDevice().getName()+" conectado");
            }
        }
        // Here we launch a Dialog requesting the name of the patient
        View view = LayoutInflater.from(this.getActivity()).inflate(R.layout.dialog_name,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this.getActivity());
        alertBuilder.setView(view);
        final EditText et = (EditText)view.findViewById(R.id.etAskName);
        alertBuilder
                .setMessage(R.string.AskName)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        name = et.getText().equals("")?name:et.getText().toString();
                        comunicador.PasaSockets(sockets,name);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    private class ConnectionThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final BluetoothAdapter mmAdapter;
        private final Handler mHandler;
        private InputStream is;
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
                mHandler.obtainMessage(2,getString(R.string.ErrorSocket)).sendToTarget();
            }
            mmSocket = sock;
        }

        public void run(){
            mmAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                mHandler.obtainMessage(2,getString(R.string.ConnectingError)+" "+mmDevice.getName()).sendToTarget();
                System.out.println(e.toString());
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    System.out.println(e1.toString());
                }
                return;
            }
            System.out.println("Conectado con " + mmDevice.getName());
            mHandler.obtainMessage(3,getString(R.string.Connectedwith)+" "+mmDevice.getName()).sendToTarget();
            mHandler.obtainMessage(1,mmSocket).sendToTarget();
        }

    }

    private class Limpieza extends Thread{
        private BluetoothSocket mmSocket;
        private InputStream is;

        private Limpieza(BluetoothSocket sock){
            mmSocket = sock;
            InputStream tmpIn = null;
            try {
                tmpIn = mmSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            is = tmpIn;
        }

        public void run(){
            while (true) {
                try {
                    if(mmSocket.isConnected()) {
                        int av = is.available();
                        if (av > 100) {
                            is.read(new byte[av], 0, av);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
