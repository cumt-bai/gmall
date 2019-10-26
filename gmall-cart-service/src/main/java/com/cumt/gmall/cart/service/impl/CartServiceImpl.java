package com.cumt.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.cumt.gmall.bean.CartInfo;
import com.cumt.gmall.bean.SkuInfo;
import com.cumt.gmall.cart.constant.CartConst;
import com.cumt.gmall.cart.mapper.CartInfoMapper;
import com.cumt.gmall.config.RedisUtil;
import com.cumt.gmall.service.CartService;
import com.cumt.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {

        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfo);

        if (cartInfoExist != null){
            // 数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            // 更新价格
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            // 再更新数据库
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);

        } else {

            CartInfo cartInfo1 = new CartInfo();
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);

            cartInfo1.setSkuId(skuInfo.getId());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfoMapper.insertSelective(cartInfo1);

            // 这里都需要添加进缓存，因为当进入else时，表明cartInfoExist为空，此时再将cartInfo1赋值给cartInfoExist
            cartInfoExist = cartInfo1;

        }

        // 存入缓存 使mysql与redis同步
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        jedis.hset(userCartKey, skuId, JSON.toJSONString(cartInfoExist));

        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {

        List<CartInfo> cartInfoList = new ArrayList<>();

        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        List<String> cartInfoJsonList = jedis.hvals(userCartKey);
        if (cartInfoJsonList != null){
            for (String cartInfoJson : cartInfoJsonList) {
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }

            // 对集合进行排序，按照更新时间进行排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });

            return cartInfoList;

        } else {

            cartInfoList = loadCartCache(userId);

            return cartInfoList;
        }
    }

    // 查询数据库信息并放入缓存
    public List<CartInfo> loadCartCache(String userId) {

        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);

        if(cartInfoList == null && cartInfoList.size() == 0){
            return null;
        }

        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        //放入redis缓存
        for (CartInfo cartInfo : cartInfoList) {
            jedis.hset(userCartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        jedis.close();
        return cartInfoList;
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {

        List<CartInfo> cartListDB = cartInfoMapper.selectCartListWithCurPrice(userId);

        for (CartInfo  cartInfoCK : cartListCK) {

            boolean isMatch = false;
            for (CartInfo cartInfoDB : cartListDB) {

                if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())){

                    cartInfoDB.setSkuNum(cartInfoDB.getSkuNum() + cartInfoCK.getSkuNum());
                    //更新数据库
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch = true;
                }
            }

            if(!isMatch){
                //设置userId
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }

        }

        // 汇总合并后的数据
        List<CartInfo> cartInfoList = loadCartCache(userId);


        //勾选合并 以未登陆的数据为准
        for (CartInfo cartInfoDB : cartInfoList) {
            for (CartInfo cartInfoCK : cartListCK) {
                if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())){
                    if ("1".equals(cartInfoCK.getIsChecked())){
                        // 更改数据库的状态
                        cartInfoDB.setIsChecked(cartInfoCK.getIsChecked());

                        // 更改缓存的状态
                        checkCart(userId,"1",cartInfoCK.getSkuId());
                    }
                }
            }

        }


        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, String isChecked, String skuId) {

        // 获取购物车的一条数据修改其选中状态，并放回缓存
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = jedis.hget(userCartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfo));

        // 当点击结算时，是选择所有选中的商品。即将所有选中的商品存储在集合中，供下次使用
        String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;

        if ("1".equals(isChecked)){

            jedis.hset(cartCheckedKey,skuId,JSON.toJSONString(cartInfo));
        } else {

            jedis.hdel(cartCheckedKey,skuId);
        }

        jedis.close();


    }

    @Override
    public void addToCartRedis(String skuId, String uuid, int skuNum) {

        /*
        先获取redis中的数据
        判断uuid是否相等
        true  更改数量
        false 新增
         */

        String userCartKey = CartConst.USER_KEY_PREFIX + uuid + CartConst.USER_CART_KEY_SUFFIX;

        Jedis jedis = redisUtil.getJedis();

        List<CartInfo> cartInfoList = new ArrayList<>();
        List<String> cartInfoJsonList = jedis.hvals(userCartKey);
        if (cartInfoJsonList != null && cartInfoJsonList.size() > 0){

            for (String cartInfoJson : cartInfoJsonList) {
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
        }

        boolean isMatch = false;
        for (CartInfo cartInfo : cartInfoList) {
            if (cartInfo.getSkuId().equals(skuId)){
                //数量相加
                cartInfo.setSkuNum(skuNum + cartInfo.getSkuNum());
                //放入缓存
                jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfo));
                isMatch = true;
            }
        }

        if (!isMatch){
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();

            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setSkuId(skuInfo.getId());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);

            jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfo));
        }


        jedis.close();

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {

        String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();

        List<CartInfo> cartInfoList = new ArrayList<>();
        List<String> cartCheckedJsonList = jedis.hvals(cartCheckedKey);

        if (cartCheckedJsonList != null && cartCheckedJsonList.size() >0){
            for (String cartCheckedJson : cartCheckedJsonList) {
                //CartInfo cartInfo = JSON.parseObject(cartCheckedJson, CartInfo.class);
                cartInfoList.add(JSON.parseObject(cartCheckedJson, CartInfo.class));
            }
        }
        jedis.close();
        return cartInfoList;
    }
}
