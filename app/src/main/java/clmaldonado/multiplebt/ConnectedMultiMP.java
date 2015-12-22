package clmaldonado.multiplebt;


import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */

// TODO: Create the new Angulos function to work properly with multiple sensors connected
public class ConnectedMultiMP extends Fragment implements View.OnClickListener{
    ArrayList<BluetoothSocket> Sockets;
    int cantSockets;
    Recepcion threads[];
    Button iniciar, parar, calibrar;
    LineChart chart1, chart2;
    ArrayList<ArrayList<LineDataSet>> sets;
    public ConnectedMultiMP() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connected_multi_m, container, false);
        iniciar = (Button)view.findViewById(R.id.Receive);
        parar = (Button)view.findViewById(R.id.Stop);
        calibrar = (Button)view.findViewById(R.id.Calibrar);

        iniciar.setOnClickListener(this);
        parar.setOnClickListener(this);
        calibrar.setOnClickListener(this);
        return view;
    }

    public void getSockets(ArrayList<BluetoothSocket> socks){
        Sockets = socks;
        cantSockets = Sockets.size();
        System.out.println("Recibí los "+ cantSockets + " sockets");
        threads = new Recepcion[cantSockets];
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            // TODO: write the work of the buttons
            case R.id.Receive:
                // Aquí inicio el thread de recepción para cada Sensor
                break;
            case R.id.Stop:
                // Aquí freno los threads
                break;
            case R.id.Calibrar:
                // Aquí se hace la calibración
                break;
        }
    }

    private class Recepcion extends Thread{
        private BluetoothSocket mmSocket;
        private Handler mHandler;
        private InputStream inputStream;
        private String name;
        private int sensor;

        private Recepcion(BluetoothSocket s,Handler h){
            // TODO: Update the workflow of this thread
            mmSocket = s;
            mHandler = h;
            name = mmSocket.getRemoteDevice().getName();
            sensor = name.equals(Sockets.get(0).getRemoteDevice().getName())?1:2;
            InputStream tmpIn = null;
            try {
                tmpIn = mmSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tmpIn;
            mHandler.obtainMessage(3,"Recibiendo datos de "+name).sendToTarget();
        }

        public void run(){
            int i = 0,bytes;
            try {
                inputStream.reset();
            } catch (IOException e) {
                System.out.println(name+"No se puede resetear el flujo");
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
                    mHandler.obtainMessage(2, "La conexión con "+name+" se cerró").sendToTarget();
                    break;
                }
                mHandler.obtainMessage(1,sensor,i,new int[]{(int)buffer[1],(int)buffer[2],(int)buffer[3],(int)buffer[4]}).sendToTarget();
                //System.out.println(name+","+i+","+(int)buffer[1]+","+(int)buffer[2]+","+(int)buffer[3]+","+(int)buffer[4]);
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
