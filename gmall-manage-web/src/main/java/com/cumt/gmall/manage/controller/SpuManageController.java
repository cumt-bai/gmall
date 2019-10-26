package com.cumt.gmall.manage.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.cumt.gmall.bean.BaseSaleAttr;
import com.cumt.gmall.bean.SpuInfo;
import com.cumt.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@CrossOrigin
public class SpuManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("spuList")
    @ResponseBody
    public List<SpuInfo> spuList(String catalog3Id){

        return manageService.getSpuInfoList(catalog3Id);

    }

    //http://localhost:8082/baseSaleAttrList
    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr> baseSaleAttrList(){

        return manageService.getBaseSaleAttrList();
    }

    //http://localhost:8082/saveSpuInfo
    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo){

        manageService.saveSpuInfo(spuInfo);

    }


}
