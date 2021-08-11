package com.bt901.dialog;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bt901.R;

public class AlarmDialog extends BDialog implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    EditText xMin, xMax, yMin, yMax, time;
    RadioButton open, close;
    private int tag = 1;

    private String strXMin, strXMax, strYMin, strYMax, strTime;

    private PoliceDialogCallBack policeDialogCallBack;

    public AlarmDialog() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_police_dialog, container, false);
        Button sure = view.findViewById(R.id.bt_ok);
        Button abli = view.findViewById(R.id.bt_cancel);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        open = view.findViewById(R.id.rb_open);
        close = view.findViewById(R.id.rb_close);

        xMin = view.findViewById(R.id.et_putXMin);
        xMax = view.findViewById(R.id.et_putXMax);
        yMin = view.findViewById(R.id.et_putYMin);
        yMax = view.findViewById(R.id.et_putYMax);
        time = view.findViewById(R.id.et_time);

        radioGroup.setOnCheckedChangeListener(this);
        sure.setOnClickListener(this);
        abli.setOnClickListener(this);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void setPoliceDialogCallBack(PoliceDialogCallBack policeDialogCallBack) {
        this.policeDialogCallBack = policeDialogCallBack;
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.bt_ok) {
            strXMin = xMin.getText().toString();
            strXMax = xMax.getText().toString();
            strYMin = yMin.getText().toString();
            strYMax = yMax.getText().toString();
            strTime = time.getText().toString();
            if (strXMin == null || strXMin.equals("")) {
                Toast.makeText(getContext(), R.string.xmin_null, Toast.LENGTH_SHORT).show();
                return;
            }
            if (strXMax == null || strXMax.equals("")) {
                Toast.makeText(getContext(), R.string.xmax_null, Toast.LENGTH_SHORT).show();
                return;
            }
            if (strYMin == null || strYMin.equals("")) {
                Toast.makeText(getContext(), R.string.ymin_null, Toast.LENGTH_SHORT).show();
                return;
            }
            if (strYMax == null || strYMax.equals("")) {
                Toast.makeText(getContext(), R.string.ymax_null, Toast.LENGTH_SHORT).show();
                return;
            }
            if (strTime == null || strTime.equals("")) {
                Toast.makeText(getContext(), R.string.xy_time, Toast.LENGTH_SHORT).show();
                return;
            }
            if (Integer.parseInt(strTime) <= 0) {
                Toast.makeText(getContext(), R.string.mytime, Toast.LENGTH_SHORT).show();
                return;
            }
            if (policeDialogCallBack != null) {
                policeDialogCallBack.save(strXMin, strXMax, strYMin, strYMax, strTime, tag);
            }
            dismiss();
        } else if (i == R.id.bt_cancel) {
            if (policeDialogCallBack != null) {
                policeDialogCallBack.back();
            }
            dismiss();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        if (open.getId() == i) {
            tag = 0;
            open.setBackgroundResource(R.drawable.icon_open_press);
            close.setBackgroundResource(R.drawable.ico_close_nor);
        } else if (close.getId() == i) {
            tag = 1;
            close.setBackgroundResource(R.drawable.icon_close_press);
            open.setBackgroundResource(R.drawable.icon_open_nor);
        }
    }


    public interface PoliceDialogCallBack {

        void save(String strXMin, String strXMax, String strYMin, String strYMax, String time, int tag);

        void back();

    }


}
