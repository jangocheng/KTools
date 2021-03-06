package com.jiangkang.ktools;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.jiangkang.ktools.service.AIDLDemoActivity;
import com.jiangkang.tools.permission.RxPermissions;
import com.jiangkang.tools.struct.JsonGenerator;
import com.jiangkang.tools.system.ContactHelper;
import com.jiangkang.tools.utils.ClipboardUtils;
import com.jiangkang.tools.utils.FileUtils;
import com.jiangkang.tools.utils.ToastUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dalvik.system.DexClassLoader;
import io.reactivex.functions.Consumer;


/**
 * Created by jiangkang on 2017/9/5.
 * description：与系统相关的Demo
 * 1.打开通讯录，选择联系人，获得联系人姓名和手机号
 * 2.获取联系人列表
 * 3.设置文本到剪贴板，从剪贴板中取出文本
 */

public class SystemActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_PICK_CONTACT = 1001;

    private static final int REQUEST_PERMISSION_GET_CONTACTS = 1002;

    private static final int REQUEST_PICK_CONTACT = 1112;

    private static final String TAG = SystemActivity.class.getSimpleName();


    @BindView(R.id.btn_open_contacts)
    Button mBtnOpenContacts;

    @BindView(R.id.btn_get_all_contacts)
    Button btnGetAllContacts;

    @BindView(R.id.et_content)
    EditText etContent;

    @BindView(R.id.btn_set_clipboard)
    Button btnSetClipboard;

    @BindView(R.id.btn_get_clipboard)
    Button btnGetClipboard;
    @BindView(R.id.radio_btn_contact)
    RadioButton radioBtnContact;
    @BindView(R.id.radio_btn_storage)
    RadioButton radioBtnStorage;
    @BindView(R.id.btn_request_permission)
    Button btnRequestPermission;
    @BindView(R.id.btn_hide_app_icon)
    Button mBtnHideAppIcon;
    @BindView(R.id.btn_load_dex)
    Button mBtnLoadDex;
    @BindView(R.id.btn_exit_app)
    Button mBtnExitApp;
    @BindView(R.id.btn_hide_status_bar)
    Button mBtnHideStatusBar;
    @BindView(R.id.btn_hide_virtual_navbar)
    Button mBtnHideVirtualNavbar;
    @BindView(R.id.btn_aidl)
    Button mBtnAidl;

    private JSONObject jsonObject;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system);
        setTitle("System");
        ButterKnife.bind(this);
        FileUtils.copyAssetsToFile("code/hello_world_dex.jar", "hello_world_dex.jar");
    }

    @TargetApi(Build.VERSION_CODES.M)
    @OnClick(R.id.btn_open_contacts)
    public void onOpenContactsClicked() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.READ_CONTACTS)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) throws Exception {
                        if (granted) {
                            gotoContactPage();
                        } else {
                            ToastUtils.showShortToast("权限被拒绝");
                        }
                    }
                });

    }

    private void gotoContactPage() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    Log.d(TAG, "onActivityResult: 什么东西都没有选");
                } else {
                    Log.d(TAG, "onActivityResult: 有返回");
                    Cursor cursor = getContentResolver().query(
                            data.getData(),
                            null,
                            null,
                            null,
                            null
                    );
                    JSONObject contact = handleCursor(cursor);
                    try {
                        new AlertDialog.Builder(this)
                                .setTitle("选择的联系人信息")
                                .setMessage(contact.toString(4))
                                .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create()
                                .show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private JSONObject handleCursor(Cursor data) {
        JSONObject result = new JSONObject();
        if (data != null && data.moveToFirst()) {
            do {
                String name = data.getString(
                        data.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                );
                int id = data.getInt(
                        data.getColumnIndex(ContactsContract.Contacts._ID)
                );

                //指定获取NUMBER这一列数据
                String[] phoneProjection = new String[]{
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                };

                Cursor cursor = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        phoneProjection,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id,
                        null,
                        null
                );
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String number = cursor.getString(
                                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        );

                        result = new JsonGenerator()
                                .put("name", name)
                                .put("tel", number)
                                .gen();
                    } while (cursor.moveToNext());
                }

            } while (data.moveToNext());
        }

        Log.d(TAG, "onLoadFinished: result =\n" + result.toString());

        return result;
    }


    @TargetApi(Build.VERSION_CODES.M)
    @OnClick(R.id.btn_get_all_contacts)
    public void onBtnGetAllContactsClicked() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.READ_CONTACTS)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            getContactList();
                        } else {
                            ToastUtils.showShortToast("权限被拒绝");
                        }
                    }

                });
    }

    private void getContactList() {
        final ContactHelper helper = new ContactHelper(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                jsonObject = helper.queryContactList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new AlertDialog.Builder(SystemActivity.this)
                                    .setTitle("通讯录")
                                    .setMessage(jsonObject.toString(4))
                                    .setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton("复制", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                ClipboardUtils.putStringToClipboard(jsonObject.toString(4));
                                                ToastUtils.showShortToast("复制成功");
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            dialog.dismiss();
                                        }
                                    })
                                    .show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();

    }


    @OnClick(R.id.btn_set_clipboard)
    public void onBtnSetClipboardClicked() {
        String content = etContent.getText().toString();
        ClipboardUtils.putStringToClipboard(content);
        ToastUtils.showShortToast("设置成功");
    }

    @OnClick(R.id.btn_get_clipboard)
    public void onBtnGetClipboardClicked() {
        ToastUtils.showShortToast(ClipboardUtils.getStringFromClipboard());
    }

    @OnClick(R.id.btn_request_permission)
    public void onBtnRequestPermissionClicked() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.READ_CONTACTS)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            ToastUtils.showShortToast("成功了");
                        } else {
                            ToastUtils.showShortToast("失败了");
                        }
                    }

                });

    }

    @OnClick(R.id.btn_hide_app_icon)
    public void onHideAppIconClicked() {
        PackageManager manager = getPackageManager();
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        int status = manager.getComponentEnabledSetting(componentName);
        if (status == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            manager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } else {
            manager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
        }

    }

    @OnClick(R.id.btn_load_dex)
    public void onBtnLoadDexClicked() {

        File jarFile = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ktools",
                "hello_world_dex.jar"
        );

        if (!jarFile.exists()) {
            //todo:这里应该从assets中复制到sdcard中
            ToastUtils.showShortToast("文件不存在");
            return;
        }

        DexClassLoader loader = new DexClassLoader(
                jarFile.getAbsolutePath(),
                getExternalCacheDir().getAbsolutePath(),
                null,
                getClassLoader()
        );

        try {

            Class clazz = loader.loadClass("com.jiangkang.ktools.HelloWorld");

            ISayHello iSayHello = (ISayHello) clazz.newInstance();

            ToastUtils.showLongToast("执行dex中方法：" + iSayHello.sayHello());

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }


    }

    @OnClick(R.id.btn_exit_app)
    public void onBtnExitAppClicked() {

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        hideVirtualNavbar(this);
    }


    private void hideVirtualNavbar(Activity activity) {
        View decroView = activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decroView.setSystemUiVisibility(uiOptions);
    }

    private void hideStatusBar() {
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }


    @OnClick(R.id.btn_hide_status_bar)
    public void onBtnHideStatusBarClicked() {
        hideStatusBar();
    }

    @OnClick(R.id.btn_hide_virtual_navbar)
    public void onBtnHideVirtualNavbarClicked() {

        hideVirtualNavbar(this);
    }


    @OnClick(R.id.btn_aidl)
    public void onAIDLClicked() {
        AIDLDemoActivity.launch(this,null);
    }
}
