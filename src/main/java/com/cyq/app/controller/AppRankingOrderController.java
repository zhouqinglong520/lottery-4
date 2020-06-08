package com.cyq.app.controller;


import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.app.entity.AppOrder;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppRankingOrderJoinService;
import com.cyq.app.service.AppRankingOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/rankingOrder")
public class AppRankingOrderController extends ApiController {

    @Autowired
    private AppRankingOrderService appRankingOrderService;

    @Autowired
    private AppRankingOrderJoinService appRankingOrderJoinService;

    /** 
    * @Description: 创建榜单 
    * @Param:  
    * @return:  
    * @Author: shaz 
    * @Date: 2019/8/1 
    */ 
    @PostMapping("/create")
    public R<String> create(@RequestParam("stakeNumber") String stakeNumber,
                            @RequestParam("lotteryTypeNo") Integer lotteryTypeNo,
                            @RequestParam("betTimes") Integer betTimes,
                            @RequestParam("amount") int amount,
                            @RequestParam("phase") String phase,
                            @RequestParam(required = false,defaultValue = "当前大神未填写推荐理由，点击弹幕区与其他用户互动吧") String description){

        Integer userId = RequestContext.getUserId();
        AppOrder appOrder = appRankingOrderService.create(userId,stakeNumber,lotteryTypeNo,phase,betTimes,amount,description);
        return success(appOrder.getOrderId());
    }


    /** 
    * @Description: 参与榜单 
    * @Param:  
    * @return:  
    * @Author: shaz 
    * @Date: 2019/8/1 
    */ 
    @PostMapping("/join")
    public R<String> join(@RequestParam Integer rankingOrderId,
                          @RequestParam("stakeNumber") String stakeNumber,
                          @RequestParam("lotteryTypeNo") Integer lotteryTypeNo,
                          @RequestParam("betTimes") Integer betTimes,
                          @RequestParam("amount") int amount,
                          @RequestParam("phase") String phase){
        Integer userId = RequestContext.getUserId();
        AppOrder appOrder= appRankingOrderJoinService.join(userId,stakeNumber,lotteryTypeNo,phase,betTimes,amount,rankingOrderId);
        return success(appOrder.getOrderId());
    }


    /** 
    * @Description: 榜单详情 
    * @Param:  
    * @return:  
    * @Author: shaz 
    * @Date: 2019/8/1 
    */ 
    @GetMapping("/detail")
    public R<Map<String,Object>> detail(@RequestParam Integer rankingOrderId){
        return success(appRankingOrderService.detail(rankingOrderId,RequestContext.getUserId(),1,null));
    }


    /**
     * 榜单排行榜
     * @param
     * @param sortType 1:红榜 2:黑榜
     * @param type 1:包含弹幕的列表 2:不包含弹幕的列表
     * @return
     * @throws Exception
     */
    @GetMapping("/getBangDanList")
    public R<List<Map<String,Object>>> getBangDanList(
            @RequestParam Integer pageNum,@RequestParam Integer pageSize,
            @RequestParam Integer sortType,@RequestParam(required = false,defaultValue = "1") Integer type) {
        return success(appRankingOrderService.getBangdanByPage(pageNum,pageSize,RequestContext.getUserId(),sortType,type));
    }


    /** 
    * @Description: 榜单评价 
    * @Param:
    * @return:  
    * @Author: shaz 
    * @Date: 2019/8/1 
    */ 
    @GetMapping("/getRankingOrderComment")
    public R<List<Map<String,Object>>>  getRankingOrderComment(@RequestParam Integer rankingOrderId){
        return success(appRankingOrderService.getRankingOrderComment(rankingOrderId,RequestContext.getUserId()));
    }


}
