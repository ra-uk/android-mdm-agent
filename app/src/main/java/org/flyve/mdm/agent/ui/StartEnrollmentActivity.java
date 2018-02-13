/*
 *   Copyright (C) 2017 Teclib. All rights reserved.
 *
 * This file is part of flyve-mdm-android-agent
 *
 * flyve-mdm-android-agent is a subproject of Flyve MDM. Flyve MDM is a mobile
 * device management software.
 *
 * Flyve MDM is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * Flyve MDM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * ------------------------------------------------------------------------------
 * @author    Rafael Hernandez
 * @date      02/06/2017
 * @copyright Copyright (C) 2017 Teclib. All rights reserved.
 * @license   GPLv3 https://www.gnu.org/licenses/gpl-3.0.html
 * @link      https://github.com/flyve-mdm/flyve-mdm-android-agent
 * @link      https://flyve-mdm.com
 * ------------------------------------------------------------------------------
 */

package org.flyve.mdm.agent.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.flyve.mdm.agent.R;
import org.flyve.mdm.agent.core.deeplink.Deeplink;
import org.flyve.mdm.agent.core.deeplink.DeeplinkPresenter;
import org.flyve.mdm.agent.core.deeplink.DeeplinkSchema;
import org.flyve.mdm.agent.data.MqttData;
import org.flyve.mdm.agent.utils.Helpers;

public class StartEnrollmentActivity extends Activity implements Deeplink.View {

    private static final int REQUEST_EXIT = 1;

    private Deeplink.Presenter presenter;
    private RelativeLayout btnEnroll;
    private TextView txtMessage;
    private TextView txtTitle;
    private ProgressBar pb;
    private boolean mPermissions = false;

    /**
     * Called when the activity is starting
     * It shows the UI to start the enrollment
     * @param savedInstanceState if the activity is being re-initialized, it contains the data it most recently supplied
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_enrollment);

        sendBroadcast();

        presenter = new DeeplinkPresenter(this);

        // check if broker is on cache open the main activity
        MqttData cache = new MqttData( StartEnrollmentActivity.this );

        String broker = cache.getBroker();
        if(broker != null) {
            openMain();
        }

        TextView txtIntro = (TextView) findViewById(R.id.txtIntro);
        txtIntro.setText( Html.fromHtml(StartEnrollmentActivity.this.getResources().getString(R.string.walkthrough_step_1)) );
        txtIntro.setMovementMethod(LinkMovementMethod.getInstance());
        txtMessage = (TextView) findViewById(R.id.txtMessage);
        txtTitle = (TextView) findViewById(R.id.txtTitle);
        pb = (ProgressBar) findViewById(R.id.progressBar);

        // get the deeplink
        Intent intent = getIntent();
        Uri data = intent.getData();
        try {
            String deeplink = data.getQueryParameter("data");
            presenter.lint(StartEnrollmentActivity.this, deeplink);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }

        requestPermission();

        btnEnroll = (RelativeLayout) findViewById(R.id.btnEnroll);
        btnEnroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMain();

//                if(Build.VERSION.SDK_INT < 23) {
//                    mPermissions = true;
//                }
//s
//                if(mPermissions) {
//                    btnEnroll.setVisibility(View.GONE);
//                    txtMessage.setText(getResources().getString(R.string.please_wait));
//                    pb.setVisibility(View.VISIBLE);
//
//                    presenter.openEnrollment(StartEnrollmentActivity.this, REQUEST_EXIT);
//                } else {
//                    showError("All permissions are require");
//                    requestPermission();
//                }
            }
        });
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(StartEnrollmentActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                },
                1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED
                        && grantResults[3] == PackageManager.PERMISSION_GRANTED
                        && grantResults[4] == PackageManager.PERMISSION_GRANTED) {
                    mPermissions = true;
                } else {
                    mPermissions = false;
                }
            }
        }
    }

    /**
     * Send a Broadcast with the Close Action
     */
    public void sendBroadcast() {
        //send broadcast
        Intent in = new Intent();
        in.setAction("flyve.ACTION_CLOSE");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(in);
    }

    /**
     * Open the main activity
     */
    private void openMain() {
        Intent intent = new Intent(StartEnrollmentActivity.this, MainActivity.class);
        StartEnrollmentActivity.this.startActivity(intent);
        StartEnrollmentActivity.this.finish();
    }

    /**
     * Shows an error message
     * @param message
     */
    @Override
    public void showError(String message) {
        txtTitle.setText(getResources().getString(R.string.fail_enroll));
        txtMessage.setText(message);
        Helpers.snack(this, message, this.getResources().getString(R.string.snackbar_close), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    @Override
    public void lintSuccess(DeeplinkSchema deeplinkSchema) {
        presenter.saveMQTTConfig(StartEnrollmentActivity.this, deeplinkSchema.getUrl(), deeplinkSchema.getUserToken(), deeplinkSchema.getInvitationToken());
        presenter.saveSupervisor(StartEnrollmentActivity.this, deeplinkSchema.getName(), deeplinkSchema.getPhone(), deeplinkSchema.getWebsite(), deeplinkSchema.getEmail());
    }

    @Override
    public void openEnrollSuccess() {
        btnEnroll.setVisibility(View.VISIBLE);
        pb.setVisibility(View.GONE);
        txtMessage.setText(getResources().getString(R.string.success));
        txtTitle.setText(getResources().getString(R.string.start_enroll));
    }

    @Override
    public void openEnrollFail() {
        btnEnroll.setVisibility(View.VISIBLE);
        pb.setVisibility(View.GONE);
    }

    /**
     * Called when the launched activity exits
     * @param requestCode the request code originally supplied, it identifies who this result came from
     * @param resultCode the result code returned
     * @param data an intent which can return result data to the caller
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EXIT && resultCode == RESULT_OK) {
            this.finish();
        }
    }
}
