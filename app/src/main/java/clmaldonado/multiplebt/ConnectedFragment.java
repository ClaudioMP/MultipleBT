package clmaldonado.multiplebt;


import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectedFragment extends Fragment implements View.OnClickListener{
    ArrayList<BluetoothSocket> Sockets;
    int cantSockets;
    Button start, stop;
    ConnectedThread[] threads;

    public ConnectedFragment() {
        // Required empty public constructor
    }
    public void getSockets(ArrayList<BluetoothSocket> socks){
        Sockets = socks;
        cantSockets = Sockets.size();
        System.out.println("Recibí los "+ cantSockets + " sockets");
        threads = new ConnectedThread[cantSockets];
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connected, container, false);
        start = (Button)view.findViewById(R.id.btnInicio);
        stop = (Button)view.findViewById(R.id.btnFin);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnInicio:
                int j=0;
                for(BluetoothSocket s: Sockets){
                    threads[j++]=new ConnectedThread(s);
                }
                for(int i=0;i<threads.length;i++){
                    threads[i].start();
                }
                break;
            case R.id.btnFin:
                // Acá cierro todos los Thread
                for(int i=0;i<threads.length;i++){
                    threads[i].cancel();
                }
                break;
        }
    }

    private class ConnectedThread extends Thread{
        private BluetoothSocket mmSocket;
        //private Handler mHandler;
        private InputStream inputStream;
        private String name;

        private ConnectedThread(BluetoothSocket s){
            mmSocket = s;
            //mHandler = h;
            name = mmSocket.getRemoteDevice().getName();
            InputStream tmpIn = null;
            try {
                tmpIn = mmSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tmpIn;
            //mHandler.obtainMessage(3,"Recibiendo datos de "+name).sendToTarget();
        }

        public void run(){
            int i = 0,bytes;
            try {
                inputStream.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while(true){
                i++;
                byte[] buffer = new byte[5];
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes < 5){
                        inputStream.read(buffer,bytes,5-bytes);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Connected Thread: Socket con " + name + " cerrado");
                    //mHandler.obtainMessage(2, "La conexión con "+name+" se cerró").sendToTarget();
                    break;
                }
                System.out.println("Dato de "+name+":"+(int)buffer[1]+","+(int)buffer[2]+","+(int)buffer[3]+","+(int)buffer[4]);
                try {
                    sleep(10,0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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
