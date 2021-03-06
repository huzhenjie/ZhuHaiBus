package com.scrat.zhuhaibus.module.about;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.scrat.zhuhaibus.R;
import com.scrat.zhuhaibus.databinding.ActivityAboutBinding;
import com.scrat.zhuhaibus.framework.common.BaseActivity;
import com.scrat.zhuhaibus.framework.util.Utils;
import com.umeng.analytics.MobclickAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by scrat on 2018/3/26.
 */

public class AboutActivity extends BaseActivity {
    public static void show(Context context) {
        Intent i = new Intent(context, AboutActivity.class);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAboutBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_about);
        String ver = Utils.getVersionName(this) + "-" + Utils.getChannelName(this);
        binding.ver.setText(ver);
    }

    @Override
    public void onResume() {
        super.onResume();

        MobclickAgent.onEvent(this, "view", "AboutActivity");
        MobclickAgent.onPageStart("AboutActivity");
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd("AboutActivity");
    }

    public void sendEmail(View view) {
        try {
            Intent data = new Intent(Intent.ACTION_SENDTO);
            data.setData(Uri.parse("mailto:huzhenjie.dev@gmail.com"));
            String subject = "[" + getString(R.string.app_name) + ']' +
                    '[' + Utils.getVersionName(this) + ']' +
                    '[' + Utils.getVersionCode(this) + ']' +
                    '[' + Utils.getChannelName(this) + ']';
            data.putExtra(Intent.EXTRA_SUBJECT, subject);
            data.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_hint));
            startActivity(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void donate(View view) {
        boolean openSuccess = Utils.openOnSysBrowser(this, "https://qr.alipay.com/tsx05817omf8rmusuew8k0f");
        if (!openSuccess) {
            // 虽然失败了，还不能谢谢人家吗？？？
            showMessage(R.string.thank_you_for_donate);
        }

        reportShowingDonate("alipay", openSuccess);
    }

    private void reportShowingDonate(String channel, boolean openSuccess) {
        Map<String, String> evt = new HashMap<>();
        evt.put("type", channel);
        evt.put("state", String.valueOf(openSuccess));
        MobclickAgent.onEvent(this, "Donate", evt);
    }

}
