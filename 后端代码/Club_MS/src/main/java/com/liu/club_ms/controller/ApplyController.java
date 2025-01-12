package com.liu.club_ms.controller;

import com.github.pagehelper.PageInfo;
import com.liu.club_ms.jwt.JWUtil;
import com.liu.club_ms.model.*;
import com.liu.club_ms.service.ApplyService;
import com.liu.club_ms.util.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/apply")
public class ApplyController {
    @Resource
    ApplyService applyService;

    //  分页查询或高级查询入团申请详情
    @GetMapping("/queryApplyInfoList")
    public Response queryApplyInfoList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            String realName,
            ApplyInfo applyInfo,
            HttpServletRequest request){

        applyInfo.setUser(new User(realName));

        //通过token获取用户类型和id
        String token = request.getHeader("token");
        Integer type = JWUtil.getType(token);
        if(type == 1){
            Integer userId = JWUtil.getUserId(token);
            Club club = new Club();
            club.setCaptainId(userId);
            applyInfo.setClub(club);
        }

        PageInfo<ApplyInfo> applyInfoPageInfo =
                applyService.queryApplyInfoList(page, limit, applyInfo);
        long total = applyInfoPageInfo.getTotal();
        Response response = new Response();
        response.setData(applyInfoPageInfo.getList());
        response.setTotal(total);
        response.setCode(20000);
        return response;

    }

    // 通过入团申请详情ID查询入团审核记录
    @GetMapping("/queryApplyListByApplyInfoId")
    public Map<String, Object> queryApplyListById(Integer applyInfoId){
        Map<String, Object> response = new HashMap<>();
        List<ApplyList> applyLists = applyService.queryApplyListByApplyInfoId(applyInfoId);
        response.put("data", applyLists);
        response.put("code", 20000);
        return response;
    }

    // 用于审核入团申请
    @PostMapping("/judge")
    public Response judge(@RequestBody ApplyList applyList,
                            HttpServletRequest request) {
        String token = request.getHeader("token");
        Integer userId = JWUtil.getUserId(token);
        applyList.setAuthorId(userId);
        if (applyService.addApplyList(applyList) &&
                applyService.editApplyInfoStatusById(
                        applyList.getStatus(),
                        applyList.getApplyInfoId())){
            return Response.ok();
        }else {
            return Response.fail("审核入团申请失败");
        }
    }

    // 添加入团申请详情
    @PostMapping("/addApplyInfo")
    public Response addApplyInfo(@RequestBody ApplyInfo applyInfo,
                                 HttpServletRequest request){
        String token = request.getHeader("token");
        applyInfo.setUserId(JWUtil.getUserId(token));
        if(applyService.checkIsExitByUserIdAndClubId(applyInfo.getUserId(),
                applyInfo.getClubId())){
            return Response.ok("warning");
        }

        applyInfo.setStatus(0);

        if(applyService.addApplyInfo(applyInfo)){
            return Response.ok();
        }else {
            return Response.fail("添加入团申请失败");
        }
    }

    // 用于删除入团申请
    @DeleteMapping("/deleteById")
    public Response deleteById(Integer applyInfoId){
        if(applyService.deleteApplyListByApplyInfoId(applyInfoId) &&
                applyService.deleteApplyInfoById(applyInfoId)){
            return Response.ok();
        }else {
            return Response.fail("删除入团申请失败");
        }
    }

    // 通过用户ID查询入团申请记录
    @GetMapping("/getApplyInfoListByUserId")
    public Response getApplyInfoListByUserId(HttpServletRequest request){
        String token = request.getHeader("token");
        List<ApplyInfo> list = applyService.getApplyInfoListByUserId(JWUtil.getUserId(token));
        return Response.ok(list);
    }
}
