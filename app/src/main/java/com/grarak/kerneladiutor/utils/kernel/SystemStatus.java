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
}
