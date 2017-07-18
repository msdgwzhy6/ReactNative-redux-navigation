package cache.pk.com.rncache;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactInstanceManagerBuilder;
import com.facebook.react.ReactPackage;
import com.facebook.react.ReactRootView;
import com.facebook.react.common.LifecycleState;
import com.facebook.react.devsupport.DoubleTapReloadRecognizer;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;
import com.facebook.react.shell.MainReactPackage;

import java.io.File;

import javax.annotation.Nullable;

public abstract class BaseReactActivity
        extends AppCompatActivity
        implements DefaultHardwareBackBtnHandler, PermissionAwareActivity {

    private static final String TAG = "BaseReactActivity";
    private static final String REDBOX_PERMISSION_MESSAGE =
            "Overlay permissions needs to be granted in order for react native apps to run in dev mode";
    public static final String JS_MAIN_BUNDLE_NAME = "index.android";
    public static final String JS_BUNDLE_LOCAL_FILE = "index.android.bundle";

    protected ReactInstanceManager mReactInstanceManager;
    private
    @Nullable
    PermissionListener mPermissionListener;
    private DoubleTapReloadRecognizer mDoubleTapReloadRecognizer;
    private ReactRootView mReactRootView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ReactNative", "BaseReactActivity.onCreate " + this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Intent serviceIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(serviceIntent);
                Toast.makeText(this, REDBOX_PERMISSION_MESSAGE, Toast.LENGTH_LONG).show();
            }
        }
        setContentView(R.layout.activity_react_native);
        mReactRootView = (ReactRootView) findViewById(R.id.baseReactView);
        iniReactRootView();
    }

    protected void iniReactRootView() {
        ReactInstanceManagerBuilder builder = ReactInstanceManager.builder()
                .setApplication(getApplication())
                .setJSMainModuleName(TextUtils.isEmpty(getMainBundleName()) ? JS_MAIN_BUNDLE_NAME : getMainBundleName())
                .setUseDeveloperSupport(BuildConfig.DEBUG)
                .addPackage(new MainReactPackage())
                .setInitialLifecycleState(LifecycleState.BEFORE_CREATE)
                .setCurrentActivity(this);
                //.setNativeModuleCallExceptionHandler(nativeModuleCallExceptionHandler);
        String jsBundleFile = getJSBundleFile();
        File file = null;
        if (!TextUtils.isEmpty(jsBundleFile)) {
            file = new File(jsBundleFile);
        }
        if (file != null && file.exists()) {
            builder.setJSBundleFile(getJSBundleFile());
        } else {
            String bundleAssetName = getBundleAssetName();
            builder.setBundleAssetName(TextUtils.isEmpty(bundleAssetName) ? JS_BUNDLE_LOCAL_FILE : bundleAssetName);
        }
        if (getPackages() != null) {
            builder.addPackage(getPackages());
        }
        mReactInstanceManager = builder.build();
        mReactRootView.startReactApplication(mReactInstanceManager, getJsModuleName(), null);
        mDoubleTapReloadRecognizer = new DoubleTapReloadRecognizer();
    }

    protected abstract String getJsModuleName();

    abstract protected ReactPackage getPackages();

    /**
     * 与modlue对应的js文件的名称
     *
     * @return
     */
    abstract protected String getMainBundleName();

    /**
     * 从本地sd卡读取bundle文件
     *
     * @return
     */
    abstract protected String getJSBundleFile();

    /**
     * assets 中自带的 bundle名称
     *
     * @return
     */
    abstract protected String getBundleAssetName();

    @Override
    protected void onDestroy() {
        Log.d("destroy", "destroy");
        mReactInstanceManager.destroy();
        mReactRootView.unmountReactApplication();
        mReactInstanceManager.onHostDestroy(this);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ReactNative", "BaseReactActivity.onResume " + this);
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostResume(this, new DefaultHardwareBackBtnHandler() {
                @Override
                public void invokeDefaultOnBackPressed() {
                    finish();
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onActivityResult(BaseReactActivity.this, requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mReactInstanceManager != null && BuildConfig.DEBUG) {
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                mReactInstanceManager.showDevOptionsDialog();
                return true;
            }
            if (mDoubleTapReloadRecognizer != null && mDoubleTapReloadRecognizer.didDoubleTapR(keyCode, getCurrentFocus())) {
                mReactInstanceManager.getDevSupportManager().handleReloadJS();
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void invokeDefaultOnBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onNewIntent(intent);
        } else {
            super.onNewIntent(intent);
        }
    }

    @TargetApi(23)
    @Override
    public void requestPermissions(String[] permissions, int requestCode, PermissionListener listener) {
        mPermissionListener = listener;
        this.requestPermissions(permissions, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        if (mPermissionListener != null &&
                mPermissionListener.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            mPermissionListener = null;
        }
    }


}
