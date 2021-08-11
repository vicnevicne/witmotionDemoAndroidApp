package com.bt901.dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bt901.R;


/**
 * ${GWB}
 * 平滑系数
 * 2017/5/9.
 */
public class DevDialog extends BDialog implements View.OnClickListener {

    EditText startName;


    private String value;

    private DevDialogCallBack devDialogCallBack;

    public DevDialog() {
    }

    public static DevDialog newInstance() {
        DevDialog dialog = new DevDialog();
        return dialog;
    }

    public void showKeybard() {
        startName.setFocusable(true);
        startName.setFocusableInTouchMode(true);
        startName.requestFocus();
        InputMethodManager imm = (InputMethodManager) startName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(startName, 0);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_dev_dialog, container, false);
        startName = (EditText) view.findViewById(R.id.et_putStart);
        Button sure = (Button) view.findViewById(R.id.bt_save);
        Button abli = (Button) view.findViewById(R.id.bt_abolish);
        sure.setOnClickListener(this);
        abli.setOnClickListener(this);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    public DevDialogCallBack getDevDialogCallBack() {
        return devDialogCallBack;
    }

    public void setDevDialogCallBack(DevDialogCallBack devDialogCallBack) {
        this.devDialogCallBack = devDialogCallBack;
    }


    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.bt_save) {
            value = startName.getText().toString();
            if (value == null || value.equals("")) {
                Toast.makeText(getContext(), R.string.data_null, Toast.LENGTH_SHORT).show();
                return;
            }
            if (devDialogCallBack != null) {
                devDialogCallBack.save(value);
            }
            dismiss();
        } else if (i == R.id.bt_abolish) {
            if (devDialogCallBack != null) {
                devDialogCallBack.back();
            }
            dismiss();
        }
    }


    public interface DevDialogCallBack {

        void save(String value);

        void back();

    }


}
