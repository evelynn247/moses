package com.biyao.moses.model.feature;

import java.util.ArrayList;
import java.util.List;

/**
 * 取值只能为0和1的稀疏向量
 * @author guochong
 */
public class Sparse01Vector{

    // 存储值为1的索引位置
    ArrayList<Integer> indices1;

    public Sparse01Vector( ) {
        super();
        this.indices1 = new ArrayList<Integer>();
    }

    public Sparse01Vector( List<Integer> indices1) {
        super();
        this.indices1 = new ArrayList<Integer>(indices1);
    }

    /**
     * 获取 存储值为1的索引位置
     */
    public ArrayList<Integer> getIndices1() {
        return indices1;
    }

    /**
     * 追加存储值为1的索引位置
     */
    public Sparse01Vector add(Integer index) {
        indices1.add(index);
        return this;
    }

    public Sparse01Vector addAll(Sparse01Vector vector) {
        indices1.addAll(vector.getIndices1());
        return this;
    }
}
