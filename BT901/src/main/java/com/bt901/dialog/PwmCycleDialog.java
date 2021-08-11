package com.bt901.dialog;

import android.os.Bundle;

import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bt901.R;


/**
 * ${GWB}
 * 地址
 * 2017/5/9.
 */
public class PwmCycleDialog extends BDialog implements View.OnClickListener {

    private EditText pwm;

    private String value;

    private PwmCycleDialogCallBack pwmCycleDialogCallBack;

    public PwmCycleDialog() {
    }

    public static PwmCycleDialog newInstance() {
        PwmCycleDialog dialog = new PwmCycleDialog();
        return dialog;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_pwmcycle_dialog, container, false);
        pwm = view.findViewById(R.id.et_pwbCycle);
        Button sure = view.findViewById(R.id.bt_ok);
        Button abli = view.findViewById(R.id.bt_cancel);
        sure.setOnClickListener(this);
        abli.setOnClickListener(this);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public PwmCycleDialogCallBack getPwmCycleDialogCallBack() {
        return pwmCycleDialogCallBack;
    }

    public void setPwmCycleDialogCallBack(PwmCycleDialogCallBack pwmCycleDialogCallBack) {
        this.pwmCycleDialogCallBack = pwmCycleDialogCallBack;
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.bt_ok) {
            value = pwm.getText().toString();
            if (value == null || value.equals("")) {
                Toast.makeText(getContext(), R.string.data_null, Toast.LENGTH_SHORT).show();
                return;
            }
            if (pwmCycleDialogCallBack != null) {
                pwmCycleDialogCallBack.save(value);
            }
            dismiss();
        } else if (i == R.id.bt_cancel) {
            if (pwmCycleDialogCallBack != null) {
                pwmCycleDialogCallBack.back();
            }
            dismiss();
        }
    }


    public interface PwmCycleDialogCallBack {

        void save(String value);

        void back();

    }


}
