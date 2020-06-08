package com.cyq.app.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.app.TIM.ChatType;
import com.cyq.app.TIM.IMCallbackProcessor;
import com.cyq.app.TIM.IMProperties;
import com.cyq.app.TIM.ProcessorHolder;
import com.cyq.app.TIM.service.IMService;
import com.cyq.app.constant.PushNoticeType;
import com.cyq.app.helper.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Validated
@RestController
@RequestMapping("/TIM")
public class TIMController extends ApiController {
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    @Autowired
    IMProperties imProperties;
    @Autowired
    ProcessorHolder processorHolder;
    @Autowired
    IMService imService;

    @PostMapping("/callback")
    public void callback(
            @RequestParam("SdkAppid") String sdkAppid,
            @RequestParam("CallbackCommand") String callbackCommand,
            @RequestParam(name = "ClientIP", required = false) String clientIp,
            @RequestParam(name = "OptPlatform", required = false) String optPlatform,
            @RequestBody String body) throws IOException {
        log.info(">>> QueryString = {}", RequestContext.getRequest().getQueryString());
        log.info(">>> Body = {}", body);
        JSONObject result = new JSONObject();
        if (!imProperties.getSdkAppid().equals(sdkAppid)) {
            result.fluentPut("ActionStatus", "FAIL").fluentPut("ErrorCode", 0).fluentPut("ErrorInfo", "SdkAppid参数错误！");
        } else {
            result.fluentPut("ActionStatus", "OK").fluentPut("ErrorCode", 0).fluentPut("ErrorInfo", "");
        }
        executorService.submit(() -> {
            if (!StringUtils.isEmpty(callbackCommand)) {
                JSONObject param = JSON.parseObject(body);
                IMCallbackProcessor processor = processorHolder.getProcessor(callbackCommand);
                if (processor == null) {
                    log.error(">>> 与[{}]相关的回调服务未配置！", callbackCommand);
                } else {
                    processor.process(param);
                }
            }
        });
        HttpServletResponse response = RequestContext.getResponse();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.getWriter().write(result.toJSONString());
    }

    /**
     * 消息测试
     *
     * @return
     */
    @RequestMapping("/test")
    public R<String> test(@RequestParam("userId") Integer userId) {
        // 只有出票了的订单才发送中奖消息提醒
        String title = "中奖通知";
        String content = "您购买的方案已中奖，点击查看！";
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("orderId", RandomStringUtils.randomAlphanumeric(16));
        imService.pushCustomMsg(String.valueOf(userId), title, content, jsonParam, PushNoticeType.SIMPLE_TEXT_MSG, ChatType.push, true);
        return success("OK");
    }

    /**
     * 充值成功
     *
     * @return
     */
    @RequestMapping("/recharge")
    public R<String> recharge(@RequestParam("userId") Integer userId) {
        String title = "充值成功";
        String content = String.format("充值到账");
        imService.pushCustomMsg(String.valueOf(userId), title, content, Collections.EMPTY_MAP, PushNoticeType.RECHARGE, ChatType.push, true);
        return success("OK");
    }

    /**
     * 提现成功
     *
     * @return
     */
    @RequestMapping("/withdraw")
    public R<String> withdraw(
            @RequestParam("userId") Integer userId,
            @RequestParam("fee") Integer fee) {
        String title = "提现到账";
        String content = String.format("您申请%s元(获取金额)提现已到账！", fee);
        imService.pushCustomMsg(String.valueOf(userId), title, content, Collections.EMPTY_MAP, PushNoticeType.WITHDRAW, ChatType.push, true);
        return success("OK");
    }

//    /**
//     * 兑奖成功
//     *
//     * @return
//     */
//    @RequestMapping("/rewarded")
//    public R<String> withdraw(
//            @RequestParam("userId") Integer userId,
//            @RequestParam("orderId") String orderId) {
//        String title = "中奖通知";
//        String content = "您购买的方案已中奖，点击查看！";
//        Map<String, Object> jsonParam = new HashMap<>();
//        jsonParam.put("orderId", orderId);
//        imService.pushCustomMsg(String.valueOf(userId), title, content, jsonParam, PushNoticeType.REWARDED, ChatType.push, true);
//        return success("OK");
//    }

    @RequestMapping("/lucky_plan_end")
    public R<String> lucky_plan_end(
            @RequestParam("userId") Integer userId) {
        String title = "福袋中奖通知";
        String content = "福袋中奖，本期已完成，试试其他福袋！";
        imService.pushCustomMsg(String.valueOf(userId), title, content, Collections.emptyMap(), PushNoticeType.LUCKY_PLAN_END, ChatType.push, true);
        return success("OK");
    }

    @RequestMapping("/order_expired")
    public R<String> order_expired(
            @RequestParam("userId") Integer userId,
            @RequestParam("orderId") String orderId) {
        String title = "订单过期商家未出票";
        String content = "方案出票失败，款项已退还至您的余额";
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("orderId", orderId);
        imService.pushCustomMsg(String.valueOf(userId), title, content, jsonParam, PushNoticeType.ORDER_EXPIRED, ChatType.push, true);
        return success("OK");
    }

    @RequestMapping("/lucky_plan_focus")
    public R<String> lucky_plan_focus(
            @RequestParam("userId") Integer userId,
            @RequestParam("planId") String planId) {
        String title = "福袋关注通知";
        String content = "福袋中关注的方案发布了新单";
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("planId", planId);
        imService.pushCustomMsg(String.valueOf(userId), title, content, jsonParam, PushNoticeType.LUCKY_PLAN_FOCUS, ChatType.push, true);
        return success("OK");
    }

    @RequestMapping("/lucky_plan_auto_buy")
    public R<String> lucky_plan_auto_buy(
            @RequestParam("userId") Integer userId,
            @RequestParam("planId") String planId) {
        String title = "福袋自动购单通知";
        String content = "福袋自动购买本期推单";
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("planId", planId);
        imService.pushCustomMsg(String.valueOf(userId), title, content, jsonParam, PushNoticeType.LUCKY_PLAN_AUTO_BUY, ChatType.push, true);
        return success("OK");
    }

    @RequestMapping("/store_balance_not_enough")
    public R<String> store_balance_not_enough(
            @RequestParam("userId") Integer userId) {
        String title = "余额不足";
        String content = String.format("store_balance_not_enough");
            imService.pushCustomMsg(String.valueOf(userId), title, content, Collections.EMPTY_MAP, PushNoticeType.STORE_BALANCE_NOT_ENOUGH, ChatType.push, true);
        return success("OK");
    }

    @RequestMapping("/pushCustomMsg")
    public R<String> pushCustomMsg(
            @RequestParam("userIdList") Set<String> userIdList,
            @RequestParam("planId") List<String> planId) {
        String title = "福袋自动购单通知";
        String content = "已自动购买福袋第 " + 1 + " 期推单";
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("planId", 1);
        imService.pushBatchCustomMsg(userIdList, title, content, jsonParam, PushNoticeType.LUCKY_PLAN_AUTO_BUY, ChatType.push, true);
        return success("OK");
    }
}
