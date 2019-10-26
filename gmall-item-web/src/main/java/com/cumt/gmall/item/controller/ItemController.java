package com.cumt.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.cumt.gmall.bean.SkuInfo;
import com.cumt.gmall.bean.SkuSaleAttrValue;
import com.cumt.gmall.bean.SpuSaleAttr;
import com.cumt.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;


    @RequestMapping("{skuId}.html")
    public String index(@PathVariable String skuId, HttpServletRequest request){

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //System.out.println(skuInfo.toString());

        //获取销售属性及销售属性值，并获取锁定数据
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);

        List<SkuSaleAttrValue> skuSaleAttrValueList =  manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        //{"119|121":"33","119|122":"34","120|123":"35"}
        String key = "";
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);

            //当key值存在则拼接"|"
            if(key.length() != 0){
                key += "|";
            }
            //拼接
            key += skuSaleAttrValue.getSaleAttrValueId();
            // 当skuId改变或者最后则停止拼接，并存入map中
            if((i+1)==skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())){
                map.put(key,skuSaleAttrValue.getSkuId());
                key = "";
            }
        }
        System.out.println(key);

        String skuJson = JSON.toJSONString(map);
        request.setAttribute("skuJson",skuJson);

        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);

        return "item";
    }



}
