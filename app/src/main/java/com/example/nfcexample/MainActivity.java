package com.example.nfcexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;

    private EditText textToWrite;
    private MaterialButton textBtn;
    private PendingIntent pendingIntent;
    private IntentFilter[] readFilters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Read NFC adapter and UI elements
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        textToWrite = ((TextInputLayout) findViewById(R.id.main_et_writeToNfc)).getEditText();
        textBtn = findViewById(R.id.main_btn_writeButton);

        if (!isNFCReady()) finish();
        try {
            Intent intent = new Intent(this, getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            IntentFilter textFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED, "text/plain");
            readFilters = new IntentFilter[]{textFilter};
        }catch (Exception e) {
            e.printStackTrace();
        }

        processNfc(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableRead();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableRead();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast.makeText(this, tag.toString(), Toast.LENGTH_SHORT).show();
            processNfc(getIntent());
        }
        super.onNewIntent(intent);
    }

    /*
     * AUX FUNCTIONS
     */

    private boolean isNFCReady() {
        boolean res = true;
        if (nfcAdapter == null) {
            Toast.makeText(this, "Device does not support NFC", Toast.LENGTH_SHORT).show();
            res = false;
        }
        if (res && !nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC disabled", Toast.LENGTH_SHORT).show();
            res = false;
        }
        return res;
    }
    private void processNfc(@NonNull Intent intent) {
        String toastMsg = "";
        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (messages != null) {
            for (Parcelable message: messages) {
                NdefMessage ndefMessage = (NdefMessage) message;
                for (NdefRecord record: ndefMessage.getRecords()) {
                    if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
                        toastMsg = "WELL KNOWN: ";
                        if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                            toastMsg += "TEXT: " + new String(record.getPayload()) + "\n";
                            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    }

    private void enableRead() {
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, readFilters, null);
    }
    private void disableRead() {
        nfcAdapter.disableForegroundDispatch(this);
    }
}