package com.tangce.filedownloader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements FileDownloader.FileDownLoaderListener {

    private TextView mStatus;

    private View start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        mStatus = (TextView) findViewById(R.id.tv_status);
    }

    public void start(View view) {
        start = view;
        view.setEnabled(false);
        FileDownloader.getInstance(this).setLoaderListener(this);
        FileDownloader.start(this, "http://**/upload/apk/CSH_v3.6.0.apk");
    }

    public void cancel(View view) {
        start.setEnabled(true);
        FileDownloader.cancel(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        start.setEnabled(true);
        FileDownloader.cancel(this);
    }

    @Override
    public void onDownLoadComplete() {
        start.setEnabled(true);
        mStatus.setText("download complete");
    }

    @Override
    public void onDownLoadError() {
        start.setEnabled(true);
        mStatus.setText("download error,please try again");
    }

    @Override
    public void onDownLoading(String p) {
        mStatus.setText("current download progress:" + p);
    }
}
