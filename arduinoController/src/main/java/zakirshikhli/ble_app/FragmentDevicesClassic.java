package zakirshikhli.ble_app;


import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.Objects;

import zakirshikhli.ble_app.classic.CLutil;

/**
 * @noinspection deprecation
 */
public class FragmentDevicesClassic extends ListFragment {

    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;
    ActivityResultLauncher<String> requestBluetoothPermissionLauncherForRefresh;
    private Menu menu;
    private boolean permissionMissing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listAdapter = new ArrayAdapter<>(requireActivity(), 0, listItems) {
            @NonNull
            @Override
            public View getView(int position, View devicesFragment, @NonNull ViewGroup parent) {
                BluetoothDevice device = listItems.get(position);
                if (devicesFragment == null)
                    devicesFragment = requireActivity().getLayoutInflater().inflate(R.layout.device_list_item_classic, parent, false);
                TextView text1 = devicesFragment.findViewById(R.id.text1);
                TextView text2 = devicesFragment.findViewById(R.id.text2);
                @SuppressLint("MissingPermission") String deviceName = device.getName();
                text1.setText(deviceName);

                String adrs = device.getAddress();
                SpannableString partOne = new SpannableString(adrs.substring(0, adrs.length() - 5));
                partOne.setSpan(
                        new ForegroundColorSpan(getResources().getColor(R.color.textColor)),0,
                        partOne.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                text2.setText(partOne);

                SpannableString partTwo = new SpannableString(" " +
                        adrs.substring(adrs.length() - 5, adrs.length()));
                partTwo.setSpan(
                        new ForegroundColorSpan(getResources().getColor(R.color.color_classic)), 0,
                        partTwo.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                partTwo.setSpan(
                        new StyleSpan(android.graphics.Typeface.BOLD), 0,
                        partTwo.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                text2.append(partTwo);
                return devicesFragment;
            }
        };
        requestBluetoothPermissionLauncherForRefresh = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> CLutil.onPermissionsResult(this, granted, this::refresh));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        View header = requireActivity().getLayoutInflater().inflate(
                R.layout.device_list_header_classic,
                requireActivity().findViewById(R.id.devListTest),
                false);
        getListView().addHeaderView(header, null, false);
        setEmptyText("initializing...");
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        setListAdapter(listAdapter);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.menu_devices_classic, menu);
        if (permissionMissing)
            menu.findItem(R.id.bt_refresh).setVisible(true);
        if (bluetoothAdapter == null)
            menu.findItem(R.id.bt_settings).setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.bt_settings) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;
        } else if (id == R.id.bt_refresh) {
            if (CLutil.hasPermissions(this, requestBluetoothPermissionLauncherForRefresh))
                refresh();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    void refresh() {
        listItems.clear();
        if (bluetoothAdapter != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionMissing = requireActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
                if (menu != null && menu.findItem(R.id.bt_refresh) != null)
                    menu.findItem(R.id.bt_refresh).setVisible(permissionMissing);
            }

            if (!permissionMissing) {
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
                    if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                        listItems.add(device);
                listItems.sort(CLutil::compareTo);
            }

        }
        if (bluetoothAdapter == null)
            setEmptyText("- Bluetooth not supported -");
        else if (!bluetoothAdapter.isEnabled())
            setEmptyText(getResources().getString(R.string.bluetooth_is_disabled));
        else if (permissionMissing)
            setEmptyText( getResources().getString(R.string.permission_missing));
        else
            setEmptyText("<no bluetooth devices found>");
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        BluetoothDevice device = listItems.get(position - 1);
        Bundle args = new Bundle();
        args.putString("device", device.getAddress());
        Fragment fragment = new FragmentController();
        fragment.setArguments(args);
        getParentFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "controller").addToBackStack(null).commit();
    }
}
