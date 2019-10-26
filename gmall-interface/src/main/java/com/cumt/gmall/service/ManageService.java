package com.cumt.gmall.service;

import com.cumt.gmall.bean.*;

import java.util.List;

public interface ManageService {

    /**
     * 获取一级分类数据
     * @return
     */
    List<BaseCatalog1> getCatalog1();

    /**
     * 根据一级分类Id查询二级分类
     * @param catalog1Id
     * @return
     */
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    /**
     * 根据二级分类Id查询三级分类
     * @param catalog2Id
     * @return
     */
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    /**
     * 根据三级分类Id查询平台属性
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    /**
     * 新增平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据Id获取平台属性
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(String attrId);

    /**
     * 根据attrId获取属性值
     * @return
     */
    List<BaseAttrValue> getAttrValueList(String attrId);

    /**
     * 根据三级分类Id查询spuInfo
     * @param catalog3Id
     * @return
     */
    List<SpuInfo> getSpuInfoList(String catalog3Id);

    /**
     * 获取基本销售属性
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     *  保存
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 获取spuImage列表
     * @return
     */
    List<SpuImage> getSpuImageList(SpuImage spuImage);

    /**
     * 根据spuId查询销售属性及对应的销售属性值，使用两张表关联查询
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    /**
     * 保存sku
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 根据skuId获取SkuInfo(库存单元表)信息
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 根据spuId，skuId获取销售属性及销售属性值并锁定
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 通过spuId查询销售属性值和skuId集合
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    /**
     * 根据平台属性值id集合查询平台属性和平台属性值集合
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
