package com.cyq.app.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyq.app.entity.AppMessage;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppMessageService;
import com.cyq.common.dto.PageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户消息中心 前端控制器
 * </p>
 *
 * @author noreply
 * @since 2019-02-18
 */
@RestController
@RequestMapping("/msg")
public class AppMessageController extends ApiController {

    @Autowired
    AppMessageService appMessageService;

    /**
     * 分页获取系统通知消息列表
     *
     * @param pageDTO
     * @return
     */
    @GetMapping("/msgList")
    public R<List<AppMessage>> msgList(PageDTO pageDTO) {
        Page<AppMessage> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        List<AppMessage> records = appMessageService.page(page, Wrappers.<AppMessage>lambdaQuery()
                .eq(AppMessage::getUserId, RequestContext.getUserId())
                .eq(AppMessage::getType,1)
                .orderByDesc(AppMessage::getCreateTime))
                .getRecords();
        return success(records);
    }

    /**
     * 标记消息为已读
     *
     * @param msgId
     * @return
     */
    @PostMapping("/markAsRead")
    public R<Boolean> markAsRead(@RequestParam("msgId") Integer msgId) {
        return success(appMessageService.markAsRead(msgId, RequestContext.getUserId()));
    }

    /**
     * 根据消息id，逻辑删除消息
     *
     * @param msgIds
     * @return
     */
    @PostMapping("deleteMsgById")
    public R<Boolean> deleteMsgById(@RequestParam("msgIds") List<String> msgIds) {
        return success(appMessageService.removeByIds(msgIds));
    }

    /**
     * 消息未读个数
     *
     * @return
     */
    @GetMapping("/unreadCount")
    public R<Integer> unreadCount() {
        return success(appMessageService.unreadCount(RequestContext.getUserId()));
    }

}
