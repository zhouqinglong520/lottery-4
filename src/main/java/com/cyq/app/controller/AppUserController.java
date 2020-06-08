package com.cyq.app.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.cyq.app.TIM.IMRemoteApi;
import com.cyq.app.TIM.service.IMService;
import com.cyq.app.constant.AppConsts;
import com.cyq.app.dto.AppUserDTO;
import com.cyq.app.entity.AppUser;
import com.cyq.app.entity.StoreMemberInfo;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppLoginLogService;
import com.cyq.app.service.AppUserService;
import com.cyq.app.service.CommonService;
import com.cyq.app.service.StoreMemberInfoService;
import com.cyq.common.cache.RedisService;
import com.cyq.common.constant.YesNoStatus;
import com.cyq.common.utils.ValidatorUtils;
import com.cyq.common.web.exception.errorcode.ApiErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/user")
public class AppUserController extends ApiController {
    @Value("${SMS.enable}")
    boolean smsEnable;
    @Autowired
    CommonService commonService;
    @Autowired
    AppLoginLogService appLoginLogService;
    @Autowired
    AppUserService appUserService;
    @Autowired
    RedisService redisService;
    @Autowired
    StoreMemberInfoService storeMemberInfoService;
    @Autowired
    IMRemoteApi imRemoteApi;
    @Autowired
    IMService imService;

    /**
     * 手机号验证码登录
     *
     * @param mobile   手机号
     * @param randCode 随机码
     * @return
     * @throws Exception
     */
    @PostMapping("/login")
    @Transactional
    public R<Map<String, Object>> login(
            @NotBlank @Pattern(regexp = ValidatorUtils.mobileValidExp, message = "手机号码格式错误") String mobile,
            @NotBlank @RequestParam("randCode") String randCode) throws Exception {
        if (smsEnable) {// 验证随机码
            if (!commonService.verifySecurityCode(mobile, randCode)) {
                throw new ApiException(ApiErrorCode.VERIFY_CODE_NOT_MATCH);
            }
        }
        //加锁5秒,防止用户刻意同时提交相同手机号注册
        boolean lock = redisService.lock("phone"+mobile,5*1000);
        Map<String, Object> map=new HashMap<>();
        if (lock){
            map=appUserService.findOrCreate(mobile, null);
        }

        return success(map);
    }

    /**
     * 根据已有账号密码登录
     *
     * @param mobile   手机号
     * @param password 随机码
     * @return
     * @throws Exception
     */
    @PostMapping("/loginByPassword")
    @Transactional
    public R<Map<String, Object>> loginByPasswd(
            @NotBlank @Pattern(regexp = ValidatorUtils.mobileValidExp, message = "手机号码格式错误") String mobile,
            @NotBlank @RequestParam("password") String password) throws Exception {
        return success(appUserService.loginByPasswd(mobile, password));
    }

    /**
     * 发送短信验证码
     *
     * @param mobile
     * @return
     * @throws Exception
     */
    @PostMapping("/randCode")
    public R<Boolean> getRandCode(@NotBlank @Pattern(regexp = ValidatorUtils.mobileValidExp, message = "手机号码格式错误") String mobile) throws Exception {
        if (smsEnable) {
            return success(commonService.sendSecurityCode(mobile));
        }
        return success(true);
    }

