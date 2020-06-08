package com.cyq.app.controller;

import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.cyq.common.dto.PageDTO;
import com.cyq.common.web.exception.errorcode.ApiErrorCode;
import com.cyq.lottery.constant.PhaseStatus;
import com.cyq.lottery.dto.DrawResult;
import com.cyq.lottery.entity.LotteryPhase;
import com.cyq.lottery.service.LotteryPhaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/phase")
public class LotteryPhaseController extends ApiController {

    @Autowired
    LotteryPhaseService lotteryPhaseService;

    /**
     * 获取彩种当前期(待优化，放缓存)
     *
     * @param lotteryType 彩种编号
     * @return
     */
    @GetMapping("/getCurrent")
    public R<Map<String, Object>> getCurrent(
            @RequestParam("lotteryType") Integer lotteryType) {
        LotteryPhase lotteryPhase = lotteryPhaseService.getCurrent(lotteryType);
        if (Objects.isNull(lotteryPhase)) {
            throw new ApiException(ApiErrorCode.NO_PHASE);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("phase", lotteryPhase.getPhase());
        map.put("endSaleTime", lotteryPhase.getSaleEndTime());
        map.put("drawTime", lotteryPhase.getActualDrawTime());
        return success(map);
    }

    /**
     * 获取所有彩种当前期（待优化，放缓存）
     *
     * @return
     */
    @GetMapping("/getCurrentList")
    public R<List<LotteryPhase>> getCurrentList() {
        return success(lotteryPhaseService.getCurrentList());
    }

    /**
     * 查询所有彩种上一期的开奖详情
     *
     * @return
     */
    @GetMapping("/getAllDrawResult")
    public R<List<DrawResult>> getWinDetails() {
        return success(lotteryPhaseService.getAllDrawingDetail());
    }

    /**
     * 查询某彩种上次开奖记录
     *
     * @param lotteryType 彩种
     * @return
     */
    @GetMapping("/getLastDrawResult")
    public R<DrawResult> getLastDrawingDetail(@RequestParam("lotteryType") int lotteryType) {
        return success(lotteryPhaseService.getLastDrawingDetail(lotteryType));
    }

    /**
     * 根据彩种和期号获取开奖号码
     *
     * @param lotteryType 彩种
     * @param phase       期号
     * @return
     */
    @GetMapping("/getDrawResult")
    public R<DrawResult> getPhase(
            @RequestParam("lotteryType") int lotteryType,
            @RequestParam("phase") String phase) {
        return success(lotteryPhaseService.findDrawResultByTypeAndPhase(lotteryType, phase));
    }

    /**
     * 根据彩种和期号获取对阵详情
     *
     * @param lotteryType 彩种
     * @param phase       期号
     * @return
     */
    @GetMapping("/getAgainstDetails")
    public R<List<Map<String, Object>>> getAgainstDetails(
            @RequestParam("lotteryType") int lotteryType,
            @RequestParam("phase") String phase) {
        return success(lotteryPhaseService.getAgainstDetails(lotteryType, phase));
    }

    /**
     * 分页获取某彩种开奖记录
     *
     * @param lotteryType 彩种
     * @param myPage      分页信息
     * @return
     */
    @GetMapping("/historyDrawResult")
    public R<List<DrawResult>> historyRecord(
            @RequestParam("lotteryType") int lotteryType,
            PageDTO myPage) {
        return success(lotteryPhaseService.historyDrawResult(lotteryType, myPage));
    }

    /**
     * 获取足彩可用期号，足彩可售期有多个
     *
     * @param lotteryType
     * @return
     */
    @GetMapping("/getZcPhase")
    public R<List<LotteryPhase>> getZcPhase(@RequestParam("lotteryType") int lotteryType) {
        return success(lotteryPhaseService.getByLotteryTypeAndStatus(lotteryType, PhaseStatus.OPEN.getValue()));
    }

}
