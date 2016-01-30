package clmaldonado.multiplebt;

/**
 * Created by claudio on 30-01-16.
 */
public class Dispositivo {
    private String Name,MAC;

    public Dispositivo(String name, String mac){
        this.Name = name;
        this.MAC = mac;
    }

    public String getName() {
        return Name;
    }

    public String getMAC() {
        return MAC;
    }

    public void setName(String name) {
        Name = name;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }
}
