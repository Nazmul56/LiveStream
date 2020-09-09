package io.nazmul.vlive.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import io.agora.vlive.R;
import io.nazmul.vlive.ui.BaseActivity;

public class DisclaimerActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);
        hideStatusBar(true);
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        View topLayout = findViewById(R.id.activity_disclaimer_title_layout);
        if (topLayout != null) {
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams)
                            topLayout.getLayoutParams();
            params.topMargin += systemBarHeight;
            topLayout.setLayoutParams(params);
        }
    }
}
