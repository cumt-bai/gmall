package com.cumt.gmall.manage.mapper;

import com.cumt.gmall.bean.SpuSaleAttr;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {

    /**
     * 根据spuId查询销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrList(@Param("spuId") String spuId);

    /**
     * 根据spuId和skuId获取销售属性及销售属性值
     * @param spuId
     * @param id
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(String spuId, String skuId);
}
