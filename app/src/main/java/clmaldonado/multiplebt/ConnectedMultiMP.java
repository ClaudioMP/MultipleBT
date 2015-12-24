package clmaldonado.multiplebt;


import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.*;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    LineChart[] charts = new LineChart[2];
    ArrayList<ArrayList<LineDataSet>> sets = new ArrayList<>();
    LineData[] datos = new LineData[2];
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
        charts[0] = (LineChart)view.findViewById(R.id.chart1);
        charts[1] = (LineChart)view.findViewById(R.id.chart2);
        ConfiguraGraficos();
        return view;
    }

    public void ConfiguraGraficos(){
        sets.add(new ArrayList<LineDataSet>());
        sets.add(new ArrayList<LineDataSet>());
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
        basePitch = new float[cantSockets];
        baseRoll = new float[cantSockets];
        baseYaw = new float[cantSockets];

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
                    threads[i] = new Recepcion(Sockets.get(i),handler);
                }
                threads[0].start();
                threads[1].start();
                parar.setClickable(true);
                calibrar.setClickable(true);
                break;
            case R.id.Stop:
                for(Recepcion th:threads){
                    th.cancel();
                }
                break;
            case R.id.Calibrar:
                for(int i=0;i<cantSockets;i++){
                    basePitch[i] = datos[i].getDataSetByIndex(0).getYValForXIndex(datos[i].getXValCount()-2);
                    baseRoll[i] = datos[i].getDataSetByIndex(1).getYValForXIndex(datos[i].getXValCount()-2);
                    baseYaw[i] = datos[i].getDataSetByIndex(2).getYValForXIndex(datos[i].getXValCount()-2);
                }
                calibrar.setClickable(false);
                calibrar.setVisibility(View.GONE);
                break;
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
        String out = sensor+","+index+","+w+","+x+","+y+","+z+"\n";
        try {
            fout.write(out.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        double[] angulos = new double[3];
        angulos[0] = Math.toDegrees(-Math.asin(2*(x*z - w*y))) - basePitch[sensor]; // pitch
        angulos[1] = Math.toDegrees(Math.atan2(2 * (w * x + y * z), w * w - x * x - y * y + z * z)) - baseRoll[sensor]; // roll
        angulos[2] = Math.toDegrees(Math.atan2(2 * (x * y + w * z), 1 - 2 * (y * y + z * z))) - baseYaw[sensor];// yaw
        datos[sensor].addXValue(index + "");
        for(int i=0;i<3;i++){
            datos[sensor].addEntry(new Entry((((int) ((angulos[i] + 0.005) * 100)) / 100f), index), i);
        }
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
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(getActivity().getBaseContext(),"SD card not available",Toast.LENGTH_LONG).show();
        }
        System.out.println("Se puede escribir");
    }

    private class Recepcion extends Thread{
        private BluetoothSocket mmSocket;
        private Handler mHandler;
        private InputStream inputStream;
        private String name;
        private int sensor;

        private Recepcion(BluetoothSocket s,Handler h){
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
                inputStream.read();
                sleep(10);
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
                    e.printStackTrace();
                    System.out.println("Connected Thread: Socket con " + name + " cerrado");
                    mHandler.obtainMessage(2, "La conexión con "+name+" se cerró").sendToTarget();
                    break;
                }
                Angulos(sensor-1, i, buffer);
                if(i%10==0) {
                    mHandler.obtainMessage(1, sensor-1, i).sendToTarget();
                }
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
