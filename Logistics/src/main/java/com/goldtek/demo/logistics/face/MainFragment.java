package com.goldtek.demo.logistics.face;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.goldtek.demo.logistics.face.dialog.ProfileDialogFragment;
import com.goldtek.demo.logistics.face.dialog.ServerDialogFragment;

/**
 * Created by Terry on 2017/12/18 0018.
 */

public class MainFragment extends Fragment implements View.OnClickListener {
    private String mArgument ;
    public static final String ARGUMENT ="argument";
    public static final String RESPONSE = "response";
    public static final String PROFILE_CREATE = "profile_create_dialog";
    public static final String SETTING_SERVER = "config_server_dialog";
    public static final int REQUEST_PROFILE_CREATE = 0X110;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null)
            mArgument = bundle.getString(ARGUMENT);
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
                    //startActivity(new Intent(getContext(), FaceTrackerActivity.class));
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.Register:
                showDialog(PROFILE_CREATE);
                break;
            case R.id.Identify:
                break;
            case R.id.SettingServer:
                showDialog(SETTING_SERVER);
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
        }
        if (newFragment != null) newFragment.show(getFragmentManager(), tag);
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
