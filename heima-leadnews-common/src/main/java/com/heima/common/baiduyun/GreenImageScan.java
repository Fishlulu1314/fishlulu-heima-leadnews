package com.heima.common.baiduyun;

import com.baidu.aip.contentcensor.AipContentCensor;
import com.heima.common.constants.BaiduyunCencorConstants;
import com.heima.common.constants.ContentCencorConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GreenImageScan {
    @Autowired
    private AipContentCensor aipContentCensor;

    public Map imageScan(List<byte[]> imageList) throws Exception{
        if (imageList == null || imageList.isEmpty()) {
            return null;
        }
        List<JSONObject> resList = new ArrayList<>();
        imageList.forEach(image -> {
            JSONObject response = aipContentCensor.imageCensorUserDefined(image, null);
            resList.add(response);
        });
        return imageScanResHandler(resList);
    }
    private Map imageScanResHandler(List<JSONObject> resList){

        Map<String, Object> resMap = new HashMap<>();
        //遍历resList，如果有一张图片被识别为违规图片，则返回违规信息
        for (JSONObject res : resList) {
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

    }
        return resMap;
    }
}
