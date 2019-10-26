package com.cumt.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuLsInfo implements Serializable {

    String id;

    String spuId;

    BigDecimal price;

    String skuName;

    String catalog3Id;

    String skuDefaultImg;

    // 热度排名
    Long hotScore=0L;

    List<SkuLsAttrValue> skuAttrValueList;
}
