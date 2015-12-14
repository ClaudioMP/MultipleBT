package clmaldonado.multiplebt;


import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TabHost;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectedFragment extends Fragment implements View.OnClickListener{
    ArrayList<BluetoothSocket> Sockets;
    GraphView G1,G2;
    TabHost tabHost;
    TabHost.TabSpec tab1,tab2;
    LineGraphSeries<DataPoint> s11,s12,s13,s21,s22,s23;
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
        // Declaración de las pestañas
        tabHost = (TabHost)view.findViewById(R.id.tabHost);
        // Seteamos la primera pestaña
        tabHost.setup();
        tab1 = tabHost.newTabSpec("Tab1");
        tab1.setIndicator(Sockets.get(0).getRemoteDevice().getName());
        tab1.setContent(R.id.linearLayout);
        tabHost.addTab(tab1);
        // Seteamos la segunda pestaña
        tabHost.setup();
        tab2 = tabHost.newTabSpec("Tab2");
        tab2.setIndicator(Sockets.get(1).getRemoteDevice().getName());
        tab2.setContent(R.id.linearLayout2);
        tabHost.addTab(tab2);
        // Creación de los gráficos para dos sensores
        G1 = (GraphView)view.findViewById(R.id.Graph1);
        G2 = (GraphView)view.findViewById(R.id.Graph2);
        // Series del gráfico 1
        s11 = new LineGraphSeries<>(new DataPoint[]{});
        s12 = new LineGraphSeries<>(new DataPoint[]{});
        s13 = new LineGraphSeries<>(new DataPoint[]{});
        // Series del gráfico 2
        s21 = new LineGraphSeries<>(new DataPoint[]{});
        s22 = new LineGraphSeries<>(new DataPoint[]{});
        s23 = new LineGraphSeries<>(new DataPoint[]{});
        G1.addSeries(s11);
        G1.addSeries(s12);
        G1.addSeries(s13);
        ConfigurarGraficos(G1,s11,s12,s13);
        G2.addSeries(s21);
        G2.addSeries(s22);
        G2.addSeries(s23);
        ConfigurarGraficos(G2,s21,s22,s23);
        return view;
    }

    private void ConfigurarGraficos(GraphView G, LineGraphSeries s1,LineGraphSeries s2,LineGraphSeries s3){
        s1.setTitle("Pitch");
        s1.setColor(Color.GREEN);
        s2.setTitle("Roll");
        s2.setColor(Color.RED);
        s3.setTitle("Yaw");
        s3.setColor(Color.BLACK);
        G.getLegendRenderer().setVisible(true);
        G.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        G.getViewport().setXAxisBoundsManual(true);
        G.getViewport().setMinX(0);
        G.getViewport().setMaxX(200);
        G.onDataChanged(false, false);
        G.getViewport().setScrollable(true);

    }

    private int[] Angulos(int[] d){
        for(int i=1;i<=4;i++){
            if(d[i]<0) d[i]+=256;
        }
        int[] angulos = new int[3];
        float w,x,y,z;
        w = (d[1]/100.00f)-1;
        x = (d[2]/100.00f)-1;
        y = (d[3]/100.00f)-1;
        z = (d[4]/100.00f)-1;
        double pitch = Math.toDegrees(-Math.asin(2*(x*z - w*y))); // pitch
        double roll = Math.toDegrees(Math.atan2(2 * (w * x + y * z), w * w - x * x - y * y + z * z)); // roll
        double yaw = Math.toDegrees(Math.atan2(2 * (x * y + w * z), 1 - 2 * (y * y + z * z))); // yaw
        angulos[0] = (int)((pitch+0.005)*100);
        angulos[1] = (int)((roll+0.005)*100);
        angulos[2] = (int)((yaw+0.005)*100);
        return angulos;
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
                System.out.println("Voy a cerrar los sockets!");
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
                    //mHandler.obtainMessage(2, "La conexión con "+name+" se cerró").sendToTarget();
                    break;
                }
                System.out.println(name+","+i+","+(int)buffer[1]+","+(int)buffer[2]+","+(int)buffer[3]+","+(int)buffer[4]);
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
