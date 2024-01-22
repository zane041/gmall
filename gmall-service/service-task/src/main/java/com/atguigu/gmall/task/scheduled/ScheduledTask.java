package com.atguigu.gmall.task.scheduled;


import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;

    /**
     * 正式每天凌晨1点执行
     * 测试每隔30s执行
     */
    @Scheduled(cron = "0/30 * * * * ?")
//    @Scheduled(cron = "0 0 1 * * ?")
    public void task1() {
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_1, "");
    }

    /**
     * 每天晚上18点发送消息，通知秒杀服务清理Redis缓存
     */
//    @Scheduled(cron = "0 0 18 * * ?") //正式
////    @Scheduled(cron = "0/30 * * * * ?")
//    public void sendClearSeckillGoodsCache() {
//        log.info("定时任务执行了");
//        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_18, "");
//    }
}
