package com.dcim.platform.module.binterface.ftp;

import lombok.Data;

/**
 * FTP 配置模型
 *
 * B接口中FTP用于文件、图片、日志等能力
 * 当前仅做模型占位，不实现真实FTP连接
 *
 * TODO: 后续实现FTP连接管理
 */
@Data
public class FtpConfigModel {

    private String host;
    private int port;
    private String username;
    private String password;
    private boolean passiveMode;
    private String basePath;
}
