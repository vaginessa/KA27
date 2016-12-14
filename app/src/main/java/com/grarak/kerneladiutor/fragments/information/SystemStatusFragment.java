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
package com.grarak.kerneladiutor.fragments.information;

import android.os.Bundle;

import com.grarak.kerneladiutor.R;
import com.grarak.kerneladiutor.elements.cards.CardViewItem;
import com.grarak.kerneladiutor.fragments.RecyclerViewFragment;
import com.grarak.kerneladiutor.elements.cards.SwitchCardView;
import com.grarak.kerneladiutor.utils.kernel.SystemStatus;

/**
 * Created by bhb27 on Dec 2016
 */
public class SystemStatusFragment extends RecyclerViewFragment implements
SwitchCardView.DSwitchCard.OnDSwitchCardListener {

    private SwitchCardView.DSwitchCard mCpuService;

    @Override
    public boolean showApplyOnBoot() {
        return false;
    }

    @Override
    public void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);

        mCpuService = new SwitchCardView.DSwitchCard();
        mCpuService.setTitle(getString(R.string.status_overlay));
        mCpuService.setDescription(getString(R.string.status_overlay_summary));
        mCpuService.setChecked(SystemStatus.isCPUInfoServiceEnable(getActivity()));
        mCpuService.setOnDSwitchCardListener(this);

        addView(mCpuService);
    }

    @Override
    public void onChecked(SwitchCardView.DSwitchCard dSwitchCard, boolean checked) {
        if (dSwitchCard == mCpuService)
            SystemStatus.setCPUInfoServiceEnabled(checked, getActivity());
    }

}
