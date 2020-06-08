package com.cyq.app.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.app.constant.BannerType;
import com.cyq.app.constant.ClickType;
import com.cyq.app.entity.AppBanner;
import com.cyq.app.entity.AppClickLog;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppBannerService;
import com.cyq.app.service.AppClickLogService;
import com.cyq.app.service.PushOrderService;
import com.cyq.lottery.entity.LotteryTypeConfig;
import com.cyq.lottery.service.LotteryTypeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * app首页banner表 前端控制器
 * </p>
 */
@RestController
@RequestMapping
public class IndexController extends ApiController {
    @Autowired
    AppBannerService appBannerService;
    @Autowired
    LotteryTypeConfigService lotteryTypeConfigService;
    @Autowired
    AppClickLogService appClickLogService;
    @Autowired
    PushOrderService pushOrderService;

    /**
     * 发现标签加载的数据
     */
    @GetMapping("/homepage")
    public R<Map<String, Object>> homepage() {
        Map<String, Object> result = new HashMap<>();
        List<AppBanner> bannerList = appBannerService.selectAppBannerList(1,0,BannerType.ZI_XUN.getValue());
        List<LotteryTypeConfig> lotteryConfigList = lotteryTypeConfigService.list(Wrappers.<LotteryTypeConfig>lambdaQuery()
                .eq(LotteryTypeConfig::getSaleEnabled, 1)
                .orderByDesc(LotteryTypeConfig::getWeight));
        List<Map<String, Object>> collect = lotteryConfigList.stream().map(config -> {
            Map<String, Object> m = new HashMap<>();
            m.put("lotteryType", config.getLotteryType());
            m.put("lotteryIcon", config.getLotteryIcon());
            m.put("saleEnabled", config.getSaleEnabled());
            m.put("redText", config.getRedText());
            m.put("grayText", config.getGrayText());
            return m;
        }).collect(Collectors.toList());
        for (AppBanner appBanner:bannerList){
            if (appBanner.getJumpType().equals(1)){
                appBanner.setHref(null);
            }
        }
        result.put("bannerList", bannerList);
        result.put("lotteryConfigList", collect);
        List<Map<String,Object>> pushOrderList = pushOrderService.getPushOrderList(1,Integer.MAX_VALUE,0);
        if (pushOrderList.size()==0){
            pushOrderList = pushOrderService.getPushOrderList(1,Integer.MAX_VALUE,1);
        }
        result.put("pushOrderList",pushOrderList);
        return success(result);
    }


    /**
     * banner点击
     */
    @PostMapping("/clickBanner")
    public R<String> clickBanner(@RequestParam Integer bannerId) {
        Integer userId = RequestContext.getUserId();
        //添加点击记录
        AppClickLog appClickLog = appClickLogService.getOne(Wrappers.<AppClickLog>lambdaQuery().eq(AppClickLog::getUserId, userId)
                .eq(AppClickLog::getClickId, bannerId).eq(AppClickLog::getType, ClickType.BANNER.getValue()));
        if (appClickLog == null) {
            appClickLog = new AppClickLog();
            appClickLog.setUserId(userId);
            appClickLog.setClickId(Integer.valueOf(String.valueOf(bannerId)));
            appClickLog.setType(ClickType.BANNER.getValue());
            appClickLogService.save(appClickLog);
        }
        return success("ok");
    }


}
