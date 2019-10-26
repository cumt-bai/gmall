package com.cumt.gmall.service;

import com.cumt.gmall.bean.SkuLsInfo;
import com.cumt.gmall.bean.SkuLsParams;
import com.cumt.gmall.bean.SkuLsResult;

public interface ListService {

    /**
     * 保存es
     * @param skuLsInfo
     */
    void saveSkuInfo(SkuLsInfo skuLsInfo);

    /**
     * es查询
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);
}
