package com.goldtek.demo.logistics.face;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.goldtek.demo.logistics.face.dialog.FancyAlert;
import com.goldtek.demo.logistics.face.dialog.ProfileDialogFragment;
import com.goldtek.demo.logistics.face.dialog.ServerDialogFragment;
import com.goldtek.demo.logistics.face.dialog.UsageWarning;
import com.marcoscg.easylicensesdialog.EasyLicensesDialogCompat;

/**
 * Created by Terry on 2017/12/18 0018.
 */

public class MainFragment extends Fragment implements View.OnClickListener, DialogInterface.OnClickListener {
    private String mArgument ;
    public static final String ARGUMENT ="argument";
    public static final String RESPONSE = "response";
    public static final String PROFILE_CREATE = "profile_create_dialog";
    public static final String SETTING_SERVER = "config_server_dialog";
    public static final String USAGE_WARNING = "usage_warning_dialog";
    public static final int REQUEST_PROFILE_CREATE = 0X110;
    public static final int REQUEST_USAGE_WARNING = 0X111;
    public static final int REQUEST_REGISTER = 0X112;
    public static final int REQUEST_IDENTIFY = 0X113;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null)
            mArgument = bundle.getString(ARGUMENT);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Auto-generated method stub
        super.onCreateOptionsMenu(menu, inflater);
//        menu.add("Menu 1a").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//        menu.add("Menu 1b").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        Toast.makeText(getActivity(), "menu text is "+item.getTitle(), Toast.LENGTH_LONG).show();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        view.findViewById(R.id.Register).setOnClickListener(this);
        view.findViewById(R.id.Identify).setOnClickListener(this);
        view.findViewById(R.id.SettingServer).setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQUEST_PROFILE_CREATE:
                if (resultCode == Activity.RESULT_OK) {
                    String name = data.getStringExtra(ProfileDialogFragment.RESPONSE_NAME);
                    int level = data.getIntExtra(ProfileDialogFragment.RESPONSE_LEVEL, 0);
                    //TODO: error check
                    Intent register = new Intent(getContext(), RegisterActivity.class);
                    register.putExtra(RegisterActivity.KEY_NAME, name);
                    register.putExtra(RegisterActivity.KEY_LEVEL, level);
                    startActivityForResult(register, REQUEST_REGISTER);
                }
                break;
            case REQUEST_REGISTER:
            case REQUEST_IDENTIFY:
                String title = "";
                if (resultCode == Activity.RESULT_OK) {
                    if (requestCode == REQUEST_REGISTER) title = "Register";
                    else title = String.format("Validate %s", data.getStringExtra(IdentifyActivity.KEY_NAME));

                    new FancyAlert.Builder()
                        .setIconResource(R.drawable.tick_green)
                        .setTitle(title)
                        .setMessage(getString(R.string.dialog_pass))
                        .setClickMessage(getString(R.string.btn_ok))
                        .setOnClickListener((requestCode == REQUEST_IDENTIFY) ? this : null)
                        .show(getActivity());
                } else {
                    if (requestCode == REQUEST_REGISTER) title = "Register";
                    else title = "Validate";

                    new FancyAlert.Builder()
                        .setIconResource(R.drawable.fail_red)
                        .setTitle(title)
                        .setMessage(getString(R.string.dialog_fail))
                        .setClickMessage(getString(R.string.btn_ok))
                        .show(getActivity());
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.Register:
                //showDialog(PROFILE_CREATE);
                new UsageWarning(getActivity(), REQUEST_PROFILE_CREATE)
                        .setTitle("Usage Notices")
                        .setPositiveButton(android.R.string.ok, this)
                        .show();
                break;
            case R.id.Identify:
                //startActivityForResult(new Intent(getContext(), IdentifyActivity.class), REQUEST_IDENTIFY);
                new UsageWarning(getActivity(), REQUEST_IDENTIFY)
                        .setTitle("Usage Notices")
                        .setPositiveButton(android.R.string.ok, this)
                        .show();
                break;
            case R.id.SettingServer:
                showDialog(SETTING_SERVER);
                break;
            case R.id.click_btn:
                CBroadcast m_objOpen = new CBroadcast(getContext());
                if(m_objOpen != null){
                    m_objOpen.iocontrollerOpen("Z01", m_objOpen.genMsgClientId());
                }
                break;
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case REQUEST_PROFILE_CREATE:
                showDialog(PROFILE_CREATE);
                break;
            case REQUEST_IDENTIFY:
                startActivityForResult(new Intent(getContext(), IdentifyActivity.class), REQUEST_IDENTIFY);
                break;
        }
    }

    private void showDialog(String tag) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(tag);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = null;
        switch (tag) {
            case PROFILE_CREATE:
                newFragment = new ProfileDialogFragment();
                newFragment.setTargetFragment(this, REQUEST_PROFILE_CREATE);
                break;
            case SETTING_SERVER:
                newFragment = new ServerDialogFragment();
                break;
            case USAGE_WARNING:
                break;
        }

        if (newFragment != null) {
            newFragment.show(getFragmentManager(), tag);
        }
    }

    public static MainFragment newInstance(String argument)
    {
        Bundle bundle = new Bundle();
        bundle.putString(ARGUMENT, argument);
        MainFragment contentFragment = new MainFragment();
        contentFragment.setArguments(bundle);
        return contentFragment;
    }

}
