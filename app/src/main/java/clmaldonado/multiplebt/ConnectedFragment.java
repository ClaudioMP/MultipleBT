package clmaldonado.multiplebt;


import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.*;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.*;
import java.util.ArrayList;


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
    String FileName = "Data.csv";
    FileOutputStream salida;
    ConnectedThread[] threads;
    Handler connectedHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    int[] rB = (int[])msg.obj;
                    int[] a = Angulos(rB);
                    int i = msg.arg2;
                    int s = msg.arg1;
                    new Actualizar().execute(s,i,a[0],a[1],a[2]);
                    if(i%2==0){
                        if(s==1){
                            new Grafico1().execute((float)i,(float)a[0],(float)a[1],(float)a[2]);
                        }
                        else{
                            new Grafico2().execute((float)i,(float)a[0],(float)a[1],(float)a[2]);
                        }
                    }
                    break;
                case 2:
                    Toast.makeText(getActivity().getBaseContext(), "\u26A0 " + msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(getActivity().getBaseContext(),(String)msg.obj,Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

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
        RevisarEstado();
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

    public void RevisarEstado(){
        // Acá revisamos si el almacenamiento externo está disponible
        String estado = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(estado)){
            File Root = Environment.getExternalStorageDirectory();
            File Dir = new File(Root.getAbsolutePath()+"/MdeT");
            if(!Dir.exists()){
                Dir.mkdir();
            }
            File archivo = new File(Dir,FileName);
            try {
                salida = new FileOutputStream(archivo);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(getActivity().getBaseContext(),"SD card not available",Toast.LENGTH_LONG).show();
        }
        System.out.println("Se puede escribir");
    }

    private int[] Angulos(int[] d){
        for(int i=0;i<4;i++){
            d[i] = (d[i]<0)?d[i]+=256:d[i];
        }
        int[] angulos = new int[3];
        float w,x,y,z;
        w = (d[0]/100.00f)-1;
        x = (d[1]/100.00f)-1;
        y = (d[2]/100.00f)-1;
        z = (d[3]/100.00f)-1;
        double pitch = Math.toDegrees(-Math.asin(2*(x*z - w*y))); // pitch
        double roll = Math.toDegrees(Math.atan2(2*(w*x + y*z), w*w - x*x - y*y + z*z)); // roll
        double yaw = Math.toDegrees(Math.atan2(2*(x*y + w*z), 1 - 2*(y*y + z*z))); // yaw
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
                    threads[j++]=new ConnectedThread(s,connectedHandler);
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
        private Handler mHandler;
        private InputStream inputStream;
        private String name;
        private int sensor;

        private ConnectedThread(BluetoothSocket s,Handler h){
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

    private class Actualizar extends AsyncTask<Integer,Void,Void>{
        @Override
        protected Void doInBackground(Integer... p) {
            //sensor,indice,pitch,roll,yaw
            float[] data = new float[5];
            data[0] = (float)p[0];
            data[1] = (float)p[1];
            data[2] = p[2]/100.00f;
            data[3] = p[3]/100.00f;
            data[4] = p[4]/100.00f;
            String val = p[0].toString()+","+p[1].toString()+","+data[2]+","+data[3]+","+data[4];
            try {
                salida.write(val.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class Grafico1 extends AsyncTask<Float,Void,DataPoint[]>{
        @Override
        protected DataPoint[] doInBackground(Float... p) {

            return new DataPoint[0];
        }

        @Override
        protected void onPostExecute(DataPoint[] dataPoints) {
            super.onPostExecute(dataPoints);
        }
    }
    private class Grafico2 extends AsyncTask<Float,Void,DataPoint[]>{
        @Override
        protected DataPoint[] doInBackground(Float... p) {

            return new DataPoint[0];
        }

        @Override
        protected void onPostExecute(DataPoint[] dataPoints) {
            super.onPostExecute(dataPoints);
        }
    }
}
