package androidclient;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.goldtek.demo.logistics.face.R;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Random;

public class MainActivity extends Activity {


	String TAG = "MainActivity";

	private TextView tvServerMessage;
	private Context m_contx;
	private Button btn1, btn2, btn3, btn4;
	private CheckBox chk1;

	Random rand = new Random();

	int nSendSize = 1;

	private final MyHandler mHandler = new MyHandler(this);

	private String ServerIP 	= "192.168.43.32";
	private String RegisterName = "Fred";
	private String RegisterID 	= "Fred";
	private String m_szCmd = "";

	CClientConnection objClient = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		m_contx = this;
		btn1 = (Button)findViewById(R.id.btn1);
		btn2 = (Button)findViewById(R.id.btn2);
		btn3 = (Button)findViewById(R.id.btn3);
		btn4 = (Button)findViewById(R.id.btn4);
		chk1 = (CheckBox)findViewById(R.id.chk1);

		btn1.setOnClickListener(new OnbtnListener());
		btn2.setOnClickListener(new OnbtnListener());
		btn3.setOnClickListener(new OnbtnListener());
		btn4.setOnClickListener(new OnbtnListener());
		chk1.setOnClickListener(new OnbtnListener());
		tvServerMessage = (TextView) findViewById(R.id.textViewServerMessage);

		Common.GetEnvPath(m_contx);
	}

	/***
	 * Handler Callback for receive messages
	 */
	private static class MyHandler extends Handler {
		private final WeakReference<MainActivity> mActivity;

		public MyHandler(MainActivity activity) {
			mActivity = new WeakReference<>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity activity = mActivity.get();
			String szMsgType = msg.getData().getString(CClientConnection.Hndl_MSGTYPE, "");
			String szMsg = msg.getData().getString(CClientConnection.Hndl_MSG, "");

			if(szMsgType.compareTo(CClientConnection.MSGTYPE.STATUS) == 0){
				activity.tvServerMessage.append("Status Connection: " + szMsg + "\n");
			}
			else if(szMsgType.compareTo(CClientConnection.MSGTYPE.RECV) == 0){
				String szInfo = CClientConnection.getTagValue(szMsg, CClientConnection.XML.INFO);
				String szResult = CClientConnection.getTagValue(szMsg, CClientConnection.XML.RESULT);
				activity.tvServerMessage.append("<--" + szInfo + " : " + szResult + "\n");
			}

		}
	}

	public void CreateNew(){
		if(objClient == null) {
			objClient = new CClientConnection(mHandler, CClientConnection.PORT,
					ServerIP,
					m_szCmd,
					RegisterName,
					RegisterID);
			objClient.start();
		}
	}

	public void Release(){
		if (objClient != null) {
			objClient.OnStop();
			objClient = null;
		}
	}

	private class OnbtnListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if(v.getId() == chk1.getId()){
				if(chk1.isChecked()){
					nSendSize = 5;
				}
				else
					nSendSize = 1;
			}
			else if(v.getId() == btn3.getId()){
				m_szCmd = CClientConnection.CMDTYPE.REG;
				Release();
				CreateNew();

				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if(objClient!= null) {
					// Get Image from Assets depend on RegisterName and Sending to Server
					AssetManager assetManager = m_contx.getAssets();
					try {
						String[] arrFileName = assetManager.list(RegisterName);
						for (String filename : arrFileName) {
							Log.d(TAG, RegisterName + "/" + filename);
							InputStream istr = assetManager.open(RegisterName + "/" + filename);

							String szName = CClientConnection.getFileNameWithoutExtension(filename);
							Bitmap bmp = BitmapFactory.decodeStream(istr);
							if(!objClient.sendImage(szName, bmp))
								Release();
						}
					} catch (IOException e) {
						e.printStackTrace();
						Log.e(TAG, e.getLocalizedMessage());
					}

				}

			}

			else if(v.getId() == btn4.getId()){
				m_szCmd = CClientConnection.CMDTYPE.LOGIN;
				Release();
				CreateNew();

				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if(objClient!= null) {
					// Get one Image from Assets
					AssetManager assetManager = m_contx.getAssets();
					try {
						String[] arrFileName = assetManager.list(RegisterName);
						for (String filename : arrFileName) {
							Log.d(TAG, RegisterName + "/" + filename);
							InputStream istr = assetManager.open(RegisterName + "/" + filename);

							String szName = CClientConnection.getFileNameWithoutExtension(filename);
							Bitmap bmp = BitmapFactory.decodeStream(istr);
							if(!objClient.sendImage(szName, bmp)) {
								Release();
							}

							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
						Log.e(TAG, e.getLocalizedMessage());
					}

				}

			}

		}
	}

	public void writeInt(byte[] data, int offset, int value) {
		data[offset] = (byte)((value >>> 24) & 0xFF);
		data[offset + 1] = (byte)((value >>> 16) & 0xFF);
		data[offset + 2] = (byte)((value >>> 8) & 0xFF);
		data[offset + 3] = (byte)((value >>> 0) & 0xFF);
	}

	public int readInt(byte[] data, int offset) {
		int ch1 = data[offset] & 0xff;
		int ch2 = data[offset + 1] & 0xff;
		int ch3 = data[offset + 2] & 0xff;
		int ch4 = data[offset + 3] & 0xff;
		return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);
	}


}
