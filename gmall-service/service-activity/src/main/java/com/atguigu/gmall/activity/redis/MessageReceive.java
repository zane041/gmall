package com.atguigu.gmall.activity.redis;

import com.atguigu.gmall.activity.util.CacheHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author: atguigu
 * @create: 2023-01-30 15:19
 */
@Slf4j
@Component
public class MessageReceive {

//    @Autowired
//    private Cache<String, String> seckillCache;


    /**
     * 本地缓存重启服务后没了，需要清理分布式缓存，再重新加入缓存
     * 订阅主题seckillpush中消息
     *
     *
     * @param msg 形式  ""37:1""（注意这个坑：redis传来的消息带引号，需要去除）
     *             消息格式
     *     skuId:0 表示没有商品
     *     skuId:1 表示有商品
     */
    public void receiveMessage(String msg) {
        log.info("监听到广播消息：" + msg);
        if (StringUtils.isNotBlank(msg)) {
            //去除多余引号
            msg = msg.replaceAll("\"", "");
            //将商品状态位 存入本地缓存 自定义或者Caffeine均可
            String[] split = msg.split(":");
            if (split != null && split.length == 2) {
                CacheHelper.put(split[0], split[1]);
                //seckillCache.put(split[0], split[1]);
            }
        }
    }
}