/**
 * @program school-bus-cloud
 * @description: BusController
 * @author: mf
 * @create: 2020/11/08 13:13
 */

package com.dream.bus.controller;

import cn.hutool.core.convert.Convert;
import com.dream.bus.bus.param.*;
import com.dream.bus.common.RedisUtils;
import com.dream.bus.constants.RedisConstants;
import com.dream.bus.constants.SbCode;
import com.dream.bus.param.CommonResponse;
import com.dream.bus.param.ResponseData;
import com.dream.bus.param.ResponseUtil;
import com.dream.bus.schedule.BusSchedule;
import com.dream.bus.service.IBusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/bus/")
public class BusController {

    @Autowired
    private IBusService busService;
    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private BusSchedule busSchedule;
    @GetMapping("getCount")
    public ResponseData getCount(CountPageInfo pageInfo) {
        try {
            String busStatus = pageInfo.getBusStatus();
            Long currentPage = pageInfo.getCurrentPage();
            String key = RedisConstants.COUNTS_EXPIRE.getKey() + busStatus + currentPage;
            if (redisUtils.hasKey(key)) {
                List<Object> objList = redisUtils.lGet(key, 0, -1);
                log.warn("getCount->redis\n");
                return new ResponseUtil().setData(objList);
            }

            PageCountRequest request = new PageCountRequest();
            request.setCurrentPage(pageInfo.getCurrentPage());
            request.setPageSize(pageInfo.getPageSize());
            request.setBusStatus(pageInfo.getBusStatus());
            PageCountResponse response = busService.getCount(request);
            if (!CollectionUtils.isEmpty(response.getCountSimpleDtos())) {
                redisUtils.lSet(key, response, RedisConstants.COUNTS_EXPIRE.getTime());
            }
            // 更新场次列表的页数缓存
            Long countPages = response.getPages();
            String countPagesKey = RedisConstants.COUNTS_PAGES_EXPIRE.getKey() + request.getBusStatus();
            // 肯定存在，就不判断了
            Long countPagesRedis = Convert.toLong(redisUtils.lGet(countPagesKey, 0, -1).get(0));
            if (!countPages.equals(countPagesRedis)) {
                // 如果不相等，更新
                redisUtils.lSet(countPagesKey, countPages); // 将response的总页数更新到缓存
            }
            log.warn("getCount\n");
            return new ResponseUtil().setData(response);
        } catch (Exception e) {
            log.error("getCount\n", e);
            CommonResponse response = new CommonResponse();
            response.setCode(SbCode.SYSTEM_ERROR.getCode());
            response.setMsg(SbCode.SYSTEM_ERROR.getMessage());
            return new ResponseUtil().setData(response);
        }
    }


    @GetMapping("getCountDetail")
    public ResponseData getCountDetail(Long countId) {
        // id 从本队缓存中取
        try {
            String key = RedisConstants.COUNT_DETAIL_EXPIRE.getKey()+countId;
            if (redisUtils.hasKey(key)) {
                Object obj = redisUtils.get(key);
                log.warn("getCountDetailById->redis\n");
                return new ResponseUtil().setData(obj);
            }
            CountDetailRequest request = new CountDetailRequest();
            request.setCountId(countId);
            CountDetailResponse response = busService.getCountDetailById(request);
            if (response.getCountDetailDto() != null) {
                redisUtils.set(key, response, RedisConstants.COUNT_DETAIL_EXPIRE.getTime());
            }
            log.warn("getCountDetailById\n");
            return new ResponseUtil().setData(response);
        } catch (Exception e) {
            log.error("getCountDetailById\n", e);
            CommonResponse response = new CommonResponse();
            response.setCode(SbCode.SYSTEM_ERROR.getCode());
            response.setMsg(SbCode.SYSTEM_ERROR.getMessage());
            return new ResponseUtil().setData(response);
        }
    }

    /**
     * 调用链路
     * @param seats
     * @param coundId
     * @return
     */
    @GetMapping("repeatSeats")
    public boolean repeatSeats(String seats, Long coundId) {
        try {
            return busService.repeatSeats(seats, coundId);
        } catch (Exception e) {
            log.error("repeatSeats\n", e);
            return false;
        }
    }

    @GetMapping("addSeats")
    public boolean addSeats(String seats, Long coundId) {
        try {
            return busService.addSeats(seats, coundId);
        } catch (Exception e) {
            log.error("repeatSeats\n", e);
            return false;
        }
    }

    @GetMapping("filterRepeatSeats")
    public boolean filterRepeatSeats(String seats, Long coundId) {
        try {
            return busService.filterRepeatSeats(seats, coundId);
        } catch (Exception e) {
            log.error("repeatSeats\n", e);
            return false;
        }
    }

    /**
     * 手动触发每天班次
     * @return
     */
    @GetMapping("addCounts")
    public boolean addCounts() {
        try {
            busSchedule.addCounts();
            return true;
        } catch (Exception e) {
            log.error("repeatSeats\n", e);
            return false;
        }
    }

}
