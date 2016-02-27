package clmaldonado.multiplebt;


import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.*;
import android.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectedNoCharts extends Fragment implements View.OnClickListener {

    ArrayList<BluetoothSocket> Sockets;
    int cantSockets = 0;
    Recepcion[] threads;
    String[] Sensors;
    AppCompatButton iniciar, parar;
    ArrayList<Dispositivo> devices;
    String[] Joints;
    // Para las marcas de tiempo
    long tstart;
    // Para la escritura en archivo
    FileOutputStream fout;
    String FileName = "Data.csv";
    boolean firstLine = true;
    // Para los InfoCards
    LinearLayout Infocards;
    TextView[] cantDatos;

    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case 1:
                    cantDatos[msg.arg1].setText(getString(R.string.Samples)+" "+msg.arg2);
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


    public ConnectedNoCharts() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connected_no_charts, container, false);
        // Botones
        iniciar = (AppCompatButton)view.findViewById(R.id.ReceiveNC);
        parar = (AppCompatButton)view.findViewById(R.id.StopNC);
        iniciar.setOnClickListener(this);
        parar.setOnClickListener(this);
        parar.setClickable(false);
        iniciar.setSupportBackgroundTintList(new ColorStateList(new int[][]{new int[0]}, new int[]{Color.parseColor("#0033cc")}));
        parar.setSupportBackgroundTintList(new ColorStateList(new int[][]{new int[0]},new int[]{Color.parseColor("#0033cc")}));
        Joints = getResources().getStringArray(R.array.Joints);
        RevisarEstado();
        // Here we create the Cards to show some information
        Infocards = (LinearLayout)view.findViewById(R.id.infoCards);
        Context ctx = getActivity().getBaseContext();
        for(int i=0;i<cantSockets;i++){
            // Creación de elementos del card
            CardView card = new CardView(ctx);
            LinearLayout LL = new LinearLayout(ctx);
            TextView Nombre = new TextView(ctx);
            TextView Segmento = new TextView(ctx);
            cantDatos[i] = new TextView(ctx);
            // Personalización del card
            card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.setCardElevation(5f);
            card.setRadius(0f);
            card.setCardBackgroundColor(Color.parseColor("#eaeaea"));
            card.setUseCompatPadding(true);
            card.setPreventCornerOverlap(true);
            // Asignación de ID
            Nombre.setId(View.generateViewId());
            Segmento.setId(View.generateViewId());
            cantDatos[i].setId(View.generateViewId());
            // Parámetros de los TextViews
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            // Asignación de textos
            Nombre.setText(getString(R.string.SensorName)+" "+devices.get(i).getName());
            Segmento.setText(getString(R.string.Segment)+" "+Joints[devices.get(i).getJoint()-1]);
            cantDatos[i].setText(getString(R.string.Samples) + " " + 0);
            // Adición de vistas
            LL.addView(Segmento,textParams);
            LL.addView(Nombre,textParams);
            LL.addView(cantDatos[i],textParams);
            card.addView(LL);
            Infocards.addView(card);
        }
        return view;
    }

    public void getSockets(ArrayList<BluetoothSocket> socks,String nombre,ArrayList<Dispositivo> dispositivos) {
        Sockets = socks;
        for (BluetoothSocket s : Sockets) {
            cantSockets += s.isConnected() ? 1 : 0;
        }
        //System.out.println("Recibí los " + cantSockets + " sockets");
        threads = new Recepcion[cantSockets];
        Sensors = new String[cantSockets];
        Sensors = getNames(Sockets);
        cantDatos = new TextView[cantSockets];
        Calendar c = Calendar.getInstance();
        FileName = nombre+"_"+c.get(Calendar.DATE)+(c.get(Calendar.MONTH)+1)+c.get(Calendar.YEAR)+"_"+c.get(Calendar.HOUR_OF_DAY)+c.get(Calendar.MINUTE)+".csv";
        //System.out.println("all creado, listo para iniciar");
        //System.out.println(FileName);
        devices = dispositivos;
    }

    private String[] getNames(ArrayList<BluetoothSocket> s){
        String[] names = new String[s.size()];
        for(int i=0;i<s.size();i++){
            names[i] = s.get(i).getRemoteDevice().getName();
        }
        return names;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.Receive:
                tstart = System.currentTimeMillis();
                for(int i = 0;i< cantSockets;i++){
                    threads[i] = new Recepcion(Sockets.get(i),handler,i);
                }
                for (int i=0;i<cantSockets;i++){
                    threads[i].start();
                }
                parar.setClickable(true);
                iniciar.setVisibility(View.GONE);
                parar.setVisibility(View.VISIBLE);
                break;
            case R.id.Stop:
                for(int i=0;i<cantSockets;i++){
                    //System.out.println("Cerrando Thread " + i);
                    threads[i].cancel();
                    int data = threads[i].i;
                    cantDatos[i].setText(getString(R.string.Samples)+" "+data);
                    //System.out.println("Thread " + i + " cerrado");
                    if(i==cantSockets-1){
                        new Archivo().execute(0,0,0,0,0,0,0,1);
                    }

                }
                parar.setVisibility(View.GONE);
                break;
        }
    }

    private void Angulos(int sensor, int index,long time, byte[] rawData){
        int[] d = new int[4];
        for(int i=0;i<4;i++){
            d[i] = rawData[i+1]<0?(int)rawData[i+1]+256:(int)rawData[i+1];
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
                //System.out.println("Se puede escribir");
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
        public int i=0;

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
            int bytes;
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
                Arrays.fill(buffer, (byte) 0);
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
                    //System.out.println(e.toString());
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
            // Sensor (nombre), sensor index, joint index, sample number, time, w,x,y,z
            String out = "";
            if(!firstLine){
                out = "\n";
            }
            else{
                firstLine = false;
            }
            if(p[2]!=-1){
                out += Sensors[p[0]]+","+p[0]+","+devices.get(p[0]).getJoint()+","+p[1]+","+p[2]/1000f+","+p[3]/100.00f+","+p[4]/100.00f+ "," + p[5] / 100.00f+","+p[6]/100.00f;
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
            if(p.length==8 && p[7]==1){
                try {
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

}
