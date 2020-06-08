package com.cyq.app.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.lottery.entity.LotteryTypeConfig;
import com.cyq.lottery.service.LotteryTypeConfigService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 彩种配置 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/lottery")
public class LotteryTypeController extends ApiController {
    @Resource
    private LotteryTypeConfigService lotteryTypeConfigService;

    /**
     * 发现首页-所有彩种
     *
     * @return
     */
    @PostMapping("/list")
    public R<List<Map<String, Object>>> list() {
        List<LotteryTypeConfig> lotteryTypeConfigs = lotteryTypeConfigService.list(Wrappers.<LotteryTypeConfig>lambdaQuery()
                .eq(LotteryTypeConfig::getSaleEnabled, 1)
                .orderByDesc(LotteryTypeConfig::getWeight));
        List<Map<String, Object>> collect = lotteryTypeConfigs.stream().map(config -> {
            Map<String, Object> m = new HashMap<>();
            m.put("lotteryType", config.getLotteryType());
            m.put("lotteryIcon", config.getLotteryIcon());
            m.put("saleEnabled", config.getSaleEnabled());
            m.put("redText", config.getRedText());
            m.put("grayText", config.getGrayText());
            return m;
        }).collect(Collectors.toList());
        return success(collect);

    }
}
