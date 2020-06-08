package com.cyq.app.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.cyq.app.constant.OrderSource;
import com.cyq.app.dto.AppCooperationOrderDTO;
import com.cyq.app.dto.CooperationOrderJoinListDTO;
import com.cyq.app.dto.CooperationOrderListDTO;
import com.cyq.app.entity.AppCooperationOrderJoin;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppCooperationOrderJoinService;
import com.cyq.app.service.AppCooperationOrderService;
import com.cyq.common.cache.RedisService;
import com.cyq.lottery.constant.BetType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * 合买
 */
@Slf4j
@Validated
@RequestMapping("/cooperationOrder")
@RestController
public class CooperationOrderController extends ApiController {

    @Autowired
    private AppCooperationOrderService appCooperationOrderService;
    @Autowired
    private AppCooperationOrderJoinService appCooperationOrderJoinService;
    @Autowired
    private RedisService redisService;

    /**
     * 发起合买
     * @param stakeNumber
     * @param lotteryTypeNo
     * @param betTimes
     * @param amount
     * @param phase
     * @param title
     * @param description
     * @param proportion
     * @param num
     * @param selfNum
     * @param visibles
     * @return
     */
    @PostMapping("/createOrder")
    public R<String> createOrder(
            @RequestParam String stakeNumber,
            @RequestParam Integer lotteryTypeNo,
            @RequestParam Integer betTimes,
            @RequestParam int amount,
            @RequestParam String phase,
            @RequestParam String title,
            @RequestParam(required = false,defaultValue = "") String description,
            @Min(0) @Max(10) @RequestParam(required = false) Integer proportion,
            @RequestParam Integer num,
            @Min(1) @RequestParam Integer selfNum,
            @RequestParam Integer visibles
    ) {
        if (num>100){
            throw new ApiException("份数不能超过100份");
        }
        Integer userId = RequestContext.getUserId();
        AppCooperationOrderJoin appCooperationOrderJoin =appCooperationOrderService.createOrder(userId, stakeNumber, lotteryTypeNo, phase, betTimes,BetType.HEMAI_BY_STARTER
                , amount, OrderSource.APP,title,description,proportion,num,selfNum,visibles);
        return success(String.valueOf(appCooperationOrderJoin.getId()));
    }


    /**
     * 支付参与合买的订单
     * @param cooperationOrderJoinId
     * @return
     */
    @PostMapping("/pay")
    public R<String> payOrder(@RequestParam String cooperationOrderJoinId) {
        AppCooperationOrderJoin appCooperationOrderJoin = appCooperationOrderJoinService.getOne(Wrappers.<AppCooperationOrderJoin>lambdaQuery().eq(AppCooperationOrderJoin::getId,cooperationOrderJoinId));
        //加锁10秒 防止并发付款
        String lockKey = "cooperationOrderLock:"+appCooperationOrderJoin.getCooperationOrderId();
        boolean lock = redisService.lock(lockKey,10,cooperationOrderJoinId);
        if (lock){
            appCooperationOrderJoinService.payOrder(cooperationOrderJoinId);
            redisService.delete(lockKey);
        }else {
            throw new ApiException("当前购买人数过多,请稍后付款");
        }
        return success("OK");
    }


    /**
     * 参与合买
     * @param cooperationOrderId 合买的id
     * @param num 认购份数
     * @return
     */
    @PostMapping("/join")
    public R<String> joinOrder(
            @RequestParam String cooperationOrderId,
            @Min(1) @RequestParam Integer num
    ) {
        AppCooperationOrderJoin appCooperationOrderJoin = appCooperationOrderJoinService.joinOrder(RequestContext.getUserId(),cooperationOrderId,num);
        return success(String.valueOf(appCooperationOrderJoin.getId()));
    }


    /**
     * 获取合买支付页面信息
     * @param cooperationOrderJoinId 参与合买的id
     * @return
     */
    @GetMapping("/getPayInfo")
    public R<AppCooperationOrderDTO> getPayInfo(@RequestParam String cooperationOrderJoinId) {
        AppCooperationOrderDTO appCooperationOrderDTO = appCooperationOrderJoinService.getPayInfo(cooperationOrderJoinId);
        return success(appCooperationOrderDTO);
    }



    /**
     * 合买大厅
     * @return
     */
    @GetMapping("/getCooperationOrderList")
    public R<List<Map<String,Object>>> getCooperationOrderList(@RequestParam Integer pageNum,@RequestParam Integer pageSize) {
        List<Map<String,Object>> re = appCooperationOrderService.getCooperationOrderList(pageNum,pageSize);
        return success(re);
    }


    /**
     * 参与合买的列表
     * @return
     */
    @GetMapping("/getCooperationOrderJoinList")
    public R<List<CooperationOrderJoinListDTO>> getCooperationOrderJoinList(@RequestParam Integer pageNum,@RequestParam Integer pageSize) {
        Integer userId = RequestContext.getUserId();
        List<CooperationOrderJoinListDTO> re = appCooperationOrderJoinService.getCooperationOrderJoinList(userId,pageNum,pageSize);
        return success(re);
    }

    /**
     * 发布合买的列表
     * @return
     */
    @GetMapping("/getCooperationOrderPublishList")
    public R<List<CooperationOrderListDTO>> getCooperationOrderPublishList(@RequestParam Integer pageNum,@RequestParam Integer pageSize) {
        Integer userId = RequestContext.getUserId();
        List<CooperationOrderListDTO> re = appCooperationOrderService.getCooperationOrderPublishList(userId,pageNum,pageSize);
        return success(re);
    }

    /**
     * 合买的详情
     * @return
     */
    @GetMapping("/detail")
    public R<Map<String,Object>> detail(@RequestParam String cooperationOrderId) {
        Integer userId = RequestContext.getUserId();
        return success(appCooperationOrderService.detail(cooperationOrderId,userId));
    }


}
