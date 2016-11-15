package com.tangce.filedownloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

/**
 * Created by Tanck on 11/9/2016.
 * <p>
 * Describe:
 */
public class FileDownloader {

    private static final boolean DEBUG = true;

    private static final String TAG = "Tanck";

    private final int ERROR_CODE = 400;

    private final int DEFAULT_TIMES = 3;

    private int mTryTimes = 0;

    private long mTempProgress;

    private long mRemoteFileLength;

    private long mFileStartP = 0;

    private long mFileEndP;

    private String mFilepath;

    private String mFileName = "temp.apk";

    private final String mFolderName = "Android";

    private static FileDownloader instance;

    private DecimalFormat mDf;

    private DownLoader mDownloader;

    private FileDownLoaderListener loaderListener;

    private Context context;

    public void setLoaderListener(FileDownLoaderListener loaderListener) {
        this.loaderListener = loaderListener;
    }

    public interface FileDownLoaderListener {
        void onDownLoadComplete();

        void onDownLoadError();

        /**
         * the p is current download position
         *
         * @param p
         */
        void onDownLoading(String p);
    }

    private void log(String data) {
        if (DEBUG)
            Log.d(TAG, data);
    }

    private FileDownloader(Context context) {
        this.context = context;
        mDf = new DecimalFormat("0.00");
//        mDownloader = new DownLoader();
        mFilepath = Environment.getExternalStorageDirectory() + File.separator + mFolderName + File.separator + mFileName;
    }

    public static FileDownloader getInstance(Context context) {
        if (null == instance) {
            synchronized (FileDownloader.class) {
                if (null == instance)
                    instance = new FileDownloader(context);
            }
        }
        return instance;
    }

    public static void start(Context context, String url) {
        getInstance(context).execute(url);
    }

    private void execute(String url) {
        mDownloader = new DownLoader();
        mDownloader.execute(url);
    }


    public static void cancel(Context context) {
        getInstance(context).cancel();
    }

    private void cancel() {
        mDownloader.cancel(true);
    }

    private class DownLoader extends AsyncTask<String, Float, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            HttpURLConnection connection;
            InputStream input = null;
            RandomAccessFile oSavedFile = null;
            boolean result = true;
            mTryTimes = 0;
            while (DEFAULT_TIMES >= mTryTimes) {
                try {
                    mFileStartP = (int) getSavePosition();
                    mTempProgress = mFileStartP;
                    mRemoteFileLength = getRemoteFileLength(params[0]);
                    log("length:" + mRemoteFileLength);
                    mFileEndP = mRemoteFileLength;
                    URL mUrl = new URL(params[0]);
                    connection = (HttpURLConnection) mUrl.openConnection();
                    setHeader(connection);
                    connection.setRequestProperty("RANGE", "bytes=" + mFileStartP + "-" + mFileEndP);
                    if (ERROR_CODE <= connection.getResponseCode()) {
                        ++mTryTimes;
                        result = false;
                    }
                    input = connection.getInputStream();
                    oSavedFile = new RandomAccessFile(mFilepath, "rw");
                    oSavedFile.seek(mFileStartP);// move to save position
                    byte[] b = new byte[1024 * 10];
                    int nRead;

                    while ((nRead = input.read(b)) > 0) {
                        savePosition(mTempProgress);
                        if (mDownloader.isCancelled()) {
                            mTryTimes = 4;
                            result = false;
                            break;
                        }
                        oSavedFile.write(b, 0, nRead);
                        mTempProgress += nRead;
                        publishProgress((float) ((mTempProgress / (1.00 * mRemoteFileLength)) * 100.00));
                        if (mTempProgress == mRemoteFileLength) {
                            mTryTimes = 4;
                            result = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    log("error :" + mTempProgress);
                    if (-1 != mTempProgress)
                        savePosition(mTempProgress);
                    result = false;
                    ++mTryTimes;
                    e.printStackTrace();
                } finally {
                    try {
                        input.close();
                        oSavedFile.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return result;
        }


        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            log("onPostExecute:" + aBoolean);
            if (null == loaderListener)
                return;
            if (aBoolean) {
                savePosition(0);
                loaderListener.onDownLoadComplete();
            } else
                loaderListener.onDownLoadError();
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            super.onProgressUpdate(values);
            String tempProgress = mDf.format(values[0]);
            if (null != loaderListener)
                loaderListener.onDownLoading(tempProgress);
            log("onProgressUpdate:" + tempProgress);
        }


        private void setHeader(HttpURLConnection con) {
            con.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.3) Gecko/2008092510 Ubuntu/8.04 (hardy) Firefox/3.0.3");
            con.setRequestProperty("Accept-Language", "en-us,en;q=0.7,zh-cn;q=0.3");
            con.setRequestProperty("Accept-Encoding", "aa");
            con.setRequestProperty("Accept-Charset",
                    "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            con.setRequestProperty("Keep-Alive", "300");
            con.setRequestProperty("Connection", "keep-alive");
            con.setRequestProperty("If-Modified-Since",
                    "Fri, 02 Jan 2009 17:00:05 GMT");
            con.setRequestProperty("If-None-Match", "\"1261d8-4290-df64d224\"");
            con.setRequestProperty("Cache-Control", "max-age=0");
        }

        private long getRemoteFileLength(String url) throws IOException {
            URL mUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) mUrl.openConnection();
            setHeader(connection);
            return connection.getContentLength();
        }

        private void savePosition(long position) {
            SharedPreferences sp = context.getSharedPreferences(FileDownloader.class.getSimpleName(), Context.MODE_PRIVATE);
            sp.edit().putLong("start", position).commit();
        }

        private long getSavePosition() {
            SharedPreferences sp = context.getSharedPreferences(FileDownloader.class.getSimpleName(), Context.MODE_PRIVATE);
            return sp.getLong("start", 0);
        }
    }

}
