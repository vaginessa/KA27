/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.grarak.kerneladiutor.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import com.grarak.kerneladiutor.R;
import com.grarak.kerneladiutor.utils.kernel.CPU;
import com.grarak.kerneladiutor.utils.kernel.GPU;
import com.grarak.kerneladiutor.utils.Utils;
import com.grarak.kerneladiutor.utils.kernel.SystemStatus;
import com.kerneladiutor.library.root.RootFile;
import com.kerneladiutor.library.root.RootUtils;
import com.grarak.kerneladiutor.utils.Constants;

import java.lang.StringBuffer;

public class CPUInfoService extends Service {
    private View mView;
    private Thread mCurCPUThread;
    private final String TAG = "CPUInfoService";
    private PowerManager mPowerManager;
    private int mNumCpus = 1;
    private String[] mCurrFreq = null;
    private String[] mCurrGov = null;

    private class CPUView extends View {
        private Paint mOnlinePaint;
        private Paint mOfflinePaint;
        private float mAscent;
        private int mFH;
        private int mMaxWidth;
        private int mPaddingRight = 10;
        private int mPaddingLeft = 10;
        private int mPaddingTop = 10;
        private int mPaddingBottom = 10;

        private int mNeededWidth;
        private int mNeededHeight;

        private String mCPUTemp;
        private String mGPU;
        private boolean mDataAvail;

