package clmaldonado.multiplebt;

/**
 * Created by claudio on 30-01-16.
 */
public class Dispositivo {
    private String Name,MAC;
    private int joint;
    //private int joint;
    // joint default: 0, none selected
    // 1 : Hip
    // 2 : Knee
    // 3 : Ankle
    public Dispositivo(String name, String mac){

        this.Name = name;
        this.MAC = mac;
        this.joint = 0;
    }
    public Dispositivo(String name, int joint){
        this.Name = name;
        this.joint = joint;
        this.MAC = "useless";
    }

    public int getJoint() {
        return joint;
    }

    public String getName() {
        return Name;
    }

    public String getMAC() {
        return MAC;
    }

    public void setJoint(int joint) {
        this.joint = joint;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }
}
