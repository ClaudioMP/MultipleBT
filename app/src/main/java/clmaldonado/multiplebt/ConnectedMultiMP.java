package clmaldonado.multiplebt;


import android.bluetooth.BluetoothSocket;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.*;
import android.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;


/**
 * A simple {@link Fragment} subclass.
 */

public class ConnectedMultiMP extends Fragment implements View.OnClickListener{
    ArrayList<BluetoothSocket> Sockets;
    int cantSockets = 0;
    Recepcion[] threads;
    String[] Sensors;
    AppCompatButton iniciar, parar, calibrar;
    LineChart[] charts;
    ArrayList<ArrayList<LineDataSet>> sets = new ArrayList<>();
    LineData[] datos;
    ArrayList<Dispositivo> devices;
    String[] Joints;
    int maxX;
    // Para las marcas de tiempo
    long tstart;
    // Layout con las gráficas
    LinearLayout graficos;
    // Para la escritura en archivo
    FileOutputStream fout;
    String FileName = "Data.csv";
    boolean firstLine = true;
    // Para la calibración
    float[] basePitch, baseRoll, baseYaw;
    int[] basei;
    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case 1:
                    charts[msg.arg1].notifyDataSetChanged();
                    charts[msg.arg1].setVisibleXRangeMaximum(maxX);
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
    public double Inches(){
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width=dm.widthPixels;
        int height=dm.heightPixels;
        int dens=dm.densityDpi;
        double wi=(double)width/(double)dens;
        double hi=(double)height/(double)dens;
        double x = Math.pow(wi,2);
        double y = Math.pow(hi,2);
        return Math.sqrt(x+y);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connected_multi_m, container, false);
        // Botones
        iniciar = (AppCompatButton)view.findViewById(R.id.Receive);
        parar = (AppCompatButton)view.findViewById(R.id.Stop);
        calibrar = (AppCompatButton)view.findViewById(R.id.Calibrar);
        iniciar.setOnClickListener(this);
        parar.setOnClickListener(this);
        calibrar.setOnClickListener(this);
        parar.setClickable(false);
        calibrar.setClickable(false);
        iniciar.setSupportBackgroundTintList(new ColorStateList(new int[][]{new int[0]}, new int[]{Color.parseColor("#0033cc")}));
        parar.setSupportBackgroundTintList(new ColorStateList(new int[][]{new int[0]},new int[]{Color.parseColor("#0033cc")}));
        calibrar.setSupportBackgroundTintList(new ColorStateList(new int[][]{new int[0]},new int[]{Color.parseColor("#0033cc")}));
        Joints = getResources().getStringArray(R.array.Joints);
        int minHeight = Inches()>6?300:250;
        // Gráficas
        graficos = (LinearLayout)view.findViewById(R.id.graficas);
        for(int i=0;i<cantSockets;i++){
            // Create the cardView to display the graphic
            CardView card = new CardView(getActivity().getBaseContext());
            RelativeLayout rl = new RelativeLayout(getActivity().getBaseContext());
            TextView tiempo = new TextView(getActivity().getBaseContext());
            charts[i] = new LineChart(getActivity().getBaseContext());
            charts[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, minHeight));
            charts[i].setHardwareAccelerationEnabled(true);
            charts[i].setId(View.generateViewId());
            card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.setCardElevation(5f);
            card.setRadius(0f);
            card.setCardBackgroundColor(Color.parseColor("#eaeaea"));
            card.setUseCompatPadding(true);
            card.setPreventCornerOverlap(true);
            RelativeLayout.LayoutParams timeParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            timeParams.addRule(RelativeLayout.BELOW,charts[i].getId());
            timeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            tiempo.setTextColor(Color.BLACK);
            tiempo.setTextSize(12f);
            tiempo.setText(getString(R.string.tiempo));
            rl.addView(charts[i]);
            rl.addView(tiempo,timeParams);
            card.addView(rl);
            graficos.addView(card);
        }
        ConfiguraGraficos();
        RevisarEstado();
        maxX = Inches()>6?200:100;
        return view;
    }

    public void ConfiguraGraficos(){
        MyMarker marker = new MyMarker(getActivity().getBaseContext(),R.layout.mymarker);
        for(int i=0; i<cantSockets;i++) {
            sets.add(new ArrayList<LineDataSet>());
        }
        int i = 0;
        for(ArrayList<LineDataSet> s:sets){
            s.add(new LineDataSet(new ArrayList<Entry>(), "Pitch"));
            s.add(new LineDataSet(new ArrayList<Entry>(), "Roll"));
            s.add(new LineDataSet(new ArrayList<Entry>(), "Yaw"));
            for(LineDataSet d: s){
                d.setDrawCircles(false);
                d.setDrawValues(false);
                d.setHighlightEnabled(true);
                d.setLineWidth(2.5f);
                d.setHighLightColor(Color.parseColor("#0033cc"));
                d.setHighlightLineWidth(.8f);
            }
            s.get(0).setColor(Color.BLACK);
            s.get(1).setColor(Color.RED);
            s.get(2).setColor(Color.GREEN);
            datos[i++] = new LineData(new ArrayList<String>(),s);
        }
        int j=0;
        for(LineChart c: charts){
            c.setNoDataText(getActivity().getString(R.string.noData));
            c.setDoubleTapToZoomEnabled(false);
            c.setPinchZoom(false);
            c.setHighlightPerDragEnabled(true);
            c.setHighlightPerTapEnabled(true);
            c.setScaleYEnabled(true);
            c.setDescription("");
            c.getLegend().setPosition(Legend.LegendPosition.ABOVE_CHART_RIGHT);
            c.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            c.setDrawBorders(true);
            c.getAxisRight().setDrawAxisLine(false);
            c.setDescription(Joints[devices.get(j).getJoint() - 1]);
            System.out.println(Joints[devices.get(j++).getJoint()-1]);
            c.getAxisRight().setEnabled(false);
            c.setMarkerView(marker);
        }
    }

    public void getSockets(ArrayList<BluetoothSocket> socks,String nombre,ArrayList<Dispositivo> dispositivos) {
        Sockets = socks;
        for (BluetoothSocket s : Sockets) {
            cantSockets += s.isConnected() ? 1 : 0;
        }
        System.out.println("Recibí los " + cantSockets + " sockets");
        threads = new Recepcion[cantSockets];
        charts = new LineChart[cantSockets];
        datos = new LineData[cantSockets];
        basePitch = new float[cantSockets];
        baseRoll = new float[cantSockets];
        baseYaw = new float[cantSockets];
        basei = new int[cantSockets];
        Sensors = new String[cantSockets];
        Sensors = getNames(Sockets);
        Calendar c = Calendar.getInstance();
        FileName = nombre+"_"+c.get(Calendar.DATE)+(c.get(Calendar.MONTH)+1)+c.get(Calendar.YEAR)+"_"+c.get(Calendar.HOUR_OF_DAY)+c.get(Calendar.MINUTE)+".csv";
        System.out.println("Todo creado, listo para iniciar");
        System.out.println(FileName);
        devices = dispositivos;
    }

    private String[] getNames(ArrayList<BluetoothSocket> s){
        String[] names = new String[s.size()];
        for(int i=0;i<s.size();i++){
            names[i] = s.get(i).getRemoteDevice().getName().toString();
        }
        return names;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.Receive:
                int j = 0;
                tstart = System.currentTimeMillis();
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
                calibrar.setVisibility(View.VISIBLE);
                parar.setVisibility(View.VISIBLE);
                break;
            case R.id.Stop:
                for(int i=0;i<cantSockets;i++){
                    System.out.println("Cerrando Thread " + i);
                    threads[i].cancel();
                    System.out.println("Thread " + i + " cerrado");
                    new Archivo().execute(i,-1,-1,(int)(basePitch[i]*100),(int)(baseRoll[i]*100),(int)(baseYaw[i]*100));
                }
                parar.setVisibility(View.GONE);
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
                    basei[i] = datos[i].getXValCount()-2;
                }
                calibrar.setClickable(false);
                calibrar.setVisibility(View.GONE);
                System.out.println("Calibrado");
                break;
        }
    }

    private void Angulos(int sensor, int index,long time, byte[] rawData){
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
        //index -= basei[sensor];
        angulos[0] = Math.toDegrees(-Math.asin(2*(x*z - w*y))) - basePitch[sensor]; // pitch
        angulos[1] = Math.toDegrees(Math.atan2(2 * (w * x + y * z), w * w - x * x - y * y + z * z)) - baseRoll[sensor]; // roll
        angulos[2] = Math.toDegrees(Math.atan2(2 * (x * y + w * z), 1 - 2 * (y * y + z * z))) - baseYaw[sensor];// yaw
        datos[sensor].addXValue(time/1000f + "");
        for(int i=0;i<3;i++){
            datos[sensor].addEntry(new Entry((((int) ((angulos[i] + 0.005) * 100)) / 100f), index), i);
        }
        new Archivo().execute(sensor,index,(int)time,d[0],d[1],d[2],d[3]);
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
            Toast.makeText(getActivity().getBaseContext(),R.string.noSD,Toast.LENGTH_LONG).show();
        }

    }

    private class Recepcion extends Thread{
        private BluetoothSocket mmSocket;
        private Handler mHandler;
        private InputStream inputStream;
        private String name;
        private int sensor;
        private int joint;

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
            mHandler.obtainMessage(3,getString(R.string.recvData)+" "+name).sendToTarget();
            try {
                inputStream.read(new byte[inputStream.available()],0,inputStream.available());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            int i=0,bytes;
            byte[] buffer = new byte[5];
            // Esto es una especie de GC para evitar graficas de golpe
            try {
                int av = inputStream.available();
                if(av>0) {
                    inputStream.read(new byte[av], 0,av);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            while(true){
                i++;
                long tnow;
                Arrays.fill(buffer,(byte)0);
                bytes = 0;
                tnow = System.currentTimeMillis() - tstart;
                try {
                    while(bytes < 5){
                        //if(inputStream.available()>0) {
                            bytes += inputStream.read(buffer,bytes,1);
                            if (buffer[0] != -1) {
                                buffer[0] = 0;
                                bytes = 0;
                            }
                        //}
                    }
                } catch (IOException e) {
                    System.out.println(e.toString());
                    mHandler.obtainMessage(2, getString(R.string.connection)+" "+name+" "+getString(R.string.closed)).sendToTarget();
                    break;
                }
                Angulos(sensor,i,tnow, buffer);
                if(i%10==0) {
                    mHandler.obtainMessage(1, sensor, i).sendToTarget();
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
            // sensor,index, time, w,x,y,z
            String out = "";
            if(!firstLine){
                out = "\n";
            }
            else{
                firstLine = false;
            }
            if(p[2]!=-1){
                out += Sensors[p[0]]+","+p[0]+","+devices.get(p[0]).getJoint()+p[1]+","+p[2]/1000f+","+p[3]/100.00f+","+p[4]/100.00f+ "," + p[5] / 100.00f+","+p[6]/100.00f;
            }
            else{
                //
                out+=Sensors[p[0]]+","+p[0]+","+devices.get(p[0]).getJoint()+","+p[2]+","+p[3]/100.00f+","+p[4]/100.00f+","+p[5]/100.00f;
            }
            try {
                fout.write(out.getBytes());
                //System.out.println(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
