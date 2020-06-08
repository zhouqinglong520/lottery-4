package com.cyq.app.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.cyq.app.constant.AppConsts;
import com.cyq.app.constant.OrderSource;
import com.cyq.app.entity.*;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.*;
import com.cyq.lottery.constant.BetType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 跟单
 */
@Slf4j
@Validated
@RequestMapping("/followOrder")
@RestController
public class FollowOrderController extends ApiController {

    @Autowired
    private AppFollowOrderService appFollowOrderService;
    @Autowired
    private BetService betService;
    @Autowired
    private AppLotteryKingApplyService appLotteryKingApplyService;
    @Autowired
    private AppOrderService appOrderService;
    @Autowired
    private AppOrderCommentService appFollowOrderCommentService;
    @Autowired
    private AppOrderCommentLikeService appFollowOrderCommentUserService;

    /**
     * 彩帝排行榜
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getLotteryKingRankList")
    public R<List<Map<String, Object>>> getLotteryKingRankList(
         @RequestParam(required = false,defaultValue = "1") Integer pageNum
        ,@RequestParam(required = false,defaultValue = "10") Integer pageSize
        ,@RequestParam Integer sortType) throws Exception {
        return success(appFollowOrderService.getLotteryKingRankList(pageNum,pageSize,sortType,RequestContext.getUserId()));
    }



    /**
     * 跟单列表
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getFollowOrderList")
    public R<List<Map<String, Object>>> getFollowOrderList(@RequestParam Integer pageNum,
                                                         @RequestParam Integer pageSize,
                                                           @RequestParam(required = false) Integer sort) throws Exception {
        return success(appFollowOrderService.getFollowOrderList(pageNum,pageSize,RequestContext.getUserId(),sort));
    }

    /**
     * 我的跟单列表
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getJoinFollowOrderList")
    public R<List<Map<String, Object>>> getJoinFollowOrderList(@RequestParam Integer pageNum,
                                                         @RequestParam Integer pageSize) throws Exception {
        return success(appFollowOrderService.getJoinFollowOrderList(pageNum,pageSize,RequestContext.getUserId(),RequestContext.getUserId()));
    }

    /**
     * 彩帝详情页 正在推荐
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getFollowOrderIng")
    public R<Map<String, Object>> getFollowOrderIng(@RequestParam Integer userId
                                                    ,@RequestParam Integer pageNum
                                                    ,@RequestParam Integer pageSize) throws Exception {
        return success(appFollowOrderService.getFollowOrderIng(pageNum,pageSize,userId,RequestContext.getUserId()));
    }

    /**
     * 彩帝详情页 历史推荐
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getFollowOrderHistory")
    public R<List<Map<String, Object>>> getFollowOrderHistory(@RequestParam Integer userId
            ,@RequestParam Integer pageNum
            ,@RequestParam Integer pageSize) throws Exception {
        return success(appFollowOrderService.getFollowOrderHistory(pageNum,pageSize,userId,RequestContext.getUserId()));
    }


    /**
     * 判断是否是彩帝
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getIfLotteryKing")
    public R<String> getIfLotteryKing() throws Exception {
        List<AppLotteryKingApply> appLotteryKingApplyList = appLotteryKingApplyService.list(Wrappers.<AppLotteryKingApply>lambdaQuery().eq(AppLotteryKingApply::getUserId,RequestContext.getUserId()).eq(AppLotteryKingApply::getStatus,2));
        if (appLotteryKingApplyList==null||appLotteryKingApplyList.size()==0){
            return success("false");
        }else {
            return success("true");
        }
    }


    /**
     * 跟单详情
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getFollowOrderDetail")
    public R<Map<String, Object>> getFollowOrderDetail(@RequestParam Integer id ) throws Exception {
        return success(appFollowOrderService.getFollowOrderDetail(id,RequestContext.getUserId()));

    }


    /**
     * 参与跟单
     *
     * @param followOrderId
     * @param betTimes      倍数
     * @return orderId 订单
     */
    @PostMapping("/joinFollowOrder")
    public R<String> joinFollowOrder(
            @RequestParam Integer betTimes,
            @RequestParam Integer followOrderId) {
        Integer userId = RequestContext.getUserId();
        AppFollowOrder appFollowOrder = appFollowOrderService.getFollowOrderByOrderId(followOrderId);

        String stakeNumber = String.valueOf(appFollowOrder.getContent());
        String phase = String.valueOf(appFollowOrder.getPhase());
        int amount = Integer.parseInt(String.valueOf(appFollowOrder.getAmount()));
        AppOrder order = betService.createOrder(userId, stakeNumber, appFollowOrder.getLotteryType(), phase, betTimes, BetType.GENDAN_BY_FOLLOWER, amount, OrderSource.APP);
        appFollowOrderService.createAppFollowOrderJoin(followOrderId,order.getOrderId(),userId,order.getTotalAmount());

        return success(order.getOrderId());
    }


