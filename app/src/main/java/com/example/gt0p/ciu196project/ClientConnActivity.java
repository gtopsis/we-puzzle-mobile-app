package com.example.gt0p.ciu196project;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.zxing.Result;

import java.io.IOException;
import java.util.UUID;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ClientConnActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;
    private static final String TAG = ClientConnActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_client_conn);

        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScannerView != null)
            mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        // Do something with the result here

        //Get a BluetoothDevice from the MAC-address and initiate a connection with the server
        final String mac_address = rawResult.getText();

        // Start the Bluetooth service to manage the bluetooth connection
        Intent intent = new Intent(getBaseContext(), BluetoothService.class);
        intent.putExtra("macaddress", mac_address);
        startService(intent);

        Intent i = new Intent(ClientConnActivity.this, LoadingGameActivity.class);
        intent.putExtra("macaddress", mac_address);
        startActivity(i);
    }
}
