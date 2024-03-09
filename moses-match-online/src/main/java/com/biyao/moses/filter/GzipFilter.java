package com.biyao.moses.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.biyao.moses.wrapper.GzipRequestWrapper;
import com.biyao.moses.wrapper.GzipResponsetWrapper;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class GzipFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		boolean isSupportGzip = responsetWrapper(request);

		GzipResponsetWrapper responsetWrapper = null;
		if(isSupportGzip) {

			responsetWrapper = new GzipResponsetWrapper(response);
		}

		filterChain.doFilter(requestWrapper(request), (isSupportGzip ? responsetWrapper : response));

		if(isSupportGzip) {

			ByteArrayOutputStream bout = null;
			GZIPOutputStream gzipOut = null;
			try {
				// 获取缓存的响应数据
				byte[] bytes = responsetWrapper.getBytes();

				bout = new ByteArrayOutputStream();
				// 创建 GZIPOutputStream 对象
				gzipOut = new GZIPOutputStream(bout);

				// 将响应的数据写到 Gzip 压缩流中
				gzipOut.write(bytes);
				gzipOut.flush();
				gzipOut.close();

				byte[] bts = bout.toByteArray();

				response.setContentLength(bts.length);
				response.setHeader("Content-Encoding", "gzip");
				response.getOutputStream().write(bts);
			}catch (Exception e) {
				log.error("[严重异常]gzip output stream error",e);
			}
		}

	}

	private boolean responsetWrapper(HttpServletRequest request) {
		String acceptEncoding = request.getHeader("Accept-Encoding");
		if (null != acceptEncoding && acceptEncoding.indexOf("gzip") != -1) {
			return true;
		}
		return false;
	}

	private HttpServletRequest requestWrapper(HttpServletRequest request) {
		String contentEncoding = request.getHeader("Content-Encoding");
		if (null != contentEncoding && contentEncoding.indexOf("gzip") != -1) {
			request = new GzipRequestWrapper(request);
		}
		return request;
	}
}