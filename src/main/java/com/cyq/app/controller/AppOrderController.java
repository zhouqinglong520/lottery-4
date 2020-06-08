package com.cyq.app.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyq.app.constant.AppConsts;
import com.cyq.app.constant.OrderSource;
import com.cyq.app.constant.OrderStatus;
import com.cyq.app.dto.AppOrderDTO;
import com.cyq.app.entity.*;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.*;
import com.cyq.common.cache.RedisService;
import com.cyq.common.dto.PageDTO;
import com.cyq.lottery.constant.BetType;
import com.cyq.lottery.constant.LotteryType;
import com.cyq.lottery.constant.ZuQiuCode;
import com.cyq.lottery.dto.QrCodeRequest;
import com.cyq.lottery.entity.LotteryPhase;
import com.cyq.lottery.lott.CoreLottManager;
import com.cyq.lottery.service.LotteryPhaseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 前端控制器
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/order")
public class AppOrderController extends ApiController {

    private static final int FIXED_BET_TIMES = 50;

    @Autowired
    BetService betService;
    @Autowired
    AppOrderService appOrderService;
    @Autowired
    private AppFollowOrderService appFollowOrderService;
    @Autowired
    AppOrderQrcodeLogService appOrderQrcodeLogService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private AppLotteryKingApplyService appLotteryKingApplyService;
    @Autowired
    private CoreLottManager coreLottManager;
    @Autowired
    private LotteryPhaseService lotteryPhaseService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private AppDataDictionaryService appDataDictionaryService;
    @Autowired
    Environment env;


