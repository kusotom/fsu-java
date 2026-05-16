package com.dcim.platform.module.binterface;

import com.dcim.platform.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/b-interface")
public class BInterfaceHealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "bInterfaceVersion", "B接口 2016 (试行 V1.0)",
                "scServiceStatus", "ACTIVE",
                "fsuServiceStatus", "STUB",
                "soapEnabled", true,
                "wsdlEnabled", true
        ));
    }
}
