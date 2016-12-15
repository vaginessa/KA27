/*
 * Copyright (C) 2015 Willi Ye
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
package com.grarak.kerneladiutor.utils.kernel;

import android.content.Context;
import android.content.Intent;

import java.util.Locale;

import com.grarak.kerneladiutor.R;
import com.grarak.kerneladiutor.utils.Constants;
import com.grarak.kerneladiutor.utils.Utils;
import com.grarak.kerneladiutor.services.CPUInfoService;

/**
 * Created by bhb27 dec 2016
 */
public class SystemStatus implements Constants {

    public static boolean isCPUInfoServiceEnable(Context context) {
        try {
            return Utils.getBoolean("CPUInfoService", false, context);
        } catch (NullPointerException err) {
            return false;
        }
    }

    public static void setCPUInfoServiceEnabled(boolean active, Context context) {
        Utils.saveBoolean("CPUInfoService", active, context);
        //If deactivate reset gov to core 0 freq
        if (active)
            context.startService(new Intent(context, CPUInfoService.class));
        else
            context.stopService(new Intent(context, CPUInfoService.class));

    }

    public static String getTemp(int zone) {
        String zoned = String.format(Locale.US, CPU_TEMP_ZONED, zone);
        if (!Utils.existFile(zoned)) return "";
        int temp = Utils.stringToInt(Utils.readFile(zoned));
        if (temp > 1000) temp /= 1000;
        else if (temp > 200) temp /= 10;
	return String.valueOf(temp) + "°C";
	//return String.valueOf(temp* 9 / 5 + 32,2) + "°F";
    }

    public static int getGpuCurFreq() {
	if (!Utils.existFile(GPU_CUR_FDB00000_QCOM_FREQ)) return 0;
        String value = Utils.readFile(GPU_CUR_FDB00000_QCOM_FREQ);
        if (value != null) return ((int) Utils.stringToInt(value) / 1000000);
        return 0;
    }

    public static String getGpuGovernor() {
	if (!Utils.existFile(GPU_SCALING_FDB00000_QCOM_GOVERNOR)) return "";
        String value = Utils.readFile(GPU_SCALING_FDB00000_QCOM_GOVERNOR);
        if (value != null) return value;
        return "";
    }

    public static String getCurGovernor(int core) {
	if (!Utils.existFile(String.format(Locale.US, CPU_SCALING_GOVERNOR, core))) return "";
        String value = Utils.readFile(String.format(Locale.US, CPU_SCALING_GOVERNOR, core));
        if (value != null) return value;
        return "";
    }

    public static int getCurFreq(int core) {
	if (!Utils.existFile(String.format(Locale.US, CPU_CUR_FREQ, core))) return 0;
        String value = Utils.readFile(String.format(Locale.US, CPU_CUR_FREQ, core));
        if (value != null) return Utils.stringToInt(value);
        return 0;
    }
}