        private Handler mCurCPUHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.obj == null) {
                    return;
                }
                if (msg.what == 1) {
                    String msgData = (String) msg.obj;
                    try {
                        String[] parts = msgData.split(";");
                        mCPUTemp = parts[0];
                        mGPU = parts[1];

                        String[] cpuParts = parts[2].split("\\|");
                        for (int i = 0; i < cpuParts.length; i++) {
                            String cpuInfo = cpuParts[i];
                            String cpuInfoParts[] = cpuInfo.split(":");
                            if (cpuInfoParts.length == 2) {
                                mCurrFreq[i] = cpuInfoParts[0];
                                mCurrGov[i] = cpuInfoParts[1];
                            } else {
                                mCurrFreq[i] = "0";
                                mCurrGov[i] = "";
                            }
                        }
                        mDataAvail = true;
                        updateDisplay();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.e(TAG, "illegal data " + msgData);
                    }
                }
            }
        };

        CPUView(Context context) {
            super(context);

            setPadding(4, 4, 4, 4);
            int textSize = 10;
            float density = context.getResources().getDisplayMetrics().density;
            if (density < 1) {
                textSize = 9;
            } else {
                textSize = (int)(12 * density);
                if (textSize < 10) {
                    textSize = 10;
                }
            }
            mOnlinePaint = new Paint();
            mOnlinePaint.setAntiAlias(true);
            mOnlinePaint.setTextSize(textSize);
            mOnlinePaint.setARGB(255, 0, 255, 0);

            mOfflinePaint = new Paint();
            mOfflinePaint.setAntiAlias(true);
            mOfflinePaint.setTextSize(textSize);
            mOfflinePaint.setARGB(255, 255, 0, 0);

            mAscent = mOnlinePaint.ascent();
            float descent = mOnlinePaint.descent();
            mFH = (int)(descent - mAscent + .5f);

            final String maxWidthStr = "GPU: simple_ondemand: 2800MHz T=30.1C"; // probably biggest possible
            mMaxWidth = (int) mOnlinePaint.measureText(maxWidthStr);

            updateDisplay();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mCurCPUHandler.removeMessages(1);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(resolveSize(mNeededWidth, widthMeasureSpec),
                resolveSize(mNeededHeight, heightMeasureSpec));
        }

        private String getCPUInfoString(int i) {
            String freq = mCurrFreq[i];
            String gov = mCurrGov[i];
            return "CORE:" + i + " " + gov + ":" + freq;
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!mDataAvail) {
                return;
            }
            int x = (getWidth() - 1) - mPaddingRight - mMaxWidth;
            int y = ((mPaddingTop - (int) mAscent) - 1);

            canvas.drawText(mCPUTemp, x, y, mOnlinePaint);
            y += mFH;
            canvas.drawText("GPU: " + mGPU, x, y, mOnlinePaint);
            y += mFH;
            for (int i = 0; i < mCurrFreq.length; i++) {
                String cpu = getCPUInfoString(i);
                String freq = mCurrFreq[i];
                if (!freq.equals("0")) {
                    canvas.drawText(cpu, x, y, mOnlinePaint);
                } else {
                    canvas.drawText(cpu, x, y, mOfflinePaint);
                }
                y += mFH;
            }
        }

        void updateDisplay() {
            if (!mDataAvail) {
                return;
            }
            final int NW = mNumCpus + 1; //+1 for GPU

            int neededWidth = mPaddingLeft + mPaddingRight + mMaxWidth;
            int neededHeight = mPaddingTop + mPaddingBottom + (mFH * (1 + NW));
            if (neededWidth != mNeededWidth || neededHeight != mNeededHeight) {
                mNeededWidth = neededWidth;
                mNeededHeight = neededHeight;
                requestLayout();
            } else {
                invalidate();
            }
        }

        public Handler getHandler() {
            return mCurCPUHandler;
        }
    }

    protected class CurCPUThread extends Thread {
        private boolean mInterrupt = false;
        private Handler mHandler;

        public CurCPUThread(Handler handler, int numCpus) {
            mHandler = handler;
            mNumCpus = numCpus;
        }

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(500);
                    StringBuffer sb = new StringBuffer();
                    sb.append("Temp - BAT: " + CPUInfoService.readOneLine(String.format(Locale.US, Constants.CPU_TEMP_ZONED, 0), true) + " - CPU: " + CPUInfoService.readOneLine(String.format(Locale.US, Constants.CPU_TEMP_ZONED, 1), true)); 
                    sb.append(";");
                    sb.append(CPUInfoService.readOneLine(Constants.GPU_SCALING_FDB00000_QCOM_GOVERNOR, true) + ":" + CPUInfoService.readOneLine(Constants.GPU_CUR_FDB00000_QCOM_FREQ, true) + getString(R.string.mhz) + " T " + CPUInfoService.readOneLine(String.format(Locale.US, Constants.CPU_TEMP_ZONED, 10), true));
                    sb.append(";");

                    for (int i = 0; i < mNumCpus; i++) {
                        String currGov = "";
                        String currFreq = CPUInfoService.readOneLine(String.format(Locale.US, Constants.CPU_CUR_FREQ, i), true);
                        if (currFreq == null) {
                            currFreq = "0";
                            currGov = "";
                        } else {
                            currGov = CPUInfoService.readOneLine(String.format(Locale.US, Constants.CPU_SCALING_GOVERNOR, i), true);
                            currFreq = currFreq + getString(R.string.mhz) + " T " + CPUInfoService.readOneLine((String.format(Locale.US, Constants.CPU_TEMP_ZONED, i + 6)), true);
			}
                        sb.append(currFreq + ":" + currGov + "|");
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    mHandler.sendMessage(mHandler.obtainMessage(1, sb.toString()));
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mNumCpus = CPU.getCoreCount();
        mCurrFreq = new String[mNumCpus];
        mCurrGov = new String[mNumCpus];

        mView = new CPUView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT);
        //params.gravity = Gravity.END | Gravity.TOP;
        params.gravity = Gravity.START | Gravity.TOP;
        //params.gravity = Gravity.TOP;
        //params.gravity = Gravity.START | Gravity.BOTTOM;
        //params.gravity = Gravity.END | Gravity.BOTTOM; //can't be used because Android Touch-Event Hijacking
        //params.gravity = Gravity.BOTTOM;
        params.setTitle("CPU Info");

        mCurCPUThread = new CurCPUThread(mView.getHandler(), mNumCpus);
        mCurCPUThread.start();

        Log.d(TAG, "started CurCPUThread");

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCurCPUThread.isAlive()) {
            mCurCPUThread.interrupt();
            try {
                mCurCPUThread.join();
            } catch (InterruptedException e) {}
        }
        Log.d(TAG, "stopped CurCPUThread");
        ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mView);
        mView = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static String readOneLine(String fname, boolean asRoot) {
        if (asRoot) return new RootFile(fname).readFile();

        BufferedReader br;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fname), 512);
            try {
                line = br.readLine();
            } finally {
                br.close();
            }
        } catch (Exception e) {
            return null;
        }
        return line;
    }
}
