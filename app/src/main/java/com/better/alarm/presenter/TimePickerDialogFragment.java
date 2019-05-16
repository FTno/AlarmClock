/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * limitations under the License
 */

package com.better.alarm.presenter;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.better.alarm.R;
import com.better.alarm.util.Optional;
import com.better.alarm.view.TimePicker;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Cancellable;

import static com.better.alarm.configuration.AlarmApplication.container;
import static com.better.alarm.configuration.AlarmApplication.themeHandler;

/**
 * Dialog to set alarm time.
 */
public class TimePickerDialogFragment extends DialogFragment {
    private TimePicker mPicker;
    private SingleEmitter<Optional<PickedTime>> emitter;

    /**
     * @return
     */
    public static TimePickerDialogFragment newInstance() {
        final TimePickerDialogFragment frag = new TimePickerDialogFragment();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_TITLE, themeHandler().getIdForName(TimePickerDialogFragment.class.getName()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.time_picker_dialog, null);

        Button set = (Button) v.findViewById(R.id.set_button);
        Button cancel = (Button) v.findViewById(R.id.cancel_button);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notifyOnCancelListener();
                dismiss();
            }
        });
        mPicker = (TimePicker) v.findViewById(R.id.time_picker);
        mPicker.setSetButton(set);
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PickedTime picked = new PickedTime(mPicker.getHours(), mPicker.getMinutes());
                emitter.onSuccess(Optional.<PickedTime>of(picked));
                dismiss();
            }
        });
        return v;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        notifyOnCancelListener();
        super.onCancel(dialog);
    }

    private void notifyOnCancelListener() {
        emitter.onSuccess(Optional.<PickedTime>absent());
    }

    private void setEmitter(SingleEmitter<Optional<PickedTime>> emitter) {
        this.emitter = emitter;
    }

    public static Single<Optional<PickedTime>> showTimePicker(final FragmentManager fragmentManager) {
        return Single.create(new SingleOnSubscribe<Optional<PickedTime>>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<Optional<PickedTime>> emitter) throws Exception {

                final FragmentTransaction ft = fragmentManager.beginTransaction();
                final Fragment prev = fragmentManager.findFragmentByTag("time_dialog");
                if (prev != null) {
                    ft.remove(prev);
                }

                final TimePickerDialogFragment fragment = TimePickerDialogFragment.newInstance();

                try {
                    fragment.show(ft, "time_dialog");
                    fragment.setEmitter(emitter);
                    emitter.setCancellable(new Cancellable() {
                        @Override
                        public void cancel() throws Exception {
                            if (fragment.isAdded()) {
                                fragment.dismiss();
                            }
                        }
                    });
                } catch (IllegalStateException e) {
                    container().logger().e("Failed to show picker", e);
                    emitter.onSuccess(Optional.<PickedTime>absent());
                }
            }
        });
    }
}

