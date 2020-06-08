package com.cyq.app.controller;


import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.lottery.service.MatchAnalysisZQService;
import com.cyq.lottery.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jczqDetail")
public class MatchAnalysisZQController extends ApiController {

    @Autowired
    MatchAnalysisZQService matchAnalysisZQService;

    /**
     * 竞彩足球比赛详情
     *
     * @param fId
     * @return
     */
    @GetMapping
    public R<JczqRaceInfoVO> getInfo(@RequestParam("fId") Integer fId) {
        return success(matchAnalysisZQService.getInfo(fId));
    }

    /**
     * 战绩---联赛积分排名
     *
     * @param fId
     * @return
     */
    @GetMapping("/ranking")
    public R<List<JczqLeaguePartVO>> getRanking(@RequestParam("fId") Integer fId) {
        return success(matchAnalysisZQService.getRanking(fId));
    }

    /**
     * 战绩---国籍积分排名
     *
     * @param fId
     * @return
     */
    @GetMapping("/nation/ranking")
    public R<List<JczqLeaguePartNationVO>> getNationRanking(@RequestParam("fId") Integer fId) {
        return success(matchAnalysisZQService.getNationRanking(fId));
    }

    /**
     * 战绩---近期交战（近6场）
     *
     * @param homeId
     * @param awayId
     * @return
     */
    @GetMapping("/recentAgainst")
    public R<JczqRecentAgainstVO> getRecentAgainst(@RequestParam("homeId") Integer homeId,
                                                   @RequestParam("awayId") Integer awayId) {
        return success(matchAnalysisZQService.getRecentAgainst(homeId, awayId));
    }

    /**
     * 战绩---近期战绩（近10场）
     *
     * @param homeId
     * @param awayId
     * @return
     */
    @GetMapping("/recentGame")
    public R<Map<String, JczqRecentAgainstVO>> getRecentGame(@RequestParam("homeId") Integer homeId,
                                                             @RequestParam("awayId") Integer awayId) {
        Map<String, JczqRecentAgainstVO> map = new HashMap<>();
        JczqRecentAgainstVO home = matchAnalysisZQService.getRecentGame(homeId);
        map.put("homeList", home);
        JczqRecentAgainstVO away = matchAnalysisZQService.getRecentGame(awayId);
        map.put("awayList", away);
        return success(map);
    }

    /**
     * 战绩---未来赛事（3场）
     *
     * @param homeId
     * @param awayId
     * @return
     */
    @GetMapping("/futurityRace")
    public R<Map<String, Object>> getFuturityRace(@RequestParam("homeId") Integer homeId,
                                                  @RequestParam("awayId") Integer awayId) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> home = matchAnalysisZQService.getFuturityRace(homeId);
        map.put("homeList", home);
        Map<String, Object> away = matchAnalysisZQService.getFuturityRace(awayId);
        map.put("awayList", away);
        return success(map);
    }

    /**
     * 赛程---本联赛所有赛程
     *
     * @param teamId
     * @return
     */
    @GetMapping("/agenda")
    public R<List<JczqAllMatchVO>> getAgenda(@RequestParam("teamId") Integer teamId) {
        return success(matchAnalysisZQService.getAgenda(teamId));
    }

    /**
     * 阵容---概览
     *
     * @param fId
     * @return
     */
    @GetMapping("/overview")
    public R<Map<String, Object>> getOverview(@RequestParam("fId") Integer fId) {
        return success(matchAnalysisZQService.getOverview(fId));
    }

    /**
     * 阵容---上场首发
     *
     * @param fId
     * @return
     */
    @GetMapping("/firstTeam")
    public R<Map<String, Object>> getFirstTeam(@RequestParam("fId") Integer fId) {
        return success(matchAnalysisZQService.getFirstTeam(fId));
    }

}
