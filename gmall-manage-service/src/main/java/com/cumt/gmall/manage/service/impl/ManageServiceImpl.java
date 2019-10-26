package com.cumt.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.cumt.gmall.bean.*;
import com.cumt.gmall.config.RedisUtil;
import com.cumt.gmall.manage.constant.ManageConst;
import com.cumt.gmall.manage.mapper.*;
import com.cumt.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {

        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {

        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {

        //BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        //baseAttrInfo.setCatalog3Id(catalog3Id);
        //return baseAttrInfoMapper.select(baseAttrInfo);
        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);
    }


    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        //修改操作
        if(baseAttrInfo.getId() != null && baseAttrInfo.getId().length()>0){
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        }else {
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        //先删除再添加  一种修改的方式
        BaseAttrValue baseAttrValueDel = new BaseAttrValue();
        baseAttrValueDel.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueDel);

        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if(attrValueList != null && attrValueList.size() > 0){

            for (BaseAttrValue baseAttrValue : attrValueList) {

                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(baseAttrValue);

            }

        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {

        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        return baseAttrValueMapper.select(baseAttrValue);
    }


    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {

        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuInfoList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {

        //更新
        if(spuInfo.getId()!=null && spuInfo.getId().length() > 0 ){
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        } else {
            spuInfoMapper.insertSelective(spuInfo);
        }

        // 保存图片
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0){
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }

        // 保存销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if(spuSaleAttrList != null && spuSaleAttrList.size() > 0){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                //保存销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if(spuSaleAttrValueList!=null && spuSaleAttrValueList.size()>0){

                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);

                    }
                }

            }
        }

    }

    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {

        // 通用mapper没有实现多表关联查询机制
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {

        skuInfoMapper.insertSelective(skuInfo);

        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() >0){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size() > 0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);

            }
        }

    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {

        // 加分布式锁的两种方式
        return getSkuInfoRedisSet(skuId);

        //return getSkuInfoRedisson(skuId);

        /*try {
            jedis = redisUtil.getJedis();
            //redis 存储的五种类型：String List Set Hash ZSet
            //此时存储对象可以使用Hash，但是此处仅展示不需要修改，所以用String比较方便
            String key = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            if (jedis.exists(key)){
                // 缓存命中走缓存
                String skuInfoJson = jedis.get(key);
                if(skuInfoJson != null && skuInfoJson.length() > 0){
                    skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                }
                return  skuInfo;
            } else {
                // 没有则走数据库
                skuInfo = getSkuInfoDB(skuId);
                // 放入redis缓存中
                jedis.setex(key,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }*/



    }

    private SkuInfo getSkuInfoRedisson(String skuId) {
        SkuInfo skuInfo = null;
        Jedis jedis = null;
        RLock lock = null;

        try {
            jedis = redisUtil.getJedis();
            String key = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            String skuJson = jedis.get(key);

            if(StringUtils.isEmpty(skuJson)){

                Config config = new Config();
                config.useSingleServer().setAddress("redis://192.168.12.110:6379");
                RedissonClient redisson = Redisson.create(config);
                lock = redisson.getLock("my-lock");
                // 缓存为空，获取数据库中的数据时上锁
                lock.lock();
                // 获取数据库中的数据
                skuInfo = getSkuInfoDB(skuId);
                // 放入redis缓存中
                jedis.setex(key,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                return skuInfo;
            } else {
                // 获取缓存中的数据
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                return skuInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis != null){
                jedis.close();
            }
            if(lock != null){
                lock.unlock();
            }
        }

        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedisSet(String skuId) {
        SkuInfo skuInfo = null;
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();
            String key = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            String skuJson = jedis.get(key);
            if(skuJson == null){// 没有数据 ,需要加锁！取出完数据，还要放入缓存中，下次直接从缓存中取得即可！
                // 定义key user:userId:lock
                String lockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
                // 生成锁
                String lockKeyResult = jedis.set(lockKey,"cumt","NX","PX",ManageConst.SKULOCK_EXPIRE_PX);
                if("OK".equals(lockKeyResult)){
                    // 获取分布式锁
                    System.out.println("获取分布式锁");
                    skuInfo = getSkuInfoDB(skuId);
                    // 放入redis缓存中
                    jedis.setex(key,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                    //删除锁
                    jedis.del(lockKeyResult);
                    return skuInfo;
                } else {
                    Thread.sleep(ManageConst.SKULOCK_EXPIRE_PX);
                    //调用方法再查询数据
                    return getSkuInfo(skuId);
                }
            } else {
                skuInfo = JSON.parseObject(skuJson,SkuInfo.class);
                return skuInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);

        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);

        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValueList);

        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {

        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getSpuId(),skuInfo.getId());

    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {

        String valueIds = StringUtils.join(attrValueIdList.toArray(), ",");

        return baseAttrInfoMapper.selectAttrInfoListByIds(valueIds);
    }
}