    /**
     * 更新官方开奖时间
     */
    @PostMapping("/updateLotteryOfficialDrawTime")
    public R<String> updateLotteryOfficialDrawTime(){
        UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://121.43.166.70:5001/lottery/single_num").build();
        URI uri = uriComponents.encode().toUri();

        List<Integer> list = new ArrayList();
        list.add(4001);
        list.add(4002);
        list.add(4003);
        list.add(4004);
        List<LotteryPhase> lotteryPhaseList = lotteryPhaseService.list(Wrappers.<LotteryPhase>lambdaQuery().in(LotteryPhase::getLotteryType,list)
                .isNull(LotteryPhase::getOfficialDrawTime).isNotNull(LotteryPhase::getActualDrawTime).orderByDesc(LotteryPhase::getCreateTime));
        for (LotteryPhase lotteryPhase:lotteryPhaseList){
            try {
                MultiValueMap<String, Object> param = new LinkedMultiValueMap<>();
                Integer lotteryType = lotteryPhase.getLotteryType();
                String lotteryNum = lotteryPhase.getPhase();
                param.add("lottery_num",lotteryNum);
                param.add("lottery_type", lotteryType);
                HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(param);
                String str = restTemplate.postForObject(uri, httpEntity, String.class);
                if (StringUtils.isBlank(str)) {
                    log.warn("彩种:" + lotteryType + ",彩期" + lotteryPhase + "，返回为空..."+str);
                    continue;
                }
                JSONObject jsonObject = JSON.parseObject(str);
                if ("200".equals(jsonObject.getString("code"))) {
                    JSONArray jsonArray = jsonObject.getJSONArray("data");
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject jsObject = jsonArray.getJSONObject(i);
                        String drawTime = jsObject.getString("prize");
                        if (StringUtils.isNotBlank(drawTime)){
                            String phase = jsObject.getString("num");
                            LotteryPhase lotteryPhase2 = lotteryPhaseService.findByTypeAndPhase(Integer.valueOf(lotteryType), phase);
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
                            lotteryPhase2.setOfficialDrawTime(LocalDateTime.parse(drawTime, dateTimeFormatter));
                            lotteryPhase2.setUpdateTime(LocalDateTime.now());
                            lotteryPhaseService.updateById(lotteryPhase2);
                        }
                    }
                }
            }catch (Exception e){
                log.error(e.getMessage());
            }
        }
        return success(JSON.toJSONString(lotteryPhaseList));

    }

    /**
     * 分批次生成二维码
     *
     * @param orderId
     * @return
     * @throws Exception
     */
    @PostMapping("/genQrInBatche")
    public R<String> genQrInBatche(
            @NotBlank @RequestParam("orderId") String orderId,
            @NotNull @Min(1) @RequestParam("num") Integer num,
            @NotBlank @RequestParam("longitude") String longitude,
            @NotBlank @RequestParam("latitude") String latitude,
            @NotBlank @RequestParam("province") String province) throws Exception {
        // 判断是否扫过这张二维码
        AppOrderQrcodeLog appOrderQrcodeLog = appOrderQrcodeLogService.findByOrderIdAndNum(orderId, num);
        if (appOrderQrcodeLog != null) {
            return success(appOrderQrcodeLog.getQrcodeData());
        }
        AppOrder appOrder = appOrderService.findByOrderId(orderId);
        // 二维码总个数
        int qrcodeCount = appOrder.getBetTimes() / FIXED_BET_TIMES + (appOrder.getBetTimes() % FIXED_BET_TIMES == 0 ? 0 : 1);
        if (num > qrcodeCount) {
            throw new ApiException("最多能生成" + qrcodeCount + "张二维码！");
        }
        QrCodeRequest qrCodeRequest = new QrCodeRequest();
        qrCodeRequest.setLotteryTypeNo(appOrder.getLotteryType());
        qrCodeRequest.setAmount(appOrder.getAmount());
        qrCodeRequest.setPhase(appOrder.getPhase());
        qrCodeRequest.setStakeNumber(appOrder.getContent());
        qrCodeRequest.setLongitude(longitude);
        qrCodeRequest.setLatitude(latitude);
        qrCodeRequest.setProvince(province);
        if (num < qrcodeCount) {
            qrCodeRequest.setBetTimes(FIXED_BET_TIMES);
        } else {
            qrCodeRequest.setBetTimes(appOrder.getBetTimes() - (num - 1) * FIXED_BET_TIMES);
        }
        String qrcodeData = betService.genQrCode(qrCodeRequest);
        // 写扫码日志
        AppOrderQrcodeLog qrcodeLog = new AppOrderQrcodeLog();
        qrcodeLog.setCreteTime(LocalDateTime.now());
        qrcodeLog.setNum(num);
        qrcodeLog.setOrderId(orderId);
        qrcodeLog.setTotal(qrcodeCount);
        qrcodeLog.setQrcodeData(qrcodeData);
        appOrderQrcodeLogService.save(qrcodeLog);
        return success(qrcodeData);
    }

    /**
     * 生成二维码数据(版本2)
     *
     * @param qrCodeRequest
     * @return
     * @throws Exception
     */
    @PostMapping("/genQrCode")
    public R<String> genQrCode(@Valid QrCodeRequest qrCodeRequest) throws Exception {
        return success(betService.genQrCode(qrCodeRequest));
    }

    /**
     * 投注接口
     *
     * @param stakeNumber   投注内容
     * @param lotteryTypeNo 彩种
     * @param betTimes      倍数
     * @param phase         期号	一般是当天的日期
     * @param amount        单注金额（2 or 3）
     * @return sellerAppMemId 销售顾问的IM id，也是app的memId
     */
    @PostMapping("/createOrder")
    public R<String> createOrder(
            @RequestParam("stakeNumber") String stakeNumber,
            @RequestParam("lotteryTypeNo") Integer lotteryTypeNo,
            @RequestParam("betTimes") Integer betTimes,
            @RequestParam("amount") int amount,
            @RequestParam("phase") String phase,
            @RequestParam(required = false,defaultValue = "false") Boolean follow,
            @Min(1) @Max(5) @RequestParam(required = false) Integer prizeIndex,
            @RequestParam(required = false) String description,
            @Min(1) @Max(10) @RequestParam(required = false) Integer proportion
        ) {
        if (!Boolean.valueOf(env.getProperty("supportCreateOrder"))) {
            throw new ApiException("不支持在线投注");
        }

        // 订单来源 orderSource，1：APP，2：桌面端、3：顾问下单
        Integer userId = RequestContext.getUserId();
        if (follow){
            AppLotteryKingApply appLotteryKingApply = appLotteryKingApplyService.getOne(Wrappers.<AppLotteryKingApply>lambdaQuery().eq(AppLotteryKingApply::getUserId,userId).eq(AppLotteryKingApply::getStatus,2));
            if (appLotteryKingApply==null){
                throw new ApiException("请现申请成为彩帝再进行推单");
            }
            if (LotteryType.getJczqFollowValue().contains(lotteryTypeNo)) {
                if (lotteryTypeNo.equals(LotteryType.JCZQ_MIX.getValue())) {
                    String[] groups = stakeNumber.substring(0, stakeNumber.lastIndexOf("|")).split("\\|");
                    for (String group : groups) {
                        String[] codes = group.substring(group.indexOf(":") + 1, group.length()).split("\\.");
                        for (String code : codes) {
                            if (!LotteryType.getJczqFollowValue().contains(ZuQiuCode.get(code).getLotteryType().getValue())) {
                                throw new ApiException("过关方式不支持跟单");
                            }
                        }
                    }
                }
//                if ("prod".equals(env.getProperty("spring.profiles.active"))){
                    String oldNum = redisService.get("followLottery:"+String.valueOf(userId));
                    if (oldNum!=null && Integer.parseInt(oldNum)>= AppConsts.LOTTEY_KING_MAX_NUM){
                        throw new ApiException(AppConsts.LOTTEY_KING_MAX_NUM+"次发单次数已用完,请明日再试");
                    }
//                }

                int totalAmount = coreLottManager.getLott(lotteryTypeNo).getSingleTotalAmount(stakeNumber, betTimes, amount);
                AppDataDictionary appDataDictionary=appDataDictionaryService.getById(9);
                if (totalAmount < Integer.parseInt(appDataDictionary.getDataValue())){
                    throw new ApiException("推荐的最低金额为"+ Integer.parseInt(appDataDictionary.getDataValue())+"元");
                }

                AppOrder order = betService.createOrder(userId, stakeNumber, lotteryTypeNo, phase, betTimes, follow?BetType.GENDAN_BY_STARTER:BetType.BET, amount, OrderSource.APP);

                AppFollowOrder appFollowOrder = new AppFollowOrder();
                appFollowOrder.setUserId(order.getUserId());
                appFollowOrder.setPhase(order.getPhase());
                appFollowOrder.setLotteryType(order.getLotteryType());
                appFollowOrder.setLotteryName(order.getLotteryName());
                appFollowOrder.setContent(order.getContent());
                appFollowOrder.setBetTimes(1);
                appFollowOrder.setAmount(order.getAmount());
                appFollowOrder.setPrizeIndex(prizeIndex);
                appFollowOrder.setDescription(description);
                appFollowOrder.setProportion(proportion);
                appFollowOrder.setEndTicketTime(new Date((order.getEndTicketTime().atZone(ZoneOffset.systemDefault()).toEpochSecond()-Integer.parseInt(appDataDictionaryService.getById(8).getDataValue())*60)*1000));
                appFollowOrder.setTotalAmount(order.getTotalAmount());
                appFollowOrder.setOrderId(order.getOrderId());
                appFollowOrder.setCreateTime(new Date());
                appFollowOrder.setUpdateTime(new Date());
                appFollowOrderService.save(appFollowOrder);

                return success(order.getOrderId());
            }else {
                throw new ApiException("过关方式不支持跟单");
            }
        }else {
            AppOrder order = betService.createOrder(userId, stakeNumber, lotteryTypeNo, phase, betTimes, follow?BetType.GENDAN_BY_STARTER:BetType.BET, amount, OrderSource.APP);
            return success(order.getOrderId());
        }

    }

    /**
     * 购彩记录
     *
     * @param status 订单详情状态,0全部，1待付款，2待出票，3待开奖，4已中奖
     * @return
     */
    @GetMapping("/buyLottRecord")
    public R<List<Map<String, Object>>> buyLottRecord(
            @RequestParam(value = "status", required = false, defaultValue = "0") Integer status,
            PageDTO pageDTO) {
        Integer userId = Integer.valueOf(RequestContext.getUserId());
        Page<AppOrder> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        return success(appOrderService.buyLottRecord(userId, status, page));
    }

    @PostMapping("/delete")
    public R<String> delete(@RequestParam String orderId){
        appOrderService.update(Wrappers.<AppOrder>lambdaUpdate().set(AppOrder::getStatus,2).in(AppOrder::getOrderId,orderId.split(",")));
        return success("OK");

    }

    /**
     * 统计待支付订单数
     * @return
     */
    @GetMapping("/waitPay")
    public R<Integer> orderWaitPay() {
        return success(appOrderService.count(Wrappers.<AppOrder>lambdaQuery()
                .eq(AppOrder::getUserId,RequestContext.getUserId())
                .eq(AppOrder::getOrderStatus,OrderStatus.WAITING_FOR_PAY.getValue())));
    }

    /**
     * 订单详情
     *
     * @param orderId
     * @return
     */
    @GetMapping("/orderDetail")
    public R<Map<String, Object>> orderDetail(
            @NotBlank @RequestParam("orderId") String orderId) {
        return success(appOrderService.orderDetail(orderId));
    }

    /**
     * 待支付页面展示的支付信息
     *
     * @param orderId
     * @return
     */
    @GetMapping("/getPayInfo")
    public R<AppOrderDTO> getPayInfo(
            @NotBlank @RequestParam("orderId") String orderId) {
        return success(appOrderService.getPayInfo(orderId));
    }

    /**
     * 订单拆票信息
     *
     * @param orderId
     * @return
     */
    @GetMapping("/splitTicketInfo")
    public R<Object> ticketInfo(
            @NotBlank @RequestParam("orderId") String orderId) {
        return success(appOrderService.splitTicketInfo(orderId));
    }

    /**
     * 中奖通知轮播
     */
    @GetMapping("/prizeOrderNoticeList")
    public R<List<Map<String, Object>>> prizeOrderNoticeList() {
        return success(appOrderService.getPrizeOrderNoticeList());
    }

}