    /**
     * 获取是否达到了彩帝申请标准
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/ifApplyLotteryKing")
    public R<String> ifApplyLotteryKing() throws Exception {
        Integer userId = RequestContext.getUserId();
        //查询用户已购买了多少钱的彩票
        Integer totalAmount = appOrderService.getAllJcTotalAmount(userId);
        if (totalAmount.intValue()< AppConsts.MIN_LOTTEY_KING_AMOUNT){
            throw new ApiException("申请彩帝前需在本站购买竞彩足球达到"+AppConsts.MIN_LOTTEY_KING_AMOUNT+"元及以上");
        }
        return success("true");

    }


    /**
     * 申请成为彩帝
     *
     */
    @PostMapping("/applyLotteryKing")
    public R<String> applyFollowKing(@RequestParam String reason) {
        Integer userId = RequestContext.getUserId();
        //查询用户已购买了多少钱的彩票
        Integer totalAmount = appOrderService.getAllJcTotalAmount(userId);
        if (totalAmount.intValue()< AppConsts.MIN_LOTTEY_KING_AMOUNT){
            throw new ApiException("申请彩帝前需在本站购买竞彩足球达到"+AppConsts.MIN_LOTTEY_KING_AMOUNT+"元及以上");
        }

        List<AppLotteryKingApply> oldAppLotteryKingApplyList = appLotteryKingApplyService.list(Wrappers.<AppLotteryKingApply>lambdaQuery().eq(AppLotteryKingApply::getUserId,RequestContext.getUserId()).eq(AppLotteryKingApply::getStatus,2));
        if (oldAppLotteryKingApplyList!=null && oldAppLotteryKingApplyList.size()>0){
            throw new ApiException("您已经是彩帝,请勿重复申请");
        }else {
            AppLotteryKingApply oldAppLotteryKingApply = appLotteryKingApplyService.getOne(Wrappers.<AppLotteryKingApply>lambdaQuery().eq(AppLotteryKingApply::getUserId,RequestContext.getUserId()).eq(AppLotteryKingApply::getStatus,1));
            if (oldAppLotteryKingApply!=null){
                throw new ApiException("您已经申请过,工作人员未处理前请勿重复申请");
            }
        }
        AppLotteryKingApply appLotteryKingApply = new AppLotteryKingApply();
        appLotteryKingApply.setUserId(userId);
        appLotteryKingApply.setReason(reason);
        appLotteryKingApply.setTotalAmount(totalAmount);
        appLotteryKingApply.setCreateTime(new Date());
        appLotteryKingApply.setUpdateTime(new Date());
        appLotteryKingApply.setStatus(1);
        appLotteryKingApplyService.save(appLotteryKingApply);

        return success("OK");
    }



    /**
     * 评价跟单
     *
     * @param followOrderId
     * @param content      内容
     */
    @PostMapping("/commentFollowOrder")
    public R<String> commentFollowOrder(@RequestParam Integer followOrderId
            ,@RequestParam String content
            ,@RequestParam Integer type
            ,Integer subType) {
        Integer userId = RequestContext.getUserId();
        AppOrderComment appFollowOrderComment = new AppOrderComment();
        appFollowOrderComment.setUserId(userId);
        appFollowOrderComment.setKeyId(followOrderId);
        appFollowOrderComment.setContent(content);
        appFollowOrderComment.setType(type);
        appFollowOrderComment.setStatus(1);
        appFollowOrderComment.setState(0);
        if(subType!=null){
            if(subType==1){
                appFollowOrderComment.setTitle("红");
            } else{
                appFollowOrderComment.setTitle("黑");
            }
        }
        appFollowOrderCommentService.save(appFollowOrderComment);
        return success(String.valueOf(appFollowOrderComment.getId()));
    }

    /**
     * 点赞跟单评价
     *
     */
    @PostMapping("/commentFollowOrderSupport")
    public R<String> commentFollowOrderSupport(@RequestParam Integer followOrderCommentId) {
        Integer userId = RequestContext.getUserId();

        List<AppOrderCommentLike> appFollowOrderCommentUsers = appFollowOrderCommentUserService.list(Wrappers.<AppOrderCommentLike>lambdaQuery()
                .eq(AppOrderCommentLike::getUserId,userId)
                .eq(AppOrderCommentLike::getOrderCommentId,followOrderCommentId));
        if (appFollowOrderCommentUsers.size()>0){
            throw new ApiException("请勿重复评价");
        }

        AppOrderCommentLike appFollowOrderCommentUser = new AppOrderCommentLike();
        appFollowOrderCommentUser.setUserId(userId);
        appFollowOrderCommentUser.setOrderCommentId(followOrderCommentId);
        appFollowOrderCommentUserService.save(appFollowOrderCommentUser);

        return success("OK");
    }




}
