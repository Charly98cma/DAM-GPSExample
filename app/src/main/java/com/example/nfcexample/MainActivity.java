package com.example.nfcexample;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.internal.ParcelableSparseBooleanArray;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFilters;
    private NdefMessage ndefMessage;
    private IntentFilter[] readFilters;
    private IntentFilter[] writeFilter;

    private Tag myTag;

    private EditText textToWrite;
    private MaterialButton textBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Read NFC adapter and UI elements
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        textToWrite = ((TextInputLayout) findViewById(R.id.main_et_writeToNfc)).getEditText();
        textBtn = findViewById(R.id.main_btn_writeButton);

        /*
         * NFC setup
         */

        if (!isNFCReady()) return;
        try {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            IntentFilter textFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED, "text/plain");
            readFilters = new IntentFilter[]{textFilter};
            writeFilter = new IntentFilter[]{}; // No filter => any dataType

º


        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }


        /*
         * LISTENERS
         */

        textBtn.setOnClickListener(v -> {
            // TODO Ready text to write on tag
            writeText(textToWrite.getText().toString().trim());
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null)
            nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        assert nfcAdapter != null;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast.makeText(this, tag.toString(), Toast.LENGTH_SHORT).show();
        }
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

    private void writeText(String rawText) {
        if (rawText.isEmpty()) {
            Toast.makeText(this, "TEXT IS EMPTY!", Toast.LENGTH_SHORT).show();
        }else {
            byte[] lang = Locale.getDefault().getLanguage().getBytes(StandardCharsets.UTF_8);
            byte[] text = rawText.getBytes(StandardCharsets.UTF_8);
            byte[] payload = new byte[lang.length + text.length + 1];

            payload[0] = 0x02; // UTF-8
            System.arraycopy(lang, 0, payload, 1, lang.length); // Write lang
            System.arraycopy(text, 0, payload, lang.length+1, text.length); // Write text

            NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
            ndefMessage = new NdefMessage(new NdefRecord[]{record});
            Toast.makeText(this, "Tap a tag to write the text", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableRead() { nfcAdapter.enableForegroundDispatch(this, pendingIntent, readFilters, null); }
    private void disableRead() { nfcAdapter.disableForegroundDispatch(this); }
}