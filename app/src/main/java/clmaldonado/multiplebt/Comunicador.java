package clmaldonado.multiplebt;

import android.bluetooth.BluetoothSocket;

import java.util.ArrayList;

/**
 * Created by claudio on 12-12-15.
 */
public interface Comunicador {
    // Esta función se utiliza para pasar el conjunto de sockets
    // Conectados al fragment que los manejará
    public void PasaSockets(ArrayList<BluetoothSocket> sockets);
}
