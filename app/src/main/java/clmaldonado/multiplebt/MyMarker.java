package clmaldonado.multiplebt;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.Utils;
import org.w3c.dom.Text;

/**
 * Created by claudio on 18-01-16.
 */
public class MyMarker extends MarkerView {
    private TextView X;

    public MyMarker(Context context, int layoutResource){
        super(context, layoutResource);
        X = (TextView)findViewById(R.id.tvMarkerX);

    }
    @Override
    public void refreshContent(Entry e, Highlight highlight)
    {
        String angulo = "Yaw";
        if(highlight.getDataSetIndex()==0){
            angulo = "Pitch";
        }
        else if(highlight.getDataSetIndex()==1){
            angulo = "Roll";
        }
        X.setText(angulo+" "+Utils.formatNumber(e.getVal(),2,true)+"°");
    }

    @Override
    public int getXOffset(float xpos) {
        return -(getWidth()/2);
    }

    @Override
    public int getYOffset(float ypos) {
        return -getHeight();
    }
}
