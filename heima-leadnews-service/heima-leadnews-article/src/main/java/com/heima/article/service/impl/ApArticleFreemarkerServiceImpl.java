package com.heima.article.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.service.ApArticleFreemarkerService;
import com.heima.article.service.ApArticleService;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class ApArticleFreemarkerServiceImpl implements ApArticleFreemarkerService {
    @Autowired
    private Configuration configuration;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ApArticleService apArticleService;

    /**
     * 生成静态文件上传到minio中
     *
     * @param apArticle
     * @param content
     */
    @Async
    @Override
    public void buildArticleToMinio(ApArticle apArticle, String content) {
        if (StringUtils.isNotBlank(content)) {
            StringWriter out = null;
            try {
                //生成静态页面
                Template template = configuration.getTemplate("article.ftl");
                //数据模型
                Map<String,Object> contentDataModel = new HashMap<>();
                contentDataModel.put("content", JSONArray.parseArray(content));
                out = new StringWriter();
                template.process(contentDataModel,out);
            } catch (Exception e) {
                log.error("生成静态页面失败", e);
            }
            //上传到minio中
            InputStream in = new ByteArrayInputStream(out.toString().getBytes());
            String staticUrl = fileStorageService.uploadHtmlFile("", apArticle.getId()+".html", in);
            //修改ap_article表,保存static_url字段
            apArticleService.update(Wrappers.<ApArticle>lambdaUpdate()
                .eq(ApArticle::getId, apArticle.getId())
                .set(ApArticle::getStaticUrl, staticUrl));
        }
    }
}
