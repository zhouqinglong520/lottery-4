package com.cyq.app.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyq.app.constant.LuckyPlanStatus;
import com.cyq.app.dto.LuckyPlanDetailDTO;
import com.cyq.app.entity.LuckyPlan;
import com.cyq.app.entity.LuckyPlanDetail;
import com.cyq.app.entity.LuckyPlanFocus;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.*;
import com.cyq.common.dto.PageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 福袋，更加理性的投资理财计划 前端控制器
 * </p>
 */
@Validated
@RestController
@RequestMapping("/luckyPlan")
public class LuckyPlanController extends ApiController {

    @Autowired
    LuckyPlanService luckyPlanService;
    @Autowired
    LuckyPlanDetailService luckyPlanDetailService;
    @Autowired
    LuckyPlanUserService luckyPlanUserService;
    @Autowired
    LuckyPlanFocusService luckyPlanFocusService;
    @Autowired
    BetService betService;

    /**
     * 获取福袋推荐列表
     *
     * @param pageDTO
     * @return
     */
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(PageDTO pageDTO) {
        Integer userId = RequestContext.getUserId();
        Page<LuckyPlan> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        List<Map<String, Object>> maps = luckyPlanService.planInfoList(page, LuckyPlanStatus.PRE_SALE.getValue(), userId);
        return success(maps);
    }

    /**
     * 我的福袋参与列表
     */
    @GetMapping("/myParticipation")
    public R<List<Map<String, Object>>> myParticipation(PageDTO pageDTO) {
        Page<LuckyPlan> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        Integer userId = RequestContext.getUserId();
        List<Map<String, Object>> maps = luckyPlanService.myParticipation(page, userId);
        return success(maps);
    }


    /**
     * 我的福袋详情
     *
     * @param planId
     * @return
     */
    @GetMapping("/myLuckyPlan")
    public R<Map<String, Object>> myLuckyPlan(
            @RequestParam("planId") Integer planId) {
        return success(luckyPlanService.myPlanInfo(planId, RequestContext.getUserId()));
    }

    /**
     * 我的关注详情
     */
    @GetMapping("/myFocus")
    public R<Map<String, Object>> myFocus(
            @RequestParam("planId") Integer planId) {
        return success(luckyPlanService.myFocus(planId, RequestContext.getUserId()));
    }

    /**
     * 我的关注列表
     */
    @GetMapping("/myFocusList")
    public R<List<Map<String, Object>>> myFocusList(PageDTO pageDTO) {
        return success(luckyPlanService.myFocusList(RequestContext.getUserId(), pageDTO));
    }

    /**
     * 添加关注
     *
     * @param planId
     * @return
     */
    @PostMapping("/addFocus")
    public R<Boolean> addFocus(
            @RequestParam("planId") Integer planId) {
        Integer userId = RequestContext.getUserId();
        int count = luckyPlanFocusService.count(Wrappers.<LuckyPlanFocus>lambdaQuery()
                .eq(LuckyPlanFocus::getUserId, userId)
                .eq(LuckyPlanFocus::getPlanId, planId));
        if (count > 0) {
            return success(true);
        }
        LuckyPlanFocus luckyPlanFocus = new LuckyPlanFocus();
        luckyPlanFocus.setUserId(userId);
        luckyPlanFocus.setPlanId(planId);
        luckyPlanFocus.setStatus(1);
        luckyPlanFocus.setCreateTime(LocalDateTime.now());
        return success(luckyPlanFocusService.save(luckyPlanFocus));
    }


    /**
     * @param planId     福袋id
     * @param autoBuy    0 跟投的方式参数，1 全额自动参与
     * @param buyNum     购买份数
     * @param returnType 赔付类型：0半赔，1全赔，若 currentNum = 1， 则 returnType 不能为空
     * @return 返回订单号和剩余支付时间
     */
    @PostMapping("/createOrder")
    public R<String> createOrder(
            @RequestParam("planId") Integer planId,
            @RequestParam("autoBuy") Integer autoBuy,
            @RequestParam("buyNum") Integer buyNum,
            @RequestParam(name = "returnType", required = false) Integer returnType) {
        Integer userId = RequestContext.getUserId();
        return success(luckyPlanService.createOrder(userId, planId, autoBuy, buyNum, returnType));
    }

    /**
     * 获取福袋待支付信息
     *
     * @param orderId
     * @return
     */
    @RequestMapping("/getPayInfo")
    public R<Map<String, Object>> getPayInfo(
            @NotBlank @RequestParam("orderId") String orderId) {
        Integer userId = RequestContext.getUserId();
        return success(luckyPlanService.getPayInfo(userId, orderId));
    }

    /**
     * 查询福袋当前的份额信息
     *
     * @return
     */
    @GetMapping("/getShareInfo")
    public R<Map<String, Object>> getShareInfo(
            @NotNull @RequestParam("planId") Integer planId) {
        return success(luckyPlanService.getShareInfo(planId));
    }

    @PostMapping("/createPlanDetail")
    public R<LuckyPlanDetail> createPlanDetail(LuckyPlanDetailDTO luckyPlanDetailDTO) {
        LuckyPlanDetail planDetail = betService.createPlanDetail(luckyPlanDetailDTO);
        return success(planDetail);
    }
}
