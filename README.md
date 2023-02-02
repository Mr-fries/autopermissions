# 依赖使用说明

## 一、前言

本库用于自动通过权限申请，使用前请仔细阅读文档，以免程序陷入不可逆的死循环。

## 二、部署步骤

### 1.导入库文件到项目

在项目中引入库文件，以AndroidStudio为例：

1. 把autopermissions.aar包迁移至项目工程目录lib内
2. 打开 File - Project Structure - Dependencies
3. 选择需要引入库文件的包，点击加号
4. 选择添加AAR包，输入相应路径
5. 点击确认，完成库文件导入

### 2.调起AutoAccessibilityService服务

``` java
// 1. (必要)定义并拿到APP包名字符串
String packageName = XX.getPackageName();
// 2. (必要)初始化服务，引入包名
AutoAccessibilityService.initAuto(packageName);
// 3. (必要)定义 服务类启动intent 并传入APP包名
Intent intent = new Intent(this, AutoAccessibilityService.class);
intentAuto.putExtra(AutoAccessibilityService.APP_PKG_TAG, BaseApp.get().getPackageName());
// 3.1 因系统差异，当普通权限申请窗口不隶属"com.android.packageinstaller"包名下时，通过携带NORMAL_SETTING_PKG_TAG更改默认普通权限窗口隶属包名
intentAuto.putExtra(AutoAccessibilityService.NORMAL_SETTING_PKG_TAG, BaseApp.get().getPackageName());
// 3.2 因系统差异，当特殊权限申请窗口不隶属"com.android.settings"包名下时，通过携带SPECIAL_SETTING_PKG_TAG更改默认特殊权限窗口隶属包名
intentAuto.putExtra(AutoAccessibilityService.SPECIAL_SETTING_PKG_TAG, BaseApp.get().getPackageName());
// 3.3 因性能差异，当出现无点击反馈现象时，通过调整点击延迟时长来解决这一现象 (默认500(ms))
intentAuto.putExtra(AutoAccessibilityService.CLICK_DELAY_TAG, 500);
// 4. (必要)启动服务
startService(intent);
// 5. 无需手动结束服务，当所有权限申请完毕后，无论申请结果如何，服务将自行结束其生命周期
// 6. 请于服务启动后进行权限申请的操作
```

## 三、注意事项

### 1.关于特殊权限和普通权限

普通权限的申请时的顶部`Activity`包名为`com.android.packageinstaller`，而特殊权限申请时会跳转至系统设置页面，其`Activity`包名为`com.android.settings`，两个Activity包名如果根据系统不同，可在`AutoAccessibilityService`类中修改

## 四、FAQ

### 1. 当权限申请页面被调起后，无点击事件反馈

通过带有`AutoAccessibilityService`标签的日志以确认：

1. APP包名是否引入正确；
2. 服务是否正常启动；
3. 服务是否被异常结束；
4. 点击事件延迟是否适应当前设备性能。

