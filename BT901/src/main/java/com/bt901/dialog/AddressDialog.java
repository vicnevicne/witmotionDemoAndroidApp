package com.bt901.dialog;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
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
 * 地址
 * 2017/5/9.
 */
public class AddressDialog extends BDialog implements View.OnClickListener {

    EditText valueField;


    private String value;

    private AddressDialogCallBack addressDialogCallBack;

    public AddressDialog() {
    }

    public static AddressDialog newInstance() {
        AddressDialog dialog = new AddressDialog();
        return dialog;
    }

    public void showKeybard() {
        valueField.setFocusable(true);
        valueField.setFocusableInTouchMode(true);
        valueField.requestFocus();
        InputMethodManager imm = (InputMethodManager) valueField.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(valueField, 0);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_address_dialog, container, false);
        valueField = view.findViewById(R.id.value_field);
        Button okBtn = view.findViewById(R.id.bt_ok);
        Button cancelBtn = view.findViewById(R.id.bt_cancel);
        okBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    public AddressDialogCallBack getAddressDialogCallBack() {
        return addressDialogCallBack;
    }

    public void setAddressDialogCallBack(AddressDialogCallBack addressDialogCallBack) {
        this.addressDialogCallBack = addressDialogCallBack;
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.bt_ok) {
            value = valueField.getText().toString();
            if (value == null || value.equals("")) {
                Toast.makeText(getContext(), R.string.data_null, Toast.LENGTH_SHORT).show();
                return;
            }
            if (addressDialogCallBack != null) {
                addressDialogCallBack.save(value);
            }
            dismiss();
        } else if (i == R.id.bt_cancel) {
            if (addressDialogCallBack != null) {
                addressDialogCallBack.back();
            }
            dismiss();
        }
    }


    public interface AddressDialogCallBack {

        void save(String value);

        void back();

    }


}
