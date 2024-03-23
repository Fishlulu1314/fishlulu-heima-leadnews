package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.baiduyun.GreenImageScan;
import com.heima.common.baiduyun.GreenTextScan;
import com.heima.common.constants.ContentCencorConstants;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNews.Status;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {
    @Autowired
    private WmNewsMapper wmNewsMapper;
    @Autowired
    private GreenTextScan greenTextScan;
    @Autowired
    private GreenImageScan greenImageScan;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    @Qualifier("com.heima.apis.article.IArticleClient")
    private IArticleClient articleClient;
    @Autowired
    private WmChannelMapper wmChannelMapper;
    @Autowired
    private WmUserMapper wmUserMapper;
    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;
    @Autowired
    private Tess4jClient tess4jClient;

    /**
     * 自媒体文章审核
     *
     * @param id 自媒体文章id
     */
    @Override
    @Async //异步处理
    public void autoScanWmNews(Integer id) {
        System.out.println("自媒体文章审核开始,文章id:"+id);

        //查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new RuntimeException(this.getClass().getName()+ "自媒体文章不存在");
        }
        //提取内容和图片
        Map<String,Object> textAndImages =  handleTextAndImages(wmNews);
        //自管理的敏感词过滤
        boolean isSensitive = handleSensitiveScan((String)textAndImages.get("content"), wmNews);
        if(!isSensitive){
            log.warn("自媒体文章自定义敏感词审核失败,文章id:{}",id);
            return;
        }
        //自动审核
        //审核文本内容 百度云接口
        boolean isTextScan = handleTextScan(textAndImages.get("content").toString(), wmNews);
        if(!isTextScan){
            log.warn("自媒体文章文本审核失败,文章id:{}",id);
            return;
        }
        //审核图片  百度云接口
        boolean isImageScan = handleImageScan((List<String>) textAndImages.get("images"), wmNews);
        if (!isImageScan) {
            log.warn("自媒体文章图片审核失败,文章id:{}", id);
            return;
        }
        //审核通过,保存app端的相关文章数据
        ResponseResult responseResult = saveAppArticle(wmNews);
        if(responseResult == null || !responseResult.getCode().equals(200)){
            throw new RuntimeException(this.getClass().getName()+ "文章审核,保存app端文章数据失败");
        }
        //回填文章id
        wmNews.setArticleId((Long) responseResult.getData());
        updateWmNewsStatus(wmNews, Status.PUBLISHED, "审核通过");
    }

    /**
     * 自管理敏感词审核
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitiveScan(String content, WmNews wmNews) {
        boolean flag = true;
        //获取所有的敏感词
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(
            Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives)
            .collect(Collectors.toList());
        //初始化敏感词库
        SensitiveWordUtil.initMap(sensitiveList);
        //查看文章中是否包含敏感词
        Map<String,Integer> map = SensitiveWordUtil.matchWords(content);
        if(!map.isEmpty()){
            updateWmNewsStatus(wmNews, Status.FAIL, "当前文章或图片存在敏感词"+map);
            flag = false;
        }
        return flag;
    }

    /**
     * 保存app端文章数据
     *
     * @param wmNews
     * @return
     */
    private ResponseResult  saveAppArticle(WmNews wmNews) {
        ArticleDto dto = new ArticleDto();
        //属性拷贝
        BeanUtils.copyProperties(wmNews,dto);
        //文章布局
        dto.setLayout(wmNews.getType());
        //频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if(wmChannel != null){
            dto.setChannelName(wmChannel.getName());
        }
        //作者
        dto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if(wmUser != null){
            dto.setAuthorName(wmUser.getName());
        }
        //如果文章id不为空,则表示文章之前已经审核成功,需要更新修改文章
        if(wmNews.getArticleId() != null)
        {
            dto.setId(wmNews.getArticleId());
        }
        dto.setCreatedTime(new Date());
        ResponseResult responseResult = articleClient.save(dto);
        return responseResult;
    }
    /**
     * 从自媒体文章中提取文本和图片(包括内容和封面)
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {
        StringBuilder textContainer = new StringBuilder();
        List<String> imagesContainer = new ArrayList<>();
        //提取文章内容中的文本和图片
        if(StringUtils.isNotBlank(wmNews.getContent())){
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            maps.forEach(map -> {
                if("text".equals(map.get("type"))){
                    textContainer.append(map.get("value"));
                }
                if("image".equals(map.get("type"))){
                    imagesContainer.add(map.get("value").toString());
                }
            });
        }
        //提取封面图片
        if(StringUtils.isNotBlank(wmNews.getImages())){
            imagesContainer.addAll(Arrays.asList(wmNews.getImages().split(",")));
        }
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("content",textContainer.toString());
        resultMap.put("images",imagesContainer);
        return resultMap;
    }

    /**
     * 处理文本审核
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content,WmNews wmNews){
        boolean flag = true;
        String text = wmNews.getTitle() + "-" + content;
        if(StringUtils.isBlank(text)){
            return flag;
        }
        try {
            Map map = greenTextScan.greeTextScan(content);
            if(map != null){
                switch (map.get(ContentCencorConstants.SUGGESTION).toString()){
                    case ContentCencorConstants.SUGGESTION_BLOCK:
                        //审核不通过
                        updateWmNewsStatus(wmNews, Status.FAIL, "当前文章中存在违规内容");
                        flag = false;
                        break;
                    case ContentCencorConstants.SUGGESTION_REVIEW:
                        //需要人工审核
                        updateWmNewsStatus(wmNews, Status.ADMIN_AUTH,
                            "文本内容存在不确定内容,需要人工审核");
                        flag = false;
                }
            }
        } catch (Exception e) {
            flag = false;
            throw new RuntimeException(e);
        }
        return flag;
    }

    /**
     * 处理图片审核
     * @param images
     * @param wmNews
     * @return
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {
        boolean flag = true;
        //下载图片
        //用HashSet去重
        images = images.stream().distinct().collect(Collectors.toList());
        List<byte[]> imageList = new ArrayList<>();
        //图片审核
        try {
            //图片下载并OCR识别敏感词汇
            for(String image : images){
                byte[] bytes = fileStorageService.downLoadFile(image);
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(in);
                //图片识别
                String result = tess4jClient.doOCR(bufferedImage);
                //过滤文字
                boolean isSensitive = handleSensitiveScan(result, wmNews);
                if(!isSensitive){
                    log.warn("自媒体文章图片OCR敏感词审核失败,文章id:{}",wmNews.getId());
                    return false;
                }
                imageList.add(bytes);
            }
            Map map = greenImageScan.imageScan(imageList);
            if(map != null) {
                switch (map.get(ContentCencorConstants.SUGGESTION).toString()) {
                    case ContentCencorConstants.SUGGESTION_BLOCK:
                        //审核不通过
                        updateWmNewsStatus(wmNews, Status.FAIL, "当前图片中存在违规内容");
                        flag = false;
                        break;
                    case ContentCencorConstants.SUGGESTION_REVIEW:
                        //需要人工审核
                        updateWmNewsStatus(wmNews, Status.ADMIN_AUTH,
                            "图片存在不确定内容,需要人工审核");
                        flag = false;
                }
            }
        } catch (Exception e) {
            flag = false;
            throw new RuntimeException(e);
        }
        return flag;
    }


    private void updateWmNewsStatus(WmNews wmNews, Status status, String reason) {
        wmNews.setStatus(status.getCode());
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }
}
