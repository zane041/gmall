package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 检索列表接口
 * </p>
 *
 */
@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;


    // 前端传参形式。GET请求参数
    /**
     * 门户页面中商品检索页面渲染
     *
     * @param searchParam
     * @param model
     * @return
     */
    @GetMapping("/list.html")
    public String search(SearchParam searchParam, Model model) {
        Result<Map> result = listFeignClient.list(searchParam); // 返回Object这里用Map接收
        model.addAllAttributes(result.getData());
        // 返回用户根据哪些条件进行检索的url字段 —— urlParam 字段
        String urlParam = makeUrlParam(searchParam);
        model.addAttribute("urlParam", urlParam);
        // 返回检索条件
        model.addAttribute("searchParam", searchParam);
        // 品牌面包屑
        String trademarkParam = makeTradeMarkParam(searchParam.getTrademark());
        model.addAttribute("trademarkParam", trademarkParam);
        // 平台属性面包屑
        List<SearchAttr> propsParamList = makePropsParamList(searchParam.getProps());
        model.addAttribute("propsParamList", propsParamList);
        // 排序处理
        Map orderMap = makeOrderMap(searchParam.getOrder());
        model.addAttribute("orderMap", orderMap);
        return "list/index";
    }

    /**
     * 排序规则
     * @param order 如 2:asc
     * @return
     */
    private Map makeOrderMap(String order) {
        Map result = new HashMap();
        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split(":");
            if (split.length == 2) {
                result.put("type", split[0]);
                result.put("sort", split[1]);
                return result;
            }
        }
        // 设置默认排序规则
        result.put("type", "1");
        result.put("sort", "desc");
        return result;
    }

    /**
     * 制作平台属性面包屑
     * @param props 如[23:8G:运行内存,..]
     * @return
     */
    private List<SearchAttr> makePropsParamList(String[] props) {
        List<SearchAttr> searchAttrList = new ArrayList<>();
        if (props != null) {
            for (String prop : props) {
                String[] split = prop.split(":");
                if (split.length == 3) {
                    SearchAttr searchAttr = new SearchAttr();
                    searchAttr.setAttrId(Long.parseLong(split[0]));
                    searchAttr.setAttrName(split[2]);
                    searchAttr.setAttrValue(split[1]);
                    searchAttrList.add(searchAttr);
                }
            }
        }
        return searchAttrList;
    }

    /**
     * 制作品牌面包屑
     * @param trademark 如 1:小米
     * @return
     */
    private String makeTradeMarkParam(String trademark) {
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = trademark.split(":");
            if (split.length == 2) return "品牌: " + split[1];
        }
        return "";
    }

    /**
     * 记录用户检索的url
     * @param searchParam
     * @return
     */
    private String makeUrlParam(SearchParam searchParam) {
        // 需要达到的效果：如 list.html?category3Id=61&order=1:asc

        StringBuffer url = new StringBuffer();

        // 判断入口 —— 是否根据分类ID检索
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            url.append("category1Id=").append(searchParam.getCategory1Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            url.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            url.append("category3Id=").append(searchParam.getCategory3Id());
        }

        // 判断入口 —— 是否根据关键词检索
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            url.append("keyword=").append(searchParam.getKeyword());
        }

        // 是否根据品牌检索
        if (!StringUtils.isEmpty(searchParam.getTrademark())) {
            // 已知入口只有两个（分类ID和关键词），当且仅当通过url拼了前面的条件，后面的这些条件才能拼
            if (url.length() > 0) url.append("&trademark=").append(searchParam.getTrademark());
        }

        // 是否根据平台属性检索
        String[] props = searchParam.getProps();
        if (props != null) {
            for (String prop : props) {
                if (url.length() > 0) {
                    url.append("&props=").append(prop);
                }
            }
        }

        return "list.html?" + url;
    }
}
