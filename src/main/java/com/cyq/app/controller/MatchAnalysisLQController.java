package com.cyq.app.controller;


import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.lottery.service.MatchAnalysisLQService;
import com.cyq.lottery.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/jclqDetail")
public class MatchAnalysisLQController extends ApiController {

    @Autowired
    MatchAnalysisLQService matchAnalysisLQService;

    /**
     * 竞彩篮球比赛详情
     *
     * @param fId
     * @return
     */
    @GetMapping
    public R<JclqRaceInfoVO> getInfo(@RequestParam("fId") Integer fId) {
        return success(matchAnalysisLQService.getInfo(fId));
    }

    /**
     * 战绩---联赛积分排名
     *
     * @param fId
     * @return
     */
    @GetMapping("/ranking")
    public R<List<JclqLeaguePartVO>> getRanking(@RequestParam("fId") Integer fId) {
        return success(matchAnalysisLQService.getRanking(fId));
    }

    /**
     * 战绩---近期交战（近6场）
     *
     * @param homeId
     * @param awayId
     * @return
     */
    @GetMapping("/recentAgainst")
    public R<JclqRecentAgainstVO> getRecentAgainst(@RequestParam("homeId") Integer homeId,
                                                   @RequestParam("awayId") Integer awayId) {
        return success(matchAnalysisLQService.getRecentAgainst(homeId, awayId));
    }

    /**
     * 战绩---近期战绩（近10场）
     *
     * @param homeId
     * @param awayId
     * @return
     */
    @GetMapping("/recentGame")
    public R<Map<String, JclqRecentAgainstVO>> getRecentGame(@RequestParam("homeId") Integer homeId,
                                                             @RequestParam("awayId") Integer awayId) {
        Map<String, JclqRecentAgainstVO> map = new HashMap<>();
        JclqRecentAgainstVO home = matchAnalysisLQService.getRecentGame(homeId);
        map.put("homeList", home);
        JclqRecentAgainstVO away = matchAnalysisLQService.getRecentGame(awayId);
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
        log.info("篮球未来赛事（3场）");
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> home = matchAnalysisLQService.getFuturityRace(homeId);
        map.put("homeList", home);
        Map<String, Object> away = matchAnalysisLQService.getFuturityRace(awayId);
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
    public R<List<JclqAllMatchVO>> getAgenda(@RequestParam("teamId") Integer teamId) {
        return success(matchAnalysisLQService.getAgenda(teamId));
    }

    /**
     * 阵容---数据王
     *
     * @param fId
     * @return
     */
    @GetMapping("/datace")
    public R<List<JclqDataceVO>> getDatace(@RequestParam("fId") Integer fId) {
        return success(matchAnalysisLQService.getDatace(fId));
    }

    /**
     * 阵容---球队成员数据
     *
     * @param fId
     * @return
     */
    @GetMapping("/teamData")
    public R<List<JclqMemberPositionVO>> getTeamData(@RequestParam("fId") Integer fId, @RequestParam("teamId") Integer teamId) {
        return success(matchAnalysisLQService.getTeamData(fId, teamId));
    }

}
