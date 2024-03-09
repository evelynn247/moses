package com.biyao.moses.wrapper;

import lombok.extern.slf4j.Slf4j;


import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

@Slf4j
public class GzipRequestWrapper extends HttpServletRequestWrapper {
	private HttpServletRequest request;
 
	public GzipRequestWrapper(HttpServletRequest request) {
		super(request);
		this.request = request;
	}
 
	@Override
	public ServletInputStream getInputStream() throws IOException {
		ServletInputStream stream = request.getInputStream();
		String contentEncoding = request.getHeader("Content-Encoding");
		// 如果对内容进行了压缩，则解压
		if (null != contentEncoding && contentEncoding.indexOf("gzip") != -1) {
			try {
				final GZIPInputStream gzipInputStream = new GZIPInputStream(
						stream);
 
				ServletInputStream newStream = new ServletInputStream() {

					@Override
					public boolean isFinished() {
						return false;
					}

					@Override
					public boolean isReady() {
						return false;
					}

					@Override
					public void setReadListener(ReadListener readListener) {

					}

					@Override
					public int read() throws IOException {
						return gzipInputStream.read();
					}
				};
				return newStream;
			} catch (Exception e) {
				log.error("[严重异常]ungzip content fail.", e);
			}
		}
		return stream;
	}
 
}