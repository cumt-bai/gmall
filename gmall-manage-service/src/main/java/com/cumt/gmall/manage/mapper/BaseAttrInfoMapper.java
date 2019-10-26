package com.cumt.gmall.manage.mapper;

import com.cumt.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {

    /**
     * 根据三级分类Id查询销售属性及销售属性值列表
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getBaseAttrInfoListByCatalog3Id(@Param("catalog3Id") String catalog3Id);

    /**
     * 根据平台属性值id集合查询平台属性和平台属性值集合
     * @param valueIds
     * @return
     */
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("valueIds") String valueIds);
}
