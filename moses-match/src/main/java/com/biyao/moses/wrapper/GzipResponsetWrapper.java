package com.biyao.moses.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GzipResponsetWrapper extends HttpServletResponseWrapper {

	private PrintWriter pwrite;
	private ByteArrayOutputStream bytes = new ByteArrayOutputStream();

	public GzipResponsetWrapper(HttpServletResponse response) {
		super(response);
	}

	  @Override
	    public ServletOutputStream getOutputStream() throws IOException {
	        return new GzipServletOutputStream(bytes); 
	    }
 
	    @Override
	    public PrintWriter getWriter() throws IOException {
	        try{
	            pwrite = new PrintWriter(new OutputStreamWriter(bytes, "utf-8"));
	        } catch(UnsupportedEncodingException e) {
	            log.error("将响应数据缓存在 PrintWriter异常",e);
	        }
	        return pwrite;
	    }

	    /**
	     * 获取缓存在 PrintWriter 中的响应数据
	     * @return
	     */
	    public byte[] getBytes() {
	        if(null != pwrite) {
	            pwrite.close();
	            return bytes.toByteArray();
	        }

	        if(null != bytes) {
	            try {
	                bytes.flush();
	            } catch(IOException e) {
	                e.printStackTrace();
	            }
	        }
	        return bytes.toByteArray();
	    }

	    class GzipServletOutputStream extends ServletOutputStream {
	        private ByteArrayOutputStream ostream ;

	        public GzipServletOutputStream(ByteArrayOutputStream ostream) {
	            this.ostream = ostream;
	        }

	        @Override
	        public void write(int b) throws IOException {
	            ostream.write(b); 
	        }

	    } 
}