    /**
     * 查询登陆用户个人信息
     *
     * @return
     * @throws Exception
     */
    @GetMapping("/userinfo")
    public R<Map<String, Object>> userinfo() throws Exception {
        // 返回信息
        Map<String, Object> result = new HashMap<>();
        Integer userId = RequestContext.getUserId();
        AppUser appUser = appUserService.getById(userId);
        result.put("nickName", appUser.getNickName());
        result.put("sex", appUser.getSex() != null ? (appUser.getSex() == 1 ? "男" : appUser.getSex() == 2 ? "女" : "不详") : null);
        result.put("mobile", appUser.getMobile());
        result.put("avatar", appUser.getAvatar());
        result.put("birthday", appUser.getBirth() != null ? appUser.getBirth().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
        result.put("reliability", appUser.getReliability());
        result.put("inviteCode", appUser.getInviteCode());
        result.put("recharge",appUser.getRecharge());
        result.put("selfSignature", appUser.getSelfSignature());
        result.put("passwordIsset", StringUtils.isBlank(appUser.getPassword()) ? false : true);
        result.put("assetPasswordIsset", StringUtils.isBlank(appUser.getAssetPassword()) ? false : true);
        // 查询未读消息
        result.put("unreadCount", appUserService.countByIsRead(userId, YesNoStatus.NO.getValue()));
        // 查询是否设置收款方式
        result.put("isUpload", !appUserService.qrcodeList(userId).isEmpty());    // 是否上传收款方式
        //获取当前绑定销售
        StoreMemberInfo storeMemberInfo = storeMemberInfoService.getOne(Wrappers.<StoreMemberInfo>lambdaQuery().eq(StoreMemberInfo::getInviteCode
                , StringUtils.isBlank(appUser.getInviteCode())?AppConsts.DEFAULT_INVITE_CODE:appUser.getInviteCode()));
        result.put("sellerId",storeMemberInfo.getAppMemId());
        return success(result);
    }

    /**
     * 修改个人信息并同步修改IM个人信息
     *
     * @param appUserDTO
     * @return
     * @throws Exception
     */
    @PostMapping("/updateUserInfo")
    public R<String> updateMemInfo(AppUserDTO appUserDTO) throws Exception {
        Integer userId = RequestContext.getUserId();
        AppUser oldAppUser = appUserService.getById(userId);
        //如果修改了昵称
        if (!oldAppUser.getNickName().equals(appUserDTO.getNickName())){
            //判断昵称是否已存在
            List<AppUser> appUserList = appUserService.list(Wrappers.<AppUser>lambdaQuery()
                    .eq(AppUser::getNickName,appUserDTO.getNickName())
                    .eq(AppUser::getRegisterType,0));
            if (appUserList!=null&&appUserList.size()>0){
                throw new ApiException("昵称已存在");
            }
        }
        // 准备入参
        boolean update = appUserService.update(Wrappers.<AppUser>lambdaUpdate()
                .set(StringUtils.isNotEmpty(appUserDTO.getNickName()), AppUser::getNickName, appUserDTO.getNickName())
                .set(appUserDTO.getSex() != null, AppUser::getSex, appUserDTO.getSex())
                .set(StringUtils.isNotEmpty(appUserDTO.getSelfSignature()), AppUser::getSelfSignature, appUserDTO.getSelfSignature())
                .eq(AppUser::getId, userId));
        JSONObject param = new JSONObject();
        param.put("userId", userId);
        param.put("nickName", appUserDTO.getNickName());
        param.put("sex", appUserDTO.getSex());
        param.put("selfSignature", appUserDTO.getSelfSignature());
        imRemoteApi.portraitSet(param);
        return success(update ? "修改成功" : "修改失败");
    }

    /**
     * 设置登录密码
     *
     * @param password
     * @return
     * @throws Exception
     */
    @PostMapping("/setPassword")
    public R<String> setPassword(@NotBlank @RequestParam("password") String password) throws Exception {
        String regExp = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z_]{6,20}$";
        if (StringUtils.isEmpty(password) || !password.matches(regExp)) {
            throw new ApiException("密码格式有误，可以包含数字、字母、下划线，并且要同时含有数字和字母，且长度要在6-20位之间。");
        }
        boolean update = appUserService.update(Wrappers.<AppUser>lambdaUpdate()
                .set(AppUser::getPassword, DigestUtils.md5Hex(password))
                .eq(AppUser::getId, RequestContext.getUserId()));
        return success(update ? "密码设置成功！" : "设置失败");
    }

    /**
     * 设置资产密码
     *
     * @param assetPassword
     * @return
     * @throws Exception
     */
    @PostMapping("/setAssetPassword")
    public R<String> setAssetPassword(@NotBlank @RequestParam("assetPassword") String assetPassword) throws Exception {
        boolean update = appUserService.update(Wrappers.<AppUser>lambdaUpdate()
                .set(AppUser::getAssetPassword, DigestUtils.md5Hex(assetPassword))
                .eq(AppUser::getId, RequestContext.getUserId()));
        return success(update ? "设置成功" : "设置失败");
    }

    /**
     * 验证资产密码
     *
     * @param assetPassword
     * @return
     * @throws Exception
     */
    @PostMapping("/verifyAssetPassword")
    public R<String> verifyAssetPassword(@NotBlank @RequestParam("assetPassword") String assetPassword) throws Exception {
//        int allowCount = 3;
//        String keyPrefix = "retryLimitCache:";
        Integer userId = RequestContext.getUserId();
//        String key = keyPrefix + userId;
        AppUser appUser = appUserService.getById(userId);
//        AtomicInteger retryCount = redisService.get(key, AtomicInteger.class);
//        if (retryCount == null) {
//            retryCount = new AtomicInteger(0);
//            redisService.set(key, retryCount, 24 * 60 * 60L); // 锁定24小时
//        }
//        if (retryCount.incrementAndGet() > allowCount) {
//            //if retry count > 5 throw
//            log.warn("资产密码错误次数超过限制，24小时内将不得进行提现、添加银行卡、解绑银行卡等相关操作");
//            throw new ApiException("资产密码错误次数超过限制，24小时内将不得进行提现、添加银行卡、解绑银行卡等相关操作");
//        } else {
//            redisService.set(key, retryCount, 24 * 60 * 60L); // 锁定24小时
//        }
        boolean matches = DigestUtils.md5Hex(assetPassword).equals(appUser.getAssetPassword());
//        if (matches) {
//            redisService.delete(key);
//        } else {
//            log.warn("资产密码错误，还剩余" + (allowCount - retryCount.get()) + "次机会进行验证，验证失败24小时内将不得进行提现、添加银行卡、解绑银行卡等相关操作");
//            if (allowCount - retryCount.get() > 0) {
//                throw new ApiException("资产密码错误，还剩余" + (allowCount - retryCount.get()) + "次机会进行验证，验证失败24小时内将不得进行提现、添加银行卡、解绑银行卡等相关操作");
//            } else {
//                throw new ApiException("资产密码错误次数超过限制，24小时内将不得进行提现、添加银行卡、解绑银行卡等相关操作");
//            }
//        }
        if (matches){
            return success("验证通过");
        }else {
            throw new ApiException("验证失败");
        }

    }

