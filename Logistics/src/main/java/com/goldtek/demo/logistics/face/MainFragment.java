package com.goldtek.demo.logistics.face;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.goldtek.demo.logistics.face.dialog.AboutDialogFragment;
import com.goldtek.demo.logistics.face.dialog.FancyAlert;
import com.goldtek.demo.logistics.face.dialog.GTSharedPreferences;
import com.goldtek.demo.logistics.face.dialog.ProfileDialogFragment;
import com.goldtek.demo.logistics.face.dialog.ServerDialogFragment;
import com.goldtek.demo.logistics.face.dialog.UsageWarning;

import java.io.File;
import java.io.IOException;

/**
 * Created by Terry on 2017/12/18 0018.
 */

public class MainFragment extends Fragment implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener ,View.OnClickListener, DialogInterface.OnClickListener {
    private static final String TAG = "MainFragment";
    private static final String RAW_NAME = "demo3";
    private static final String FILE_NAME = "demo3.mp4";
    private static final String KEY_STATE ="main_fragment_state";
    private static final String PROFILE_CREATE = "profile_create_dialog";
    private static final String SETTING_SERVER = "config_server_dialog";
    private static final String ABOUT = "about_dialog";
    private static final String USAGE_WARNING = "usage_warning_dialog";
    public static final int REQUEST_PROFILE_CREATE = 0X110;
    public static final int REQUEST_REGISTER = 0X112;
    public static final int REQUEST_IDENTIFY = 0X113;
    public static final int REQUEST_SETTING = 0X114;
    public static final int REQUEST_OTHER = 0X199;

    public static final String ARGUMENT ="argument";
    public static final String RESPONSE = "response";

    private String mArgument ;
    private Uri mVideo_uri;
    private boolean m_bPlayMedia = true;
    private boolean m_bVideofile = false;
    private boolean m_bTargetDevice = false;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private MediaPlayer mMediaPlayer;
    private int mMediaCurrentPos;
    private GTSharedPreferences mPreferences;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_bTargetDevice = Utils.isTargetDevice();
        Bundle bundle = getArguments();
        if (bundle != null)
            mArgument = bundle.getString(ARGUMENT);

