package com.mok.infrastructure.util;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpHeaders;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SysUtilTest {

    @Test
    public void testIsSuperAdmin() {
        assertTrue(SysUtil.isSuperAdmin("000000", "root"));
        assertFalse(SysUtil.isSuperAdmin("123456", "root"));
        assertFalse(SysUtil.isSuperAdmin("000000", "admin"));
        assertFalse(SysUtil.isSuperAdmin("123456", "admin"));
    }

    @Test
    public void testIsSuperTenant() {
        assertTrue(SysUtil.isSuperTenant("000000"));
        assertFalse(SysUtil.isSuperTenant("123456"));
    }

    @Test
    public void testGetIpAddressWithXForwardedFor() {
        HttpRequest<?> request = mock(HttpRequest.class);
        MutableHttpHeaders headers = mock(MutableHttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");
        String ipAddress = SysUtil.getIpAddress(request);
        assertEquals("192.168.1.100", ipAddress);
    }

    @Test
    public void testGetIpAddressWithProxyClientIp() {
        HttpRequest<?> request = mock(HttpRequest.class);
        MutableHttpHeaders headers = mock(MutableHttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get("X-Forwarded-For")).thenReturn(null);
        when(headers.get("Proxy-Client-IP")).thenReturn("192.168.1.100");
        String ipAddress = SysUtil.getIpAddress(request);
        assertEquals("192.168.1.100", ipAddress);
    }

    @Test
    public void testGetIpAddressWithWLProxyClientIp() {
        HttpRequest<?> request = mock(HttpRequest.class);
        MutableHttpHeaders headers = mock(MutableHttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get("X-Forwarded-For")).thenReturn(null);
        when(headers.get("Proxy-Client-IP")).thenReturn(null);
        when(headers.get("WL-Proxy-Client-IP")).thenReturn("192.168.1.100");
        String ipAddress = SysUtil.getIpAddress(request);
        assertEquals("192.168.1.100", ipAddress);
    }

    @Test
    public void testGetIpAddressFromRemoteAddress() throws UnknownHostException {
        HttpRequest<?> request = mock(HttpRequest.class);
        MutableHttpHeaders headers = mock(MutableHttpHeaders.class);
        InetSocketAddress remoteAddress = new InetSocketAddress(InetAddress.getByName("192.168.1.100"), 8080);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get("X-Forwarded-For")).thenReturn(null);
        when(headers.get("Proxy-Client-IP")).thenReturn(null);
        when(headers.get("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddress()).thenReturn(remoteAddress);
        String ipAddress = SysUtil.getIpAddress(request);
        assertEquals("192.168.1.100", ipAddress);
    }

    @Test
    public void testGetIpAddressWithUnknown() {
        HttpRequest<?> request = mock(HttpRequest.class);
        MutableHttpHeaders headers = mock(MutableHttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get("X-Forwarded-For")).thenReturn("unknown");
        when(headers.get("Proxy-Client-IP")).thenReturn("unknown");
        when(headers.get("WL-Proxy-Client-IP")).thenReturn("unknown");
        String ipAddress = SysUtil.getIpAddress(request);
        assertNull(ipAddress);
    }

    @Test
    public void testGetIpAddressWithEmpty() {
        HttpRequest<?> request = mock(HttpRequest.class);
        MutableHttpHeaders headers = mock(MutableHttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get("X-Forwarded-For")).thenReturn("");
        when(headers.get("Proxy-Client-IP")).thenReturn("");
        when(headers.get("WL-Proxy-Client-IP")).thenReturn("");
        String ipAddress = SysUtil.getIpAddress(request);
        assertNull(ipAddress);
    }

    @Test
    public void testGetBrowserWithChrome() {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        String browser = SysUtil.getBrowser(userAgent);
        assertEquals("Windows - Chrome", browser);
    }

    @Test
    public void testGetBrowserWithEdge() {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";
        String browser = SysUtil.getBrowser(userAgent);
        assertEquals("Windows - Edge", browser);
    }

    @Test
    public void testGetBrowserWithFirefox() {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:119.0) Gecko/20100101 Firefox/119.0";
        String browser = SysUtil.getBrowser(userAgent);
        assertEquals("Windows - Firefox", browser);
    }

    @Test
    public void testGetBrowserWithSafari() {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15";
        String browser = SysUtil.getBrowser(userAgent);
        assertEquals("macOS - Safari", browser);
    }

    @Test
    public void testGetBrowserWithPostman() {
        String userAgent = "PostmanRuntime/7.37.0";
        String browser = SysUtil.getBrowser(userAgent);
        assertEquals("Unknown OS (Postman)", browser);
    }

    @Test
    public void testGetBrowserWithCurl() {
        String userAgent = "curl/7.88.1";
        String browser = SysUtil.getBrowser(userAgent);
        assertEquals("Unknown OS (cURL)", browser);
    }

    @Test
    public void testGetBrowserWithWeChat() {
        String userAgent = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/87.0.4280.141 Mobile Safari/537.36 MicroMessenger/8.0.32.2320(0x28002030) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64";
        String browser = SysUtil.getBrowser(userAgent);
        assertEquals("Android - WeChat", browser);
    }

    @Test
    public void testGetBrowserWithIos() {
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
        String browser = SysUtil.getBrowser(userAgent);
        assertEquals("iOS - Safari", browser);
    }

    @Test
    public void testGetBrowserWithAndroid() {
        String userAgent = "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
        String browser = SysUtil.getBrowser(userAgent);
        assertEquals("Android - Chrome", browser);
    }

    @Test
    public void testGetBrowserWithLinux() {
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        String browser = SysUtil.getBrowser(userAgent);
        assertEquals("Linux - Chrome", browser);
    }

    @Test
    public void testGetBrowserWithNull() {
        String browser = SysUtil.getBrowser(null);
        assertEquals("Unknown", browser);
    }

    @Test
    public void testGetBrowserWithEmpty() {
        String browser = SysUtil.getBrowser("");
        assertEquals("Unknown", browser);
    }

    @Test
    public void testGetBrowserWithBlank() {
        String browser = SysUtil.getBrowser("   ");
        assertEquals("Unknown", browser);
    }
}