package com.dcim.platform.module.binterface.service.sc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SCService 服务端接口（/api/b-interface 路径，保留兼容）。
 *
 * 接收 FSU 主动上报的 SOAP/XML 报文（快数据通道）。
 * 处理逻辑委托给 {@link ScServiceProcessor}。
 */
@RestController
@RequestMapping("/api/b-interface")
public class ScServiceController {

    private static final Logger log = LoggerFactory.getLogger(ScServiceController.class);

    private final ScServiceProcessor processor;

    public ScServiceController(ScServiceProcessor processor) {
        this.processor = processor;
    }

    @PostMapping(value = "/sc-service",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = MediaType.TEXT_XML_VALUE)
    public String handleScService(@RequestBody(required = false) String requestBody) {
        log.debug("SCService /api/b-interface 入口收到请求");
        return processor.process(requestBody);
    }
}
