# fir_plugin
由于蒲公英上传app有有效期，改用fir，在之前写的pgy的插件上修改的

## gradle配置
```gradle
fir {
    //必填 上传 fir.im apk 字段，否则无法上传 APP 到 fir.im
    api_token 'fir上token'
    change_log '上传更新描述'
    app_name '应用名称'
    app_icon ’应用图标的绝对路径‘
}
```
