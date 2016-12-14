package com.sdf.aso.debuglog;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements LANLogDebugClient.ILogCallbackImpl {
    private final static String TAG = "dfsu";
    private LANLogDebugClient mLANLogDebugClient;
    private TextView mShowLogs;
    private EditText mServiceHost;
    private EditText mServicePort;
    private int mPrintCount = 0;

    String mHost;
    int mPort;

    private Handler mUIHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void dispatchMessage(Message msg) {
            String logs = (String) msg.obj;
            mShowLogs.append(logs);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mShowLogs = (TextView) findViewById(R.id.message_show);
        mServiceHost = (EditText) findViewById(R.id.service_host);
        mServicePort = (EditText) findViewById(R.id.service_port);
        mLANLogDebugClient = new LANLogDebugClient(this);
        findViewById(R.id.connection_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLANLogDebugClient.getConnectionStatus()) {
                    Toast.makeText(MainActivity.this, "服务器已连接，无需重复连接", Toast.LENGTH_SHORT).show();
                    return;
                }
                mHost = mServiceHost.getText().toString().trim();
                if (TextUtils.isEmpty(mHost)) {
                    mHost = "127.0.0.1";
                }
                String port = mServicePort.getText().toString().trim();
                if (TextUtils.isEmpty(port)) {
                    port = "6666";
                }
                try {
                    mPort = Integer.valueOf(port);
                } catch (Exception e) {
                    printLog(e.toString());
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mLANLogDebugClient.connectServer(mPort, mHost, "dfsu");
                    }
                }).start();

            }
        });
        findViewById(R.id.disconnection_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLANLogDebugClient.closeConnection();
            }
        });
        findViewById(R.id.clean_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShowLogs.setText("");
                mPrintCount = 0;
                Toast.makeText(MainActivity.this, "已清除", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.print_logs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPrintCount++;
                printLog("第" + mPrintCount + "次log打印");
            }
        });
    }

    private void printLog(final String logs) {
        Message msg = mUIHandler.obtainMessage();
        msg.obj = logs + "\n";
        mUIHandler.sendMessage(msg);
        Log.d(TAG, "printLog : " + logs + "\n");
        Log.d(TAG, mLANLogDebugClient.getConnectionStatus() + ":" + mLANLogDebugClient);
        if (null != mLANLogDebugClient && mLANLogDebugClient.getConnectionStatus()) {
            Log.d(TAG, TAG.equals(mLANLogDebugClient.getTAG()) + ":" + TAG + "=" + mLANLogDebugClient.getTAG());
            if (TAG.equals(mLANLogDebugClient.getTAG())) {
                mLANLogDebugClient.sendMessage(logs);
            }
        }
    }

    @Override
    public void callbackLog(String logs) {
        printLog(logs);
    }

    @Override
    public void callbackUser(String user, int type) {
        printLog("type = " + type + ",user = " + user);
    }

    @Override
    public void errorCallback(String error, int errorType) {
        printLog("errorType = " + errorType + ",error = " + error);
    }
}
