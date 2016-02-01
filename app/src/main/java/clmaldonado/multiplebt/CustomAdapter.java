package clmaldonado.multiplebt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

/**
 * Created by claudio on 30-01-16.
 */

public class CustomAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<Dispositivo> dispositivos;
    private int Resource;

    public CustomAdapter(Context context, ArrayList<Dispositivo> dispositivos,int LayoutResource){
        this.context = context;
        this.dispositivos = dispositivos;
        this.Resource = LayoutResource;
    }

    @Override
    public int getCount() {
        return dispositivos.size();
    }

    @Override
    public Object getItem(int position) {
        return dispositivos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertview = inflater.inflate(Resource,null);
        TextView Nombre = (TextView)convertview.findViewById(R.id.tvNombre);
        TextView Direcc = (TextView)convertview.findViewById(R.id.tvMac);
        Nombre.setText(dispositivos.get(position).getName());
        Direcc.setText(dispositivos.get(position).getMAC());
        return convertview;
    }
}
