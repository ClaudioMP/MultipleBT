package clmaldonado.multiplebt;


import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.*;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.*;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */

public class ConnectedMultiMP extends Fragment implements View.OnClickListener{
    ArrayList<BluetoothSocket> Sockets;
    int cantSockets;
    Recepcion[] threads;
    Button iniciar, parar, calibrar;
    LineChart[] charts;
    ArrayList<ArrayList<LineDataSet>> sets = new ArrayList<>();
    LineData[] datos;
    // Layout con las gráficas
    LinearLayout graficos;
    // Para la escritura en archivo
    FileOutputStream fout;
    String FileName = "Data.csv";
    // Para la calibración
    float[] basePitch, baseRoll, baseYaw;
    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case 1:
                    charts[msg.arg1].notifyDataSetChanged();
                    charts[msg.arg1].setVisibleXRangeMaximum(200);
                    charts[msg.arg1].moveViewToX(msg.arg2);
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
    public ConnectedMultiMP() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connected_multi_m, container, false);
        // Botones
        iniciar = (Button)view.findViewById(R.id.Receive);
        parar = (Button)view.findViewById(R.id.Stop);
        calibrar = (Button)view.findViewById(R.id.Calibrar);
        iniciar.setOnClickListener(this);
        parar.setOnClickListener(this);
        calibrar.setOnClickListener(this);
        parar.setClickable(false);
        calibrar.setClickable(false);
        // Gráficas
        graficos = (LinearLayout)view.findViewById(R.id.graficas);
        for(int i=0;i<cantSockets;i++){
            charts[i] = new LineChart(getActivity().getBaseContext());
            charts[i].setMinimumHeight(300);
            graficos.addView(charts[i]);
        }
        ConfiguraGraficos();
        RevisarEstado();
        return view;
    }

    public void ConfiguraGraficos(){
        for(int i=0; i<cantSockets;i++) {
            sets.add(new ArrayList<LineDataSet>());
        }
        int i = 0;
        for(ArrayList<LineDataSet> s:sets){
            s.add(new LineDataSet(new ArrayList<Entry>(),"Pitch"));
            s.add(new LineDataSet(new ArrayList<Entry>(),"Roll"));
            s.add(new LineDataSet(new ArrayList<Entry>(),"Yaw"));
            for(LineDataSet d: s){
                d.setDrawCircles(false);
                d.setDrawValues(false);
                d.setHighlightEnabled(false);
                d.setLineWidth(2.5f);
            }
            s.get(0).setColor(Color.BLACK);
            s.get(1).setColor(Color.RED);
            s.get(2).setColor(Color.GREEN);
            datos[i++] = new LineData(new ArrayList<String>(),s);
        }
        int j=0;
        for(LineChart c: charts){
            c.setNoDataText("No hay datos por el momento");
            c.setDoubleTapToZoomEnabled(false);
            c.setPinchZoom(false);
            c.setHighlightPerDragEnabled(false);
            c.setHighlightPerTapEnabled(false);
            c.setScaleYEnabled(false);
            c.setDescription("");
            c.getLegend().setPosition(Legend.LegendPosition.ABOVE_CHART_RIGHT);
            c.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            c.setDrawBorders(true);
            c.getAxisRight().setDrawAxisLine(false);
            c.setDescription(Sockets.get(j++).getRemoteDevice().getName());
        }
    }

    public void getSockets(ArrayList<BluetoothSocket> socks){
        Sockets = socks;
        cantSockets = Sockets.size();
        System.out.println("Recibí los " + cantSockets + " sockets");
        threads = new Recepcion[cantSockets];
        charts = new LineChart[cantSockets];
        datos = new LineData[cantSockets];
        basePitch = new float[cantSockets];
        baseRoll = new float[cantSockets];
        baseYaw = new float[cantSockets];
        System.out.println("Todo creado, listo para iniciar");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.Receive:
                int j = 0;
                for(LineChart c: charts){
                    c.setData(datos[j++]);
                }
                for(int i = 0;i< cantSockets;i++){
                    threads[i] = new Recepcion(Sockets.get(i),handler,i);
                }
                for (int i=0;i<cantSockets;i++){
                    threads[i].start();
                }
                parar.setClickable(true);
                calibrar.setClickable(true);
                iniciar.setVisibility(View.GONE);
                break;
            case R.id.Stop:
                for(Recepcion th:threads){
                    th.cancel();
                }
                break;
            case R.id.Calibrar:
                for(int i=0;i<cantSockets;i++){
                    basePitch[i] = datos[i].getDataSetByIndex(0).getYValForXIndex(datos[i].getXValCount()-2);
                    datos[i].getDataSetByIndex(0).clear();
                    baseRoll[i] = datos[i].getDataSetByIndex(1).getYValForXIndex(datos[i].getXValCount()-2);
                    datos[i].getDataSetByIndex(1).clear();
                    baseYaw[i] = datos[i].getDataSetByIndex(2).getYValForXIndex(datos[i].getXValCount()-2);
                    datos[i].getDataSetByIndex(2).clear();
                    charts[i].invalidate();
                }
                calibrar.setClickable(false);
                calibrar.setVisibility(View.GONE);
                break;
        }
    }

    public void PararTodo(){
        // Esta función se usará desde el MainActivity para frenar los threads cuando se vuelve atrás sin clickear el botón Parar
        for(int i=0;i<cantSockets;i++){
            threads[i].cancel();
        }
    }

    private void Angulos(int sensor, int index, byte[] rawData){
        int[] d = new int[4];
        for(int i=0;i<4;i++){
            d[i] = rawData[i+1]<0?(int)rawData[i+1]+256:(int)rawData[i+1];
        }
        float w,x,y,z;
        w = (d[0]/100.00f)-1;
        x = (d[1]/100.00f)-1;
        y = (d[2]/100.00f)-1;
        z = (d[3]/100.00f)-1;
        double[] angulos = new double[3];
        angulos[0] = Math.toDegrees(-Math.asin(2*(x*z - w*y))) - basePitch[sensor]; // pitch
        angulos[1] = Math.toDegrees(Math.atan2(2 * (w * x + y * z), w * w - x * x - y * y + z * z)) - baseRoll[sensor]; // roll
        angulos[2] = Math.toDegrees(Math.atan2(2 * (x * y + w * z), 1 - 2 * (y * y + z * z))) - baseYaw[sensor];// yaw
        datos[sensor].addXValue(index + "");
        for(int i=0;i<3;i++){
            datos[sensor].addEntry(new Entry((((int) ((angulos[i] + 0.005) * 100)) / 100f), index), i);
        }
        new Archivo().execute(sensor,index,d[0],d[1],d[2],d[3]);
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
                fout = new FileOutputStream(archivo);
                System.out.println("Se puede escribir");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(getActivity().getBaseContext(),"SD card not available",Toast.LENGTH_LONG).show();
        }

    }
    private class Recepcion extends Thread{
        private BluetoothSocket mmSocket;
        private Handler mHandler;
        private InputStream inputStream;
        private String name;
        private int sensor;

        public Recepcion(BluetoothSocket s,Handler h,int n){
            mmSocket = s;
            mHandler = h;
            name = mmSocket.getRemoteDevice().getName();
            sensor = n;
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
                inputStream.read();
                sleep(10,0);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
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
                    System.out.println(e.toString());
                    mHandler.obtainMessage(2, "La conexión con "+name+" se cerró").sendToTarget();
                    break;
                }
                //mHandler.obtainMessage(4,sensor-1,i,buffer).sendToTarget();
                Angulos(sensor, i, buffer);
                if(i%10==0) {
                    mHandler.obtainMessage(1, sensor, i).sendToTarget();
                }
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

    public class Archivo extends AsyncTask<Integer,Void,Void>{

        @Override
        protected Void doInBackground(Integer... p) {
            // sensor, index, w,x,y,z
            String out = p[0]+","+p[1]+","+p[2]/100.00f+","+p[3]/100.00f+","+p[4]/100.00f+","+p[5]/100.00f+"\n";
            try {
                fout.write(out.getBytes());
                //System.out.println(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
