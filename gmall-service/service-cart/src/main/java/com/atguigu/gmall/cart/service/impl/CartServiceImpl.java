package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.cart.service.CartInfoService;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CartInfoService cartInfoService;

    // 异步执行数据库操作
    @Autowired
    private CartAsyncService cartAsyncService;

    @Override
    @Transactional(rollbackFor = Exception.class) // 如果下面的数据库操作使用@Async则这里的事务无效
    public void addToCart(Long skuId, Integer skuNum, String userId) {
        /*
            先判断这个商品是否存在
               true: 更新数量、选中状态、更新时间、实时价格 (mysql，redis)
               false: 添加购物车数据：mysql, redis
            （考虑事务、异步优化）

            考虑：
                如果数据库有数据但缓存没有数据(缓存key过期)，这时会再执行添加会导致数据库多了一个重复skuId数据造成幻象
                这里数据库操作不用try-catch，如果捕获异常则@Transactional注解失效，而且这里捕获不了@Async方法里的异常所以不能用异步执行数据库操作
                若数据库操作出异常不用往下执行redis操作，若redis操作出异常数据库也会回滚！
         */

        // 1. 先判断该购物车商品中是否存在
        // 先查缓存
        // 得到购物车缓存的key
        // 缓存查不到去查数据库--如果缓存key过期仍然要保护购物车数据完整。缓存过期——更新数据库数据到缓存中
        String cartKey = getCartKey(userId);
        if (!redisTemplate.opsForHash().hasKey(cartKey, skuId.toString())) {
            loadCartCache(userId);
        }
        CartInfo cartInfoExist = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());

        // 更新或者添加商品信息
        cartInfoExist = handleCartInfo(skuId, skuNum, userId, cartInfoExist);
        if (cartInfoExist == null) throw new RuntimeException("找不到该商品！");

        // 更新数据库
        // 数据库使用异步，这里捕获不了它的异常，所以当数据库操作异常，我们做到数据库回滚但缓存不能回滚这时数据库是旧数据
        // cartAsyncService.saveOrUpdate(cartInfoExist);
        // 还是用同步，至少无bug
        cartInfoService.saveOrUpdate(cartInfoExist);

        // 放入缓存
        redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfoExist);
        redisTemplate.expire(cartKey, 60 * 60 * 24, TimeUnit.SECONDS);
    }

    /**
     * 处理商品数据，若商品不存在则查询补充数据，若商品存在则更新数量、选中状态、更新时间、实时价格
     * @param skuId
     * @param skuNum
     * @param userId
     * @param cartInfoExist
     * @return
     */
    private CartInfo handleCartInfo(Long skuId, Integer skuNum, String userId, CartInfo cartInfoExist) {
        if (cartInfoExist != null) {
            // 该商品在存在，更新数量、选中状态、更新时间、实时价格
            // 每个商品最多购买200件
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() > 200 ? 200 : cartInfoExist.getSkuNum() + skuNum);
            if (cartInfoExist.getIsChecked() == 0) cartInfoExist.setIsChecked(1);
            cartInfoExist.setUpdateTime(new Date());
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
        } else {
            // 该商品不存在，存储新增购物车数据
            // 先从mysql中查询商品信息
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            if (skuInfo == null) throw new RuntimeException("找不到该商品！");
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuNum(skuNum);
            cartInfo.setIsChecked(1);
            cartInfo.setCreateTime(new Date()); // 虽然数据库有默认值但是因为还要存缓存所以我们还是给它赋值
            cartInfo.setUpdateTime(new Date());
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setUserId(userId);
            cartInfoExist = cartInfo;
        }
        return cartInfoExist;
    }

    /**
     * 获取购物车在缓存中的key
     * @param userId
     * @return
     */
    private static String getCartKey(String userId) {
        String cartKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
        return cartKey;
    }

    @Override
    public List<CartInfo> cartList(String userId, String userTempId) {
        // 从缓存找，找不到则从数据库找并更新数据进缓存

        List<CartInfo> result = new ArrayList<>();

        // 查询未登录购物车列表
        List<CartInfo> noLoginCartInfoList = new ArrayList<>();
        if (!StringUtils.isEmpty(userTempId)) noLoginCartInfoList = getCartList(userTempId);

        if (StringUtils.isEmpty(userId)) {
            // 1. 若用户未登录——返回未登录购物车列表
            result = noLoginCartInfoList;
        } else {
            // 2. 若用户已登录——查询登录购物车列表，并合并未登录购物车列表并删除未登录购物车列表
            List<CartInfo> loginCartInfoList = getCartList(userId);
            if (!CollectionUtils.isEmpty(noLoginCartInfoList)) {
                // 合并购物车
                result = mergeCartList(noLoginCartInfoList, loginCartInfoList, userId);
                // 删除未登录购物车列表
                deleteCartList(userTempId);
            } else {
                result = loginCartInfoList;
            }
        }

        // 3. 排序 —— 按更新时间降序
        result.sort((o1, o2) -> {
            return DateUtil.truncatedCompareTo(o2.getUpdateTime(), o1.getUpdateTime(), Calendar.SECOND);
        });

        return result;
    }

    @Override
    public void checkCart(String userId, Long skuId, Integer isChecked) {
        String cartKey = getCartKey(userId);
        // 更新缓存
        if (redisTemplate.hasKey(cartKey)) {
            CartInfo cartInfo = (CartInfo) redisTemplate.boundHashOps(cartKey).get(skuId.toString());
            cartInfo.setIsChecked(isChecked);
            redisTemplate.boundHashOps(cartKey).put(skuId.toString(), cartInfo);
        }
        // 异步更新数据库
        cartAsyncService.updateCartCheck(userId, skuId, isChecked);
    }

    @Override
    public void clearCart(String userId) {
        deleteCartList(userId);
    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        // 异步执行数据库
        cartAsyncService.removeCartInfo(userId, skuId);
        // 删除缓存
        redisTemplate.boundHashOps(getCartKey(userId)).delete(skuId.toString());
    }

    @Override
    public List<CartInfo> getCartCheckedList(Long userId) {
        List<CartInfo> cartList = getCartList(userId.toString());
        if (CollectionUtils.isEmpty(cartList)) return null;
        return cartList.stream().filter(cartInfo -> cartInfo.getIsChecked() == 1).collect(Collectors.toList());
    }

    /**
     * 删除购物车数据
     * @param userId
     */
    private void deleteCartList(String userId) {
        // 异步执行数据库
        cartAsyncService.remove(userId);
        // 删除缓存
        redisTemplate.delete(getCartKey(userId));
    }

    /**
     * 合并购物车数据，之后更新数据库、缓存
     * @param noLoginCartInfoList
     * @param loginCartInfoList
     * @return
     */
    private List<CartInfo> mergeCartList(List<CartInfo> noLoginCartInfoList, List<CartInfo> loginCartInfoList, String userId) {
        List<CartInfo> result = new ArrayList<>();

        // 存放合并后数据的map
        Map<String, CartInfo> cartInfoMap = new HashMap<>();

        // 1. 将已登录购物车列表数据都放进map中
        if (!CollectionUtils.isEmpty(loginCartInfoList)) {
            cartInfoMap = loginCartInfoList.stream().collect(Collectors.toMap(cartInfo -> {
                return cartInfo.getSkuId().toString();
            }, cartInfo -> cartInfo));
        }
        // 2. 遍历未登录购物车列表，合并数据进map
        if (!CollectionUtils.isEmpty(noLoginCartInfoList)) {
            for (CartInfo noLoginCartInfo : noLoginCartInfoList) {
                // 若有共同商品
                if (cartInfoMap.containsKey(noLoginCartInfo.getSkuId().toString())) {
                    CartInfo cartInfo = cartInfoMap.get(noLoginCartInfo.getSkuId().toString());
                    // 数量相加
                    cartInfo.setSkuNum(cartInfo.getSkuNum() + noLoginCartInfo.getSkuNum());
                    // 选中状态以勾选为准
                    cartInfo.setIsChecked(cartInfo.getIsChecked() + noLoginCartInfo.getIsChecked() > 0 ? 1 : 0);
                    // 更新时间以最后更新时间为准
                    cartInfo.setUpdateTime(new Date());
                } else {
                    // 若还没有该商品
                    noLoginCartInfo.setUserId(userId);
                    noLoginCartInfo.setId(null); // 数据库插入新数据，不进行更新操作是因为避免异步执行时旧数据先被删掉
                    cartInfoMap.put(noLoginCartInfo.getSkuId().toString(), noLoginCartInfo);
                }
            }
        }
        result = new ArrayList<>(cartInfoMap.values());

        if (!CollectionUtils.isEmpty(result)) {
            // 3. 异步更新数据库
            cartAsyncService.saveOrUpdateBatch(result);
            // 4. 更新缓存
            redisTemplate.boundHashOps(getCartKey(userId)).putAll(cartInfoMap);
        }
        return result;
    }

    /**
     * 查询购物车列表，从缓存里查，查不到则从数据库里查并将数据放入缓存
     * @param userId
     */
    private List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartList = new ArrayList<>();
        if (!StringUtils.isEmpty(userId)) {
            String noLoginCartKey = getCartKey(userId);
            cartList = redisTemplate.boundHashOps(noLoginCartKey).values();
            if (CollectionUtils.isEmpty(cartList)) {
                cartList = loadCartCache(userId);
            }
        }
        return cartList;
    }

    /**
     * 从数据库查数据并更新缓存
     * @param userId
     * @return
     */
    private List<CartInfo> loadCartCache(String userId) {
        // 从数据库查数据
        List<CartInfo> cartInfoList = cartInfoService.list(new LambdaQueryWrapper<CartInfo>()
                .eq(CartInfo::getUserId, userId));
        if (CollectionUtils.isEmpty(cartInfoList)) return null;
        // 更新最新价格，然后将数据放入缓存
        Map<String, CartInfo> cartInfoMap = new HashMap<>();
        cartInfoList.stream().forEach(cartInfo -> {
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            cartInfoMap.put(cartInfo.getSkuId().toString(), cartInfo);
        });
        redisTemplate.boundHashOps(getCartKey(userId)).putAll(cartInfoMap);
        return cartInfoList;
    }

}
