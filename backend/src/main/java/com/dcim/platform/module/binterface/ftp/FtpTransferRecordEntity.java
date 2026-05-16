package com.dcim.platform.module.binterface.ftp;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * FTP 文件/图片传输记录实体
 *
 * 记录通过FTP传输的文件、图片、日志等记录
 *
 * 安全限制：当前仅做数据模型占位，不允许真实连接外部FTP
 */
@Data
@Entity
@Table(name = "ftp_transfer_record")
public class FtpTransferRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 文件名 */
    private String fileName;

    /** 文件类型: image / log / config */
    private String fileType;

    /** 来源FSU编码 */
    private String fsuCode;

    /** 方向: upload / download */
    private String direction;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 传输时间 */
    private LocalDateTime transferTime;

    /** 状态: success / failed */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
