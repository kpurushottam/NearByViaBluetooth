package com.krp.social.nearby;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    public static final String KEY_INTENT_ACTIVITY_PROFILE = "activityProfile";

    private EditText etUserName, etUserAge, etUserInterests;
    private RadioButton rbMale, rbFemale;
    private LinearLayout btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        User user = (User) getIntent().getSerializableExtra(KEY_INTENT_ACTIVITY_PROFILE);
        if(user == null && NearByApplication.getInstance().getUserName() != null) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();

        } else {
            setContentView(R.layout.activity_login);
            initViews();

            if(user != null) {
                btnLogin.setVisibility(View.GONE);

                etUserName.setText(user.username);
                etUserAge.setText(user.age);
                etUserInterests.setText(user.interests);

                if(user.male) {
                    rbMale.setChecked(true);
                }
            }
        }
    }

    private void initViews() {
        etUserName = (EditText) findViewById(R.id.username);
        etUserAge = (EditText) findViewById(R.id.age);
        etUserInterests = (EditText) findViewById(R.id.interests);

        rbMale = (RadioButton) findViewById(R.id.male);
        rbMale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isMale = true;
                }
            }
        });

        rbFemale = (RadioButton) findViewById(R.id.female);
        rbFemale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isFemale = true;
                }
            }
        });

        btnLogin = (LinearLayout) findViewById(R.id.layout_btn_login);
    }

    private boolean isMale, isFemale;
    public void registerUser(View v) {
        String userName, userAge, userInterests;

        userName = etUserName.getText().toString();
        userAge = etUserAge.getText().toString();
        userInterests = etUserInterests.getText().toString();

        if(userName != null && userName.length() != 0 &&
                userAge != null && userAge.length() != 0 &&
                userInterests != null && userInterests.length() != 0 &&
                (isMale || isFemale)) {

            NearByApplication.getInstance().setUserName(userName);
            NearByApplication.getInstance().setUserAge(userAge);
            NearByApplication.getInstance().setUserGender(isMale);
            NearByApplication.getInstance().setUserInterests(userInterests);

            startActivity(new Intent(this, DashboardActivity.class));
            finish();

        } else {
            alert("Please try again with correct inputs!");
        }
    }


    private Toast mToast;
    /**
     * To show alert messages as Toasts
     * @param message
     */
    public void alert(String message) {
        if(mToast == null) {
            mToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        }

        if(mToast.getView().isShown()) {
            mToast.cancel();
        }

        mToast.setText(message);
        mToast.show();
    }
}
