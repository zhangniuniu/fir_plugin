package com.pgy.plugin

import org.gradle.api.Project

public class PgyUploadExtension {
    String api_token
    String bundle_id
    String app_name
    String app_icon
    String change_log
    String app_version
    String version_code

    PgyUploadExtension() {
        api_token = ""
        bundle_id = ""
        app_name = ""
        app_icon = ""
        change_log = ""
        app_version = ""
        version_code = ""
    }

    public static PgyUploadExtension getConfig(Project project) {
        PgyUploadExtension config =
                project.getExtensions().findByType(PgyUploadExtension.class)
        if (config == null) {
            config = new PgyUploadExtension()
        }
        return config
    }
}
