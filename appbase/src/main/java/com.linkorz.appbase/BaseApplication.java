package com.linkorz.appbase;

import android.app.Application;

import com.linkorz.appbase.utils.RebootThreadExceptionHandler;

/**
 * Created by liangxl
 * Date: 18-9-30
 * Description: base application with crash handle
 */

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 程序异常关闭1s之后重新启动
        if (!BuildConfig.DEBUG) {
            new RebootThreadExceptionHandler(getBaseContext(), "Sorry to reboot!");
        }
    }

}
