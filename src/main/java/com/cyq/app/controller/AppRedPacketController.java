package com.cyq.app.controller;


import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.app.entity.AppRedPacketInfo;
import com.cyq.app.entity.AppUser;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppRedPacketInfoService;
import com.cyq.app.service.AppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 红包
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/redPacket")
public class AppRedPacketController extends ApiController {


    @Autowired
    private AppRedPacketInfoService appRedPacketInfoService;
    @Autowired
    private AppUserService appUserService;

    /**
     * 发布红包
     * @param totalAmount
     * @param num
     * @param description
     * @return
     */
    @PostMapping("/create")
    public R<String> create(@RequestParam Integer totalAmount,
                            @RequestParam Integer num,
                            @RequestParam Integer type,
                            @RequestParam(required = false) String description){
        appRedPacketInfoService.create(RequestContext.getUserId(),totalAmount,num,description,type);
        return success("Ok");
    }

    /**
     * 领取红包
     * @param redPacketId
     * @return
     */
    @PostMapping("/get")
    public R<Map<String,Object>> get(@RequestParam Integer redPacketId){
        Map<String,Object> re = appRedPacketInfoService.get(RequestContext.getUserId(),redPacketId);
        return success(re);
    }

    /**
     * 红包列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/list")
    public R<List<Map<String,Object>>> list(@RequestParam Integer pageNum
            ,@RequestParam Integer pageSize,@RequestParam(required = false) Integer userId){
        return success(appRedPacketInfoService.list(userId==null?RequestContext.getUserId():userId,pageNum,pageSize));
    }


    /**
     * 红包排行榜
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/ranking")
    public R<List<Map<String,Object>>> ranking(@RequestParam Integer pageNum
            ,@RequestParam Integer pageSize){
        return success(appRedPacketInfoService.ranking(pageNum,pageSize,RequestContext.getUserId()));
    }


    /**
     * 红包详情
     * @param redPacketId
     * @return
     */
    @GetMapping("/detail")
    public R<Map<String,Object>> ranking(@RequestParam Integer redPacketId){
        AppRedPacketInfo appRedPacketInfo = appRedPacketInfoService.getById(redPacketId);
        AppUser appUser = appUserService.getById(appRedPacketInfo.getUserId());
        Map<String,Object> re = new HashMap<>();
        re.put("userId",appRedPacketInfo.getUserId());
        re.put("description",appRedPacketInfo.getDescription());
        re.put("nickName",appUser.getNickName());
        re.put("avatar",appUser.getAvatar());
        re.put("type",appRedPacketInfo.getType());
        re.put("redPacketId",redPacketId);
        return success(re);
    }

}