        setHasOptionsMenu(true);
        mPreferences = new GTSharedPreferences(getContext());
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
    public void onResume() {
        super.onResume();
        if (getActivity().getIntent().getIntExtra(KEY_STATE, -1) < 0) {
            fadeButton(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view;
        if (m_bTargetDevice) view = inflater.inflate(R.layout.fragment_main_on_target, container, false);
        else view = inflater.inflate(R.layout.fragment_main_on_mobile, container, false);

        if (m_bTargetDevice)
            view.findViewById(R.id.Identify).setOnClickListener(this);
        else
            view.findViewById(R.id.Register).setOnClickListener(this);

        view.findViewById(R.id.SettingServer).setOnClickListener(this);
        view.findViewById(R.id.imgAbout).setOnClickListener(this);
        view.findViewById(R.id.imgLogo).setOnClickListener(this);

        mSurfaceView = view.findViewById(R.id.surfaceViewFrame);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(MainFragment.this);

        m_bPlayMedia = mPreferences.getPlayMainMedia();
        playMedia(view);

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
                } else
                    fadeButton(true);
                break;
            case REQUEST_OTHER:
                fadeButton(true);
                break;
            case REQUEST_SETTING:
                m_bPlayMedia = mPreferences.getPlayMainMedia();

                if (m_bPlayMedia) resumeMediaPlayer();
                else pauseMediaPlayer();

                fadeButton(true);
                break;
            case REQUEST_REGISTER:
            case REQUEST_IDENTIFY:
                Log.i(TAG, "result: " + resultCode);
                String title;
                if (resultCode == Activity.RESULT_OK) {
                    if (requestCode == REQUEST_REGISTER) title = "Register";
                    else title = String.format("Welcome %s", data.getStringExtra(IdentifyActivity.KEY_NAME));

                    new FancyAlert.Builder()
                        .setIconResource(R.drawable.tick_green)
                        .setTitle(title)
                        .setMessage(getString(R.string.dialog_pass))
                        //.setClickMessage(getString(R.string.btn_ok))
                        //.setOnClickListener(this)
                        .show(getActivity(), (requestCode == REQUEST_IDENTIFY) ? REQUEST_IDENTIFY : REQUEST_OTHER);
                } else {
                    if (requestCode == REQUEST_REGISTER) title = "Register";
                    else title = "Validate";

                    new FancyAlert.Builder()
                        .setIconResource(R.drawable.fail_red)
                        .setTitle(title)
                        .setMessage(getString(R.string.dialog_fail))
                        //.setClickMessage(getString(R.string.btn_ok))
                        //.setOnClickListener(this)
                        .show(getActivity(), REQUEST_OTHER);
                }

                fadeButton(true);
                break;
        }
        getActivity().getIntent().putExtra(KEY_STATE, REQUEST_PROFILE_CREATE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.Register:
                new UsageWarning(getActivity(), REQUEST_PROFILE_CREATE)
                        .setTitle("Usage Notices")
                        .setPositiveButton(android.R.string.ok, this)
                        .show();
                break;
            case R.id.Identify:
                new UsageWarning(getActivity(), REQUEST_IDENTIFY)
                        .setTitle("Usage Notices")
                        .setPositiveButton(android.R.string.ok, this)
                        .show();
                break;
            case R.id.SettingServer:
                showDialog(SETTING_SERVER);
                break;
            case R.id.click_btn:
                if (view.getTag() instanceof Integer) {
                    if ((int) view.getTag() == REQUEST_IDENTIFY) {
                        CBroadcast.iocontrollerOpen(getContext(), "Z08");
                    }
                    fadeButton(true);
                }

                break;
            case R.id.imgAbout:
                showDialog(ABOUT);
                break;
            case R.id.imgLogo:
                startActivityForResult(new Intent(getContext(), IdentifyActivity.class), REQUEST_IDENTIFY);
                //startActivity(new Intent(getContext(), IdentifyActivity.class));
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
                //startActivity(new Intent(getContext(), IdentifyActivity.class));
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(m_bVideofile)
            releaseMediaPlayer();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        resumeMediaPlayer();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mMediaPlayer.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private void playMedia(View view) {
        int checkExistence = getContext().getResources().getIdentifier(RAW_NAME, "raw", getActivity().getPackageName());

        File media = Utils.getExternalStoragePrivateFile(getContext(), FILE_NAME);

        if (checkExistence != 0) {
            m_bVideofile = true;
            mVideo_uri = Uri.parse("android.resource://" + getContext().getPackageName() + "/" + checkExistence);
        } else if (media != null) {
            m_bVideofile = true;
            mVideo_uri = Uri.fromFile(media);
        } else m_bVideofile = false;

        if (m_bPlayMedia && m_bVideofile) {
            view.findViewById(R.id.root).setBackgroundResource(R.drawable.gradient_skyblue2);
            view.findViewById(R.id.surfaceViewFrame).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.root).setBackgroundResource(R.drawable.gradient_skyblue1);
            view.findViewById(R.id.surfaceViewFrame).setVisibility(View.INVISIBLE);
        }
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void resumeMediaPlayer() {
        if (mMediaPlayer != null)
            mMediaPlayer.seekTo(mMediaCurrentPos);
        else {
            if (m_bPlayMedia && m_bVideofile) {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDisplay(mSurfaceHolder);
                try {
                    mMediaPlayer.setDataSource(getContext(), mVideo_uri);
                    mMediaPlayer.prepare();
                    mMediaPlayer.setOnPreparedListener(MainFragment.this);
                    mMediaPlayer.setLooping(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                setVideoSize();
            }
        }
    }

    private void pauseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            mMediaCurrentPos = mMediaPlayer.getCurrentPosition();
        }
    }

    private void setVideoSize() {

        // // Get the dimensions of the video
        int videoWidth = mMediaPlayer.getVideoWidth();
        int videoHeight = mMediaPlayer.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;

        // Get the width of the screen
        int screenWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getActivity().getWindowManager().getDefaultDisplay().getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;

        // Get the SurfaceView layout parameters
        android.view.ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        // Commit the layout parameters
        mSurfaceView.setLayoutParams(lp);
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
                newFragment.setTargetFragment(this, REQUEST_SETTING);
                break;
            case ABOUT:
                newFragment = new AboutDialogFragment();
                newFragment.setTargetFragment(this, REQUEST_OTHER);
                break;
        }

        if (newFragment != null) {
            fadeButton(false);
            newFragment.show(getFragmentManager(), tag);
        }
    }

    private void fadeButton(boolean visible) {
        Animation anim;
        if (m_bTargetDevice)
        {
            anim = AnimationUtils.loadAnimation(getContext(), visible ? R.anim.fade_in_btn : R.anim.fade_out_btn);
            anim.setAnimationListener(new GTAnimationListener(getActivity().findViewById(R.id.Identify), visible ? View.VISIBLE : View.INVISIBLE));
            getActivity().findViewById(R.id.Identify).startAnimation(anim);
        }
        else
        {
            anim = AnimationUtils.loadAnimation(getContext(), visible ? R.anim.fade_in_btn : R.anim.fade_out_btn);
            anim.setAnimationListener(new GTAnimationListener(getActivity().findViewById(R.id.Register), visible ? View.VISIBLE : View.INVISIBLE));
            getActivity().findViewById(R.id.Register).startAnimation(anim);
        }

        anim = AnimationUtils.loadAnimation(getContext(), visible ? R.anim.fade_in_btn : R.anim.fade_out_btn);
        anim.setAnimationListener(new GTAnimationListener(getActivity().findViewById(R.id.SettingServer), visible ? View.VISIBLE : View.INVISIBLE));
        getActivity().findViewById(R.id.SettingServer).startAnimation(anim);
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
