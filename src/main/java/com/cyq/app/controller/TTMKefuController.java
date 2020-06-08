package com.cyq.app.controller;

import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.KefuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * TTM 客服前端控制器
 * </p>
 */
@RestController
@RequestMapping("/TTM/kefu")
public class TTMKefuController extends ApiController {

    @Autowired
    KefuService kefuService;

    /**
     * 客服跳转链接
     */
    @RequestMapping("/jumplink")
    public R<String> jumplink() throws Exception {
        Integer userId = RequestContext.getUserId();
        return success(kefuService.jumplink(userId));
    }

    /**
     * 客服聊天窗口
     */
    @RequestMapping("/jumpchat")
    public R<String> jumpchat(@RequestParam("ttmOrderId") String ttmOrderId) throws Exception {
        Integer userId = RequestContext.getUserId();
        return success(kefuService.jumpchat(ttmOrderId));
    }

}
