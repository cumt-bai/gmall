package com.cumt.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.cumt.gmall.bean.*;
import com.cumt.gmall.service.ListService;
import com.cumt.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request){

        //设置分页
        skuLsParams.setPageSize(2);

        //es 查询
        SkuLsResult skuLsResult = listService.search(skuLsParams);

        //获取结果集中的商品集合
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        request.setAttribute("skuLsInfoList",skuLsInfoList);

        //获取结果集中的平台属性值id的集合
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        //通过平台属性值id的集合查询出平台属性和平台属性值的集合
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);
        //System.out.println(baseAttrInfoList);

        //构造面包屑集合用于存放面包屑
        List<BaseAttrValue> baseAttrValueList = new ArrayList<>();

        //获取查询条件参数
        String urlParam = makeUrlParam(skuLsParams);

        //集合在循环比较中删除数据
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo = iterator.next();

            //获取平台属性值集合
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //判断
                if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0){
                    for (String valueId : skuLsParams.getValueId()) {
                        if (baseAttrValue.getId().equals(valueId)){

                            iterator.remove();

                            //添加面包屑
                            BaseAttrValue baseAttrValueed = new BaseAttrValue();
                            baseAttrValueed.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());

                            // 在这里调用 makeUrlParam  去除重复数据
                            String newUrlParam = makeUrlParam(skuLsParams, valueId);
                            // 将最新的参数条件付给baseAttrValueed 对象
                            baseAttrValueed.setUrlParam(newUrlParam);

                            baseAttrValueList.add(baseAttrValueed);

                        }
                    }
                }
            }
        }

        request.setAttribute("totalPages",skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());

        request.setAttribute("baseAttrValueList",baseAttrValueList);

        request.setAttribute("keyword",skuLsParams.getKeyword());

        request.setAttribute("baseAttrInfoList",baseAttrInfoList);

        request.setAttribute("urlParam",urlParam);

        return "list";
    }

    /**
     * 制作拼接的参数条件
     * @param skuLsParams 获取用户从首页输入的查询条件
     * @param excludeValueIds 获取用户点击面包屑中的valueId
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams, String... excludeValueIds) { //

        String urlParam="";

        if(skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() >0){
            //list.html?keyword=小米
            urlParam += "keyword=" + skuLsParams.getKeyword();
        }

        if(skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0){
            //list.html?catalog3Id=61
            urlParam += "catalog3Id=" + skuLsParams.getCatalog3Id();
        }

        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0){

            //list.html?catalog3Id=61&valueId=13
            for (String valueId : skuLsParams.getValueId()) {

                // 删除面包屑的参数
                if(excludeValueIds != null && excludeValueIds.length > 0){
                    String excludeValueId = excludeValueIds[0];
                    if(excludeValueId.equals(valueId)){
                        continue;
                    }
                }


                if(urlParam.length() > 0){
                    urlParam += "&";
                }

                urlParam += "valueId=" + valueId;

            }

        }

        return urlParam;

    }
}
