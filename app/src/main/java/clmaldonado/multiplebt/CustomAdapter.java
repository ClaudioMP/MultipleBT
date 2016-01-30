package clmaldonado.multiplebt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = (RelativeLayout)inflater.inflate(Resource,null);
            TextView Nombre = (TextView)convertView.findViewById(R.id.tvNombre);
            TextView Direcc = (TextView)convertView.findViewById(R.id.tvMac);
            Nombre.setText(dispositivos.get(position).getName());
            Direcc.setText(dispositivos.get(position).getMAC());
        }
        return convertView;
    }
}
