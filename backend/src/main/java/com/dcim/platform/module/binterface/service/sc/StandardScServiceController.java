package com.dcim.platform.module.binterface.service.sc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B接口标准 SCService 入口（/services/SCService）。
 *
 * 对应 B接口 2016 WSDL 声明的标准路径。
 * FSU gSOAP 客户端以此路径发起被动上报。
 *
 * 处理逻辑与 {@link ScServiceController} 完全一致，委托给 {@link ScServiceProcessor}。
 */
@RestController
@RequestMapping("/services")
public class StandardScServiceController {

    private static final Logger log = LoggerFactory.getLogger(StandardScServiceController.class);

    private final ScServiceProcessor processor;

    public StandardScServiceController(ScServiceProcessor processor) {
        this.processor = processor;
    }

    @PostMapping(value = "/SCService",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = MediaType.TEXT_XML_VALUE)
    public String handleScService(@RequestBody(required = false) String requestBody) {
        log.debug("SCService /services 标准入口收到请求");
        return processor.process(requestBody);
    }
}
