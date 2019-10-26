package com.cumt.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.cumt.gmall.bean.SkuLsInfo;
import com.cumt.gmall.bean.SkuLsParams;
import com.cumt.gmall.bean.SkuLsResult;
import com.cumt.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {

        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        /*
            定义dsl语句
            定义执行动作
            执行动作并返回结果集
         */

        String query = makeQueryStringForSearch(skuLsParams);

        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams,searchResult);

        return skuLsResult;
    }

    /**
     * 自定义dsl语句
     * @param skuLsParams
     * @return
     */
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {

        //创建查询构造器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //构建query方法
        //{bool}  组合查询 ：bool把各种其它查询通过must（与）、must_not（非）、should（或）的方式进行组合
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        // 全文检索查询
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0){

            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);

            //设置高亮
            //HighlightBuilder highlightBuilder = new HighlightBuilder();
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();
            //设置高亮字段
            highlighter.field("skuName");
            highlighter.postTags("</span>");
            highlighter.preTags("<span style=color:red>");
            //将高亮结果放入查询器中
            searchSourceBuilder.highlight(highlighter);

        }

        // 三级分类过滤查询
        if (skuLsParams.getCatalog3Id() != null){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }

        // 平台属性值过滤
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0){

            for (String valueId : skuLsParams.getValueId()) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }

        }

        // 调用query方法
        searchSourceBuilder.query(boolQueryBuilder);

        // 排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        //分页
        searchSourceBuilder.from((skuLsParams.getPageNo()-1)*skuLsParams.getPageSize());
        searchSourceBuilder.size(skuLsParams.getPageSize());

        //聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        String query = searchSourceBuilder.toString();
        System.out.println("query =" + query);
        return query;
    }

    /**
     * 返回结果集
     * @param skuLsParams
     * @param searchResult
     * @return
     */
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();

        List<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo skuLsInfo = hit.source;

            if( hit.highlight !=null && hit.highlight.size() > 0){
                Map<String, List<String>> highlight = hit.highlight;
                List<String> skuNameList = highlight.get("skuName");
                String skuName = skuNameList.get(0);
                skuLsInfo.setSkuName(skuName);
            }

            skuLsInfoList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoList);


        skuLsResult.setTotal(searchResult.getTotal());


        long totalPage = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPage);

        //获取平台属性值集合 通过聚合获取
        List<String> attrValueIdList = new ArrayList<>();

        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        for (TermsAggregation.Entry bucket : buckets) {

            String key = bucket.getKey();
            attrValueIdList.add(key);

        }

        skuLsResult.setAttrValueIdList(attrValueIdList);

        return skuLsResult;
    }
}
















