package com.dcim.platform.module.binterface.service.fsu;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * BIF2016-PROXY-001: FSU HTTP 代理配置。
 *
 * <p>绑定 {@code b-interface.fsu-client.proxy} 下的属性。
 * 默认 disabled，不影响无代理环境。</p>
 */
@Component
@ConfigurationProperties(prefix = "b-interface.fsu-client.proxy")
public class FsuClientProxyProperties {

    private boolean enabled = false;
    private String host;
    private int port = -1;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public String getHost() { return host; }
    public void setHost(String v) { this.host = v; }
    public int getPort() { return port; }
    public void setPort(int v) { this.port = v; }

    public boolean isValid() {
        return enabled && host != null && !host.isBlank() && port > 0;
    }
}
