package com.pgy.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.MultipartBuilder
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.Response
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.json.JSONObject

import java.util.concurrent.TimeUnit

public class PgyUploadPlugin implements Plugin<Project> {


    void apply(Project project) {
        project.extensions.create("fir", PgyUploadExtension)

        project.afterEvaluate {
            PgyUploadExtension pgyExt = project.extensions.findByName("fir") as PgyUploadExtension

            if (!project.plugins.hasPlugin("com.android.application")) {
                throw new RuntimeException("FirUploadPlugin can only be applied for android application module.")
            }

            project.android.applicationVariants.each { variant ->
                injectFirTask(project, variant, pgyExt)
            }
        }
    }

    void injectFirTask(Project project, ApplicationVariant variant, PgyUploadExtension config) {
        String name = variant.name
        String newName = name.substring(0, 1).toUpperCase() + name.substring(1)
        def pgyTask = project.tasks.create(name: "fir${newName}") << {
            // 获取要上传的APK
            File apk = variant.outputs.last().outputFile
            config.bundle_id = variant.applicationId
            config.app_version = variant.getVersionName()
            config.version_code = variant.getVersionCode();
            upload(apk, config)
        }

        pgyTask.group = "fir"

        pgyTask.dependsOn project.tasks.getByPath("assemble${newName}")
    }

    void upload(File apkFile, PgyUploadExtension config) {
        JSONObject resultJson = httpPostGetBintry(config)
        println("get_uploan_token_json:" + resultJson)
        handleGetTokenJson(resultJson, apkFile, config)
    }

    private void handleGetTokenJson(JSONObject resultJson, File apkFile, PgyUploadExtension config) {
        if (!resultJson.toString().isEmpty()) {
            //上传apk文件相关信息
            JSONObject certJsonObject = resultJson.getJSONObject("cert")
            JSONObject binaryJsonObject = certJsonObject.getJSONObject("binary")
            String binaryKey = binaryJsonObject.getString("key")
            String binaryToken = binaryJsonObject.getString("token")
            String binaryUploadUrl = binaryJsonObject.getString("upload_url")
            httpPostApkFile(apkFile, config, binaryKey, binaryToken, binaryUploadUrl)
            //上传应用icon
            JSONObject iconJsonObject = certJsonObject.getJSONObject("icon")
            String iconKey = iconJsonObject.getString("key")
            String iconToken = iconJsonObject.getString("token")
            String iconUploadUrl = iconJsonObject.getString("upload_url")
            println("app_icon_path:" + config.app_icon)
            httpPostIconFile(new File(config.app_icon), config, iconKey, iconToken, iconUploadUrl)
        } else {
            throw new GradleException("up load fail: result=${json.toString()}")
        }
    }

    private JSONObject httpPostApkFile(File apkFile, PgyUploadExtension config, String key, String token, String uploadUrl) {
        OkHttpClient client = new OkHttpClient()
        client.setConnectTimeout(10, TimeUnit.SECONDS)
        client.setReadTimeout(60, TimeUnit.SECONDS)

        MultipartBuilder multipartBuilder = new MultipartBuilder()
                .type(MultipartBuilder.FORM)


        multipartBuilder.addFormDataPart("key", key)
        multipartBuilder.addFormDataPart("token", token)
        multipartBuilder.addFormDataPart("x:name", config.app_name)
        multipartBuilder.addFormDataPart("x:version", config.app_version)
        multipartBuilder.addFormDataPart("x:changelog", config.change_log)
        multipartBuilder.addFormDataPart("x:build", config.version_code)

        multipartBuilder.addFormDataPart("file",
                apkFile.getName(), RequestBody.create(MediaType.parse("application/vnd.android.package-archive"),
                apkFile)
        )


        Request request = new Request.Builder().url(uploadUrl).
                post(multipartBuilder.build()).
                build()

        Response response = client.newCall(request).execute()

        if (response == null || response.body() == null) return null
        InputStream is = response.body().byteStream()
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        JSONObject json = new JSONObject(reader.readLine())
        is.close()
        println("post_apk_file:" + json)
        return json
    }

    private JSONObject httpPostIconFile(File iconFile, PgyUploadExtension config, String key, String token, String uploadUrl) {
        OkHttpClient client = new OkHttpClient()
        client.setConnectTimeout(10, TimeUnit.SECONDS)
        client.setReadTimeout(60, TimeUnit.SECONDS)

        MultipartBuilder multipartBuilder = new MultipartBuilder()
                .type(MultipartBuilder.FORM)


        multipartBuilder.addFormDataPart("key", key)
        multipartBuilder.addFormDataPart("token", token)
        multipartBuilder.addFormDataPart("file",
                iconFile.getName(), RequestBody.create(MediaType.parse("application/vnd.android.package-archive"),
                iconFile)
        )


        Request request = new Request.Builder().url(uploadUrl).
                post(multipartBuilder.build()).
                build()

        Response response = client.newCall(request).execute()

        if (response == null || response.body() == null) return null
        InputStream is = response.body().byteStream()
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        JSONObject json = new JSONObject(reader.readLine())
        is.close()
        println("post_icon_file:" + json)
        return json
    }

    private JSONObject httpPostGetBintry(PgyUploadExtension config) {
        OkHttpClient client = new OkHttpClient()
        client.setConnectTimeout(10, TimeUnit.SECONDS)
        client.setReadTimeout(60, TimeUnit.SECONDS)

        MultipartBuilder multipartBuilder = new MultipartBuilder()
                .type(MultipartBuilder.FORM)

        multipartBuilder.addFormDataPart("api_token", config.api_token)
        multipartBuilder.addFormDataPart("bundle_id", config.bundle_id)
        multipartBuilder.addFormDataPart("type", "android")

        Request request = new Request.Builder().url("http://api.fir.im/apps").
                post(multipartBuilder.build()).
                build()

        Response response = client.newCall(request).execute()

        if (response == null || response.body() == null) return null
        InputStream is = response.body().byteStream()
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        JSONObject json = new JSONObject(reader.readLine())
        is.close()

        return json
    }

}
