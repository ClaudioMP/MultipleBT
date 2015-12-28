package clmaldonado.multiplebt;

import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Comunicador {
    BluetoothAdapter btAdapter;
    int BT_REQUEST = 1;
    ConnectionFragment connectionFragment;
    ConnectedMultiMP connectedMultiMP;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        RevisaBluetooth(btAdapter);
    }

    private void RevisaBluetooth(BluetoothAdapter bt){
        if(bt==null){
            Toast.makeText(getApplicationContext(),"No se encontró adaptador Bluetooth",Toast.LENGTH_SHORT).show();
        } else{
            if (!bt.isEnabled()){
                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(i, BT_REQUEST);
            }
            else{
                PasarAlFragment();
            }
        }
    }

    private void PasarAlFragment(){
        connectionFragment = new ConnectionFragment();
        connectionFragment.GetAdapter(btAdapter);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.Layout,connectionFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BT_REQUEST) {
            if (resultCode == -1) {
                Toast.makeText(getApplicationContext(), "Bluetooth encendido correctamente", Toast.LENGTH_LONG).show();
                PasarAlFragment();
            }
            if (resultCode == 0) {
                Toast.makeText(getApplicationContext(), "Error al encender Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void PasaSockets(ArrayList<BluetoothSocket> sockets) {
        connectedMultiMP = new ConnectedMultiMP();
        connectedMultiMP.getSockets(sockets);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.Layout,connectedMultiMP);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        if(getFragmentManager().getBackStackEntryCount()>1){
            getFragmentManager().popBackStack();
        }
        else{
            super.onBackPressed();
        }
    }
}
