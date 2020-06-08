package com.cyq.app.controller;

import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppFansService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * <p>
 * 粉丝
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/fans")
public class AppFansController extends ApiController {

    @Autowired
    private AppFansService appFansService;

    /**
     * 关注或者取消
     * @param userId
     * @param type 1:关注 2:取消
     * @return
     */
    @PostMapping("/followOrNot")
    public R<String> followOrNot(@RequestParam Integer userId,@RequestParam(required = false) Integer type){
        appFansService.follow(userId,RequestContext.getUserId(),type);
        return success("Ok");
    }

    /**
     * 我的关注列表
     */
    @GetMapping("/myFollowList")
    public R<List<Map<String,Object>>> followList(@RequestParam Integer pageNum
            ,@RequestParam Integer pageSize) throws ExecutionException {
        return success(appFansService.followList(RequestContext.getUserId(),pageNum,pageSize));

    }

}
