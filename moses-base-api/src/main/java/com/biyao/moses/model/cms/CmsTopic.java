package com.biyao.moses.model.cms;

import java.io.Serializable;

import lombok.Data;

@Data
public class CmsTopic implements Serializable{
	
	private static final long serialVersionUID = -6590922243140685494L;
	private int topicId;
    private String entryImageUrl;
    private String price;
    private String title;
    private String entryWebpImageUrl;
    private String subtitle;
    private int productAggregationId;
} 