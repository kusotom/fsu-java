package com.dcim.platform.module.binterface.service.fsu;

/**
 * FSU 服务客户端接口。
 *
 * <p>SC 向 FSU (FSUService) 发起 SOAP 调用的抽象层。
 * 支持 Stub/Mock 和真实 HTTP 两种实现，由配置开关控制。</p>
 *
 * <p>职责边界：</p>
 * <ul>
 *   <li>只做 SOAP 调用抽象，不处理业务逻辑</li>
 *   <li>不处理 FSU 地址发现（由调用方提供 serviceUrl）</li>
 *   <li>不处理调用策略（重试/超时由调用方或配置决定）</li>
 *   <li>返回结构化结果，不抛 HTTP 异常</li>
 * </ul>
 *
 * @see StubFsuServiceClient
 * @see RealHttpFsuServiceClient
 * @see FsuServiceRequest
 * @see FsuServiceResponse
 */
public interface FsuServiceClient {

    /**
     * 向 FSU 发起 SOAP 调用。
     *
     * @param request 调用请求（fsuCode / pkType / infoXml / xmlDataXml）
     * @return 结构化响应，不会返回 null
     */
    FsuServiceResponse call(FsuServiceRequest request);
}
