package com.cumt.gmall.manage.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.cumt.gmall.bean.SkuInfo;
import com.cumt.gmall.bean.SkuLsInfo;
import com.cumt.gmall.bean.SpuImage;
import com.cumt.gmall.bean.SpuSaleAttr;
import com.cumt.gmall.service.ListService;
import com.cumt.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    //http://localhost:8082/spuSaleAttrList?spuId=58
    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId){
        return manageService.getSpuSaleAttrList(spuId);
    }


    //http://localhost:8082/spuImageList?spuId=58
    @RequestMapping("spuImageList")
    public List<SpuImage> getSpuImageList(SpuImage spuImage){
        return manageService.getSpuImageList(spuImage);
    }

    //http://localhost:8082/saveSkuInfo
    @RequestMapping("saveSkuInfo")
    @ResponseBody
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
    }

    @RequestMapping("onSave")
    public SkuLsInfo onSave(String skuId){

        SkuLsInfo skuLsInfo = new SkuLsInfo();

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        BeanUtils.copyProperties(skuInfo,skuLsInfo);

        listService.saveSkuInfo(skuLsInfo);
        return  skuLsInfo;
    }

}
