package com.gogo.sampleconnector.connector.tools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gogo.sampleconnector.R;
import com.gogo.sampleconnector.connector.scantools.FlashingItem;
import com.gogo.sampleconnector.connector.scantools.ScanningRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Basic connector for device that can be scanned.
 */
public abstract class ScannableConnector extends BaseConnector {

    protected String dialogTitle;

    protected ScanningRunnable scanningRunnable;
    protected Button btPicker;

    private LinearLayout dialogLayout;
    private TextView tvScanning;

    /* Address list found when scanning */
    protected ListView addrListView;
    protected ArrayAdapter<String> addressAdapter;

    /* Single thread pool to execute scanning and connecting runnable */
    ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Implement this method to set up address editor.
     *
     * @return the LinearLayout container
     */
    protected abstract LinearLayout setupAddressEditor();

    /**
     * Implement a scanning runnable
     *
     * @return the ScanningRunnable
     */
    protected abstract ScanningRunnable setupScanningRunnable();

    /**
     * Implement a OnItemClickListener for address list.
     *
     * @return the OnItemClickListener
     */
    protected abstract AdapterView.OnItemClickListener setupAddressClickedListener(final ArrayList<String> list);

    /**
     * Get controller of the connection.
     *
     * @return the controller
     * @throws IOException
     */
    public abstract BaseController getController() throws IOException;

    // Handler that handle the updating of address list.
    Handler flashingHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case FlashingItem.FLASHING_MESSAGE:
                    tvScanning.setText((String) message.obj);
                    break;
                case FlashingItem.STOP_FLASHING_MESSAGE:
                    tvScanning.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    };

    @Override
    protected ListView prepareListView(ArrayList<String> list) {
        addrListView = super.prepareListView(list);
        addressAdapter = new ArrayAdapter<String>(getActivity(), R.layout.address, list);
        addrListView.setAdapter(addressAdapter);

        addrListView.setOnItemClickListener(setupAddressClickedListener(list));
        return addrListView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ListView lv = prepareListView(new ArrayList<String>());

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogLayout = (LinearLayout) inflater.inflate(R.layout.address_picker, null);

        /* Prepare connection button for editor */
        btPicker = (Button) dialogLayout.findViewById(R.id.btn_address_set);
        LinearLayout llEditor = setupAddressEditor();

        /* Get address editor container and append editor views under container */
        LinearLayout lleditorContainer = (LinearLayout) dialogLayout.findViewById(R.id.ll_editor_container);
        lleditorContainer.addView(llEditor);

        /* Add auto-updated address list under address list container */
        LinearLayout containerLayout = (LinearLayout) dialogLayout.findViewById(R.id.ll_address_list_container);
        containerLayout.addView(lv);

        tvScanning = (TextView) dialogLayout.findViewById(R.id.tv_scanning);
        scanningRunnable = setupScanningRunnable();
        executor.submit(scanningRunnable);

        return new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle)
                .setView(dialogLayout)
                .create();
    }

}
