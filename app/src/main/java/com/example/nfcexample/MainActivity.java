package com.example.nfcexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Declaration of NFC elements
    private NfcAdapter nfcAdapter;
    private NdefMessage ndefMessage;
    private PendingIntent pendingIntent;
    private IntentFilter[] readFilters;
    private IntentFilter[] writeFilter;
    private String[][] techList;

    // Declaration of UI elements
    private EditText textToWrite;
    private TextView nfcText;

    @SuppressLint("UnspecifiedImmutableFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Read NFC adapter and UI elements
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        textToWrite = ((TextInputLayout) findViewById(R.id.main_et_writeToNfc)).getEditText();
        nfcText = findViewById(R.id.nfcText);

        // Listener for the write button
        ((MaterialButton) findViewById(R.id.main_btn_writeButton))
                .setOnClickListener(v -> writeText(textToWrite.getText().toString().trim()));

        // Check NFC is supporter and enabled
        if (!isNFCReady())
            finish();

        try {
            Intent intent = new Intent(this, getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            // Store for pending intents of the activity
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Definition of filters (based on dataType)
            IntentFilter textFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED, "text/plain");

            // Read and Write filters
            readFilters = new IntentFilter[]{textFilter};
            writeFilter = new IntentFilter[]{};

            // Definition of the supported tech
            techList = new String[][]{
                    {Ndef.class.getName()},
                    {NdefFormatable.class.getName()}
            };
        }catch (Exception e) {
            e.printStackTrace();
        }

        // Process any intents (in case the app is chosen when is not active)
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
        // Cover all supported possible actions
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            processNfc(intent);
        }
        super.onNewIntent(intent);
    }

    /*
     * AUX FUNCTIONS
     */

    private boolean isNFCReady() {
        boolean res = true;
        // Checl NFC is supporter
        if (nfcAdapter == null) {
            Toast.makeText(this, "Device does not support NFC", Toast.LENGTH_SHORT).show();
            res = false;
        }
        // Check NFC is enabled
        if (res && !nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC disabled", Toast.LENGTH_SHORT).show();
            res = false;
        }
        return res;
    }

    private void processNfc(Intent intent) {
        // Choose operation based on the existence of a pending message
        if (ndefMessage != null)
            writeTag(intent);
        else
            readTag(intent);
    }

    private void writeTag(Intent intent) {
        // Read tag
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            try {
                // Read tag
                Ndef ndef = Ndef.get(tag);
                if (ndef == null) {
                    // Open to write and format
                    NdefFormatable ndefFormatable = NdefFormatable.get(tag);
                    if (ndefFormatable != null) {
                        ndefFormatable.connect();
                        ndefFormatable.format(ndefMessage);
                        ndefFormatable.close();
                        Toast.makeText(this, "TAG FORMATTED AND WRITTEN", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    // Open to write only (already formatted)
                    ndef.connect();
                    ndef.writeNdefMessage(ndefMessage);
                    ndef.close();
                    Toast.makeText(this, "TAG WRITTEN", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException | FormatException e) {
                e.printStackTrace();
            } finally {
                // Clear the store message (already written or error occurred)
                ndefMessage = null;
            }
        }
    }

    private void readTag(@NonNull Intent intent) {
        String msg;
        // Read messages on the NFC tag
        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (messages != null) {
            // Process each message
            for (Parcelable message: messages) {
                NdefMessage ndefMessage = (NdefMessage) message;
                for (NdefRecord record: ndefMessage.getRecords()) {
                    // Display message on each format (multiple formats require a switch statement)
                    // switch(record.getTnf()) {...}
                    if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
                        msg = "WELL KNOWN: ";
                        // Display message based on dataType
                        if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                            msg += "TEXT: " + new String(record.getPayload()) + "\n";
                            nfcText.setText(msg);
                        }
                    }
                }
            }
        }
    }

    private void writeText(String rawText) {
        if (rawText.isEmpty()) {
            Toast.makeText(this, "TEXT IS EMPTY!", Toast.LENGTH_SHORT).show();
        }else {
            // Read language and text as byte arrays
            byte[] lang = Locale.getDefault().getLanguage().getBytes(StandardCharsets.UTF_8);
            byte[] text = rawText.getBytes(StandardCharsets.UTF_8);
            // Allocate space for lang and text (and encoding)
            byte[] payload = new byte[lang.length + text.length + 1];

            payload[0] = 0x02; // UTF-8 encoding
            System.arraycopy(lang, 0, payload, 1, lang.length); // Write lang
            System.arraycopy(text, 0, payload, lang.length+1, text.length); // Write text

            // Create record with the payload (message to write on NFC tag)
            NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
            ndefMessage = new NdefMessage(new NdefRecord[]{record});

            // Enable write when NFC is tapped
            Toast.makeText(this, "Tap a tag to write the text", Toast.LENGTH_SHORT).show();
            enableWrite();

            // Clear input box
            textToWrite.setText("");
        }
    }

    private void enableRead() {
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, readFilters, null);
    }
    private void enableWrite() {
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeFilter, techList);
    }
    private void disableRead() {
        nfcAdapter.disableForegroundDispatch(this);
    }
}