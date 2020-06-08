package com.cyq.app.controller;


import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.common.cache.RedisService;
import com.cyq.lottery.helper.JclqRaceHelper;
import com.cyq.lottery.helper.JczqRaceHelper;
import com.cyq.lottery.helper.ZcRaceHelper;
import com.cyq.lottery.service.JclqRaceService;
import com.cyq.lottery.service.JczqRaceService;
import com.cyq.lottery.service.ZcRaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 竞彩足球，篮球对阵前端控制器
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/race")
public class RaceController extends ApiController {
    @Autowired
    JczqRaceService jczqRaceService;
    @Autowired
    JclqRaceService jclqRaceService;
    @Autowired
    ZcRaceService zcRaceService;
    @Autowired
    RedisService redisService;
    @Autowired
    JczqRaceHelper jczqRaceHelper;
    @Autowired
    JclqRaceHelper jclqRaceHelper;
    @Autowired
    ZcRaceHelper zcRaceHelper;

    /**
     * 获取竞彩足球对阵列表
     *
     * @param lotteryType 彩种
     * @param type        类型 1单关，2过关
     * @return
     */
    @GetMapping("/jczqList")
    public R<List<Map<String, Object>>> getJczqList(
            @RequestParam("lotteryType") int lotteryType,
            @RequestParam(value = "type", required = false, defaultValue = "2") int type) throws Exception {
        return success(jczqRaceHelper.buildData(lotteryType, type));
    }

    /**
     * 获取竞彩篮球对阵列表
     *
     * @param lotteryType 彩种
     * @param type        类型 1单关，2过关
     * @return
     */
    @GetMapping("/jclqList")
    public R<List<Map<String, Object>>> jclqList(
            @RequestParam("lotteryType") int lotteryType,
            @RequestParam(value = "type", required = false, defaultValue = "2") int type) throws Exception {
        return success(jclqRaceHelper.buildData(lotteryType, type));
    }

    /**
     * 传统足球对阵查询
     *
     * @param lotteryType 彩种
     * @param phase       期号
     */
    @GetMapping("/zcList")
    public R<Map<String, Object>> getZcList(
            @RequestParam("lotteryType") int lotteryType,
            @RequestParam(name = "phase", required = false) String phase) throws Exception {
        return success(zcRaceHelper.buildData(lotteryType, phase));
    }

}
