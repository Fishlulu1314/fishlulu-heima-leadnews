package com.heima.common.baiduyun;

import com.baidu.aip.contentcensor.AipContentCensor;
import com.heima.common.constants.BaiduyunCencorConstants;
import com.heima.common.constants.ContentCencorConstants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GreenTextScan {
    @Autowired
    private AipContentCensor aipContentCensor;

    public Map greeTextScan(String content) throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        JSONObject response = aipContentCensor.textCensorUserDefined(content);
        return textScanResHandler(response);
    }
    private Map textScanResHandler(JSONObject res){
        Map<String, Object> resMap = new HashMap<>();
        switch (res.getInt("conclusionType")) {
            case BaiduyunCencorConstants.CONCLUSION_TYPE_LEGAL:
                break;
            case BaiduyunCencorConstants.CONCLUSION_TYPE_ILLEGAL:
                resMap.put(ContentCencorConstants.SUGGESTION, ContentCencorConstants.SUGGESTION_BLOCK);
                return resMap;
            case BaiduyunCencorConstants.CONCLUSION_TYPE_SUSPECTED:
                resMap.put(ContentCencorConstants.SUGGESTION, ContentCencorConstants.SUGGESTION_REVIEW);
                return resMap;
            case BaiduyunCencorConstants.CONCLUSION_TYPE_REVIEW_FAILED:
                log.error("百度云图片审核失败,error_code:{},error_msg:{}", res.getInt("error_code"), res.getString("error_msg"));
                resMap.put(ContentCencorConstants.SUGGESTION, ContentCencorConstants.SUGGESTION_REVIEW);
                return resMap;
            default:
                log.error("百度云图片审核未知错误");
                resMap.put(ContentCencorConstants.SUGGESTION, ContentCencorConstants.SUGGESTION_REVIEW);
        }
        resMap.put(ContentCencorConstants.SUGGESTION, ContentCencorConstants.SUGGESTION_PASS);
        return resMap;
    }



}
