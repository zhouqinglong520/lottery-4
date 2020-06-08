package com.cyq.app.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyq.app.constant.LuckyPlanOrderStatus;
import com.cyq.app.entity.LuckyPlanOrder;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.LuckyPlanOrderService;
import com.cyq.common.dto.PageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 福袋订单 前端控制器
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/luckyPlan")
public class LuckyPlanOrderController extends ApiController {

    @Autowired
    LuckyPlanOrderService luckyPlanOrderService;

    /**
     * 福袋订单列表
     *
     * @param orderStatus 0全部，1待支付，2已支付，3已取消，4已退款
     * @param pageDTO
     * @return
     */
    @GetMapping("/orderList")
    public R<List<Map<String, Object>>> list(
            @RequestParam(value = "orderStatus", required = false, defaultValue = "0") Integer orderStatus,
            PageDTO pageDTO) {
        Integer userId = RequestContext.getUserId();
        Page<LuckyPlanOrder> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        return success(luckyPlanOrderService.orderList(userId, orderStatus, page));
    }

    /**
     * 福袋订单详情
     *
     * @param orderId
     * @return
     */
    @GetMapping("/orderDetail")
    public R<Map<String, Object>> orderDetail(
            @NotBlank @RequestParam("orderId") String orderId) {
        Integer userId = RequestContext.getUserId();
        return success(luckyPlanOrderService.orderDetail(userId, orderId));
    }

    /**
     * 取消福袋订单
     *
     * @param orderId
     * @return
     */
    @PostMapping("/cancelOrder")
    public R<Boolean> cancelOrder(
            @NotBlank @RequestParam("orderId") String orderId) {
        Integer userId = RequestContext.getUserId();
        boolean update = luckyPlanOrderService.update(Wrappers.<LuckyPlanOrder>lambdaUpdate()
                .set(LuckyPlanOrder::getOrderStatus, LuckyPlanOrderStatus.CANCELED.getValue())
                .eq(LuckyPlanOrder::getOrderId, orderId)
                .eq(LuckyPlanOrder::getUserId, userId));
        return success(update);
    }

}
