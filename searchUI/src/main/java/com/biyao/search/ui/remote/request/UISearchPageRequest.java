package com.biyao.search.ui.remote.request;

import javax.ws.rs.QueryParam;

import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.ui.util.IPUtil;
import com.google.common.base.Strings;

public class UISearchPageRequest extends UIBaseRequest{
	@QueryParam("sid")
	private String sid;
	
	@QueryParam("bkId")
	private String blockId;
	
	@QueryParam("pidx")
	private Integer pageIndex;

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getBlockId() {
		return blockId;
	}

	public void setBlockId(String blockId) {
		this.blockId = blockId;
	}

	public Integer getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(Integer pageIndex) {
		this.pageIndex = pageIndex;
	}

	@Override
	public void preHandleParam() {
		super.preHandleParam();
	}
}
