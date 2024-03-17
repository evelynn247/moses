package com.biyao.search.ui.model;

public class CmsTopic {
    private int topicId;
    private String entryImageUrl;
    private String price;
    private String title;
    private String entryWebpImageUrl;
    private String subtitle;
    private int productAggregationId;

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public String getEntryImageUrl() {
        return entryImageUrl;
    }

    public void setEntryImageUrl(String entryImageUrl) {
        this.entryImageUrl = entryImageUrl;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEntryWebpImageUrl() {
        return entryWebpImageUrl;
    }

    public void setEntryWebpImageUrl(String entryWebpImageUrl) {
        this.entryWebpImageUrl = entryWebpImageUrl;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public int getProductAggregationId() {
        return productAggregationId;
    }

    public void setProductAggregationId(int productAggregationId) {
        this.productAggregationId = productAggregationId;
    }
}
