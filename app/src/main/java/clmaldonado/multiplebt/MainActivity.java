package clmaldonado.multiplebt;

import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Comunicador {
    BluetoothAdapter btAdapter;
    int BT_REQUEST = 1;
    ConnectionFragment connectionFragment = null;
    ConnectedMultiMP connectedMultiMP = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        RevisaBluetooth(btAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.next_Icon:
                if(connectionFragment.sockets.size()!=0) {
                    connectionFragment.PasarALosGraficos();
                    item.setVisible(false);
                }
                else{
                    Toast.makeText(this,getString(R.string.no_sensors),Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void RevisaBluetooth(BluetoothAdapter bt) {
        if (bt == null) {
            Toast.makeText(getApplicationContext(), R.string.NoBT, Toast.LENGTH_SHORT).show();
        } else {
            if (!bt.isEnabled()) {
                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(i, BT_REQUEST);
            } else {
                PasarAlFragment();
            }
        }
    }

    private void PasarAlFragment() {
        connectionFragment = new ConnectionFragment();
        connectionFragment.GetAdapter(btAdapter);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.Layout, connectionFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BT_REQUEST) {
            if (resultCode == -1) {
                Toast.makeText(getApplicationContext(), R.string.BTTurnedOn, Toast.LENGTH_LONG).show();
                PasarAlFragment();
            }
            if (resultCode == 0) {
                Toast.makeText(getApplicationContext(), R.string.BTErrorTurnOn, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void PasaSockets(ArrayList<BluetoothSocket> sockets, String name) {
        connectedMultiMP = new ConnectedMultiMP();
        connectedMultiMP.getSockets(sockets, name);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.Layout, connectedMultiMP);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