    /**
     * 根据会员号从数据库中查询手机号后，然后下发短信，前端界面省去用户输入手机号的流程
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/randCodeByUserId")
    public R<Boolean> randCodeByMemId() throws Exception {
        if (smsEnable) {
            AppUser appUser = appUserService.getById(RequestContext.getUserId());
            boolean b = commonService.sendSecurityCode(appUser.getMobile());
            return success(b);
        } else {
            return success(true);
        }
    }

    /**
     * 根据memId来校验验证码
     *
     * @param randCode
     * @return
     * @throws Exception
     */
    @PostMapping("/verifyRandCodeByUserId")
    public R<Boolean> verifyRandCodeByUserId(@RequestParam("randCode") String randCode) throws Exception {
        if (smsEnable) {
            AppUser appUser = appUserService.getById(RequestContext.getUserId());
            if (!commonService.verifySecurityCode(appUser.getMobile(), randCode)) {
                throw new ApiException(ApiErrorCode.VERIFY_CODE_NOT_MATCH);
            }
        }
        return success(true);
    }

    /**
     * 修改用户手机号
     *
     * @param randCode
     * @return
     * @throws Exception
     */
    @PostMapping("/changeMobile")
    public R<String> verifySmsCode(
            @NotBlank @Pattern(regexp = ValidatorUtils.mobileValidExp, message = "手机号码格式错误") @RequestParam("mobile") String mobile,
            @NotBlank @RequestParam("randCode") String randCode) throws Exception {
        boolean success;
        if (smsEnable) {
            success = commonService.verifySecurityCode(mobile, randCode);
        } else {
            success = true;
        }
        if (!success) {
            throw new ApiException(ApiErrorCode.VERIFY_CODE_NOT_MATCH);
        }
        // 判断该手机号是否被占用，占用了则无法变更
        if (appUserService.findByMobile(mobile) != null) {
            throw new ApiException("该手机号已被占用！");
        }
        boolean update = appUserService.update(Wrappers.<AppUser>lambdaUpdate()
                .set(AppUser::getMobile, mobile).
                        eq(AppUser::getId, RequestContext.getUserId()));
        return success(update ? "修改成功" : "修改失败");
    }

    /**
     * 绑定邀请码
     *
     * @param inviteCode
     * @return
     * @throws Exception
     */
    @PostMapping("/bindInviteCode")
    public R<Boolean> bindInviteCode(@NotBlank @RequestParam("inviteCode") String inviteCode) throws Exception {
        StoreMemberInfo storeMemberInfo = storeMemberInfoService.getOne(Wrappers.<StoreMemberInfo>lambdaQuery().eq(StoreMemberInfo::getInviteCode, inviteCode));

        if (storeMemberInfo == null) {
            throw new ApiException(ApiErrorCode.INVITATION_CODE);
        }
        boolean update = appUserService.update(Wrappers.<AppUser>lambdaUpdate()
                .set(AppUser::getInviteCode, inviteCode)
                .eq(AppUser::getId, RequestContext.getUserId()));


        try {
            imRemoteApi.friendAdd(String.valueOf(RequestContext.getUserId()),String.valueOf(storeMemberInfo.getAppMemId()),1);
            imService.sendMsg(String.valueOf(storeMemberInfo.getAppMemId()),String.valueOf(RequestContext.getUserId()), "我已成为您的专属顾问，很高兴为您服务");
        }catch (Exception e){
            log.error(e.getMessage());
        }

        return success(update);
    }

    /**
     * 联系人工充值
     *
     * @return
     */
    @GetMapping("/customerService")
    public R<Integer> customerService(){
        Integer userId = RequestContext.getUserId();
        return success(appUserService.customerService(userId));
    }

    /**
     * 获取收款二维码
     *
     * @return
     */
    @GetMapping("/get/incomeCode")
    public R<List<Map<String,Object>>>  getIncomeCode(@RequestParam("memId") Integer memId){
        return success(appUserService.getIncomeCode(memId));
    }
}