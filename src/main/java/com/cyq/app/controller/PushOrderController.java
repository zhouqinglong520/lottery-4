package com.cyq.app.controller;

import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.app.constant.OrderSource;
import com.cyq.app.entity.AppOrder;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppClickLogService;
import com.cyq.app.service.BetService;
import com.cyq.app.service.PushOrderService;
import com.cyq.lottery.cache.JclqMatchSpService;
import com.cyq.lottery.cache.JczqMatchSpService;
import com.cyq.lottery.constant.BetType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 推单
 */
@Slf4j
@Validated
@RequestMapping("/pushOrder")
@RestController
public class PushOrderController extends ApiController {

    @Autowired
    PushOrderService pushOrderService;
    @Autowired
    BetService betService;
    @Autowired
    JclqMatchSpService jclqMatchSpService;
    @Autowired
    JczqMatchSpService jczqMatchSpService;
    @Autowired
    AppClickLogService appClickLogService;

    /**
     * 推单列表
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getPushOrderList")
    public R<List<Map<String, Object>>> getPushOrderList(@RequestParam Integer pageNum,
                                                         @RequestParam Integer pageSize) throws Exception {
        return success(pushOrderService.getPushOrderList(pageNum,pageSize,7));
    }


    /**
     * 获取推单详情
     *
     * @param pushOrderId
     * @return
     */
    @GetMapping("/getPushOrderInfo")
    public R<Map<String, Object>> getPushOrderInfo(
                @NotNull @RequestParam("pushOrderId") Long pushOrderId) throws Exception {
        return success(pushOrderService.getPushOrderInfo(pushOrderId,RequestContext.getUserId()));
    }


    /**
     * 参与推单
     *
     * @param pushOrderId   投注内容
     * @param betTimes      倍数
     * @return orderId 订单
     */
    @PostMapping("/createOrder")
    public R<String> createOrder(
            @RequestParam Integer betTimes,
            @RequestParam Long pushOrderId) {
        Integer userId = RequestContext.getUserId();
        Map<String,Object> pushOrder = pushOrderService.getPushOrderByOrderId(pushOrderId);
        int lotteryTypeNo = Integer.parseInt(String.valueOf(pushOrder.get("lotteryType")));

        String stakeNumber = String.valueOf(pushOrder.get("content"));
        String phase = String.valueOf(pushOrder.get("phase"));
        int amount = Integer.parseInt(String.valueOf(pushOrder.get("amount")));
        AppOrder order = betService.createOrder(userId, stakeNumber, lotteryTypeNo, phase, betTimes, BetType.JOIN_PUSH_ORDER, amount, OrderSource.APP);
        pushOrderService.createPushOrderRelation(pushOrderId,order.getOrderId());

        return success(order.getOrderId());
    }

}
