package com.heima.article.test;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.ArticleApplication;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.service.ApArticleService;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = ArticleApplication.class)
@RunWith(SpringRunner.class)
public class ArticleFreemarkerTest {
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;
    @Autowired
    private Configuration configuration;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ApArticleService apArticleService;
    @Test
    public void testCreateStaticUrl() throws IOException, TemplateException {
        //获取文章内容
        ApArticleContent apArticleContent = apArticleContentMapper.selectOne(
            Wrappers.<ApArticleContent>lambdaQuery()
                .eq(ApArticleContent::getArticleId, "1302862387124125698"));
        if (apArticleContent != null || StringUtils.isNotBlank(apArticleContent.getContent())) {
            //生成静态页面
            Template template = configuration.getTemplate("article.ftl");
            //数据模型
            Map<String,Object> content = new HashMap<>();
            content.put("content", JSONArray.parseArray(apArticleContent.getContent()));
            StringWriter out = new StringWriter();
            template.process(content,out);
            //上传到minio中
            InputStream in = new ByteArrayInputStream(out.toString().getBytes());
            String staticUrl = fileStorageService.uploadHtmlFile("", apArticleContent.getArticleId()+".html", in);
            //修改ap_article表,保存static_url字段
            apArticleService.update(Wrappers.<ApArticle>lambdaUpdate()
                .eq(ApArticle::getId, apArticleContent.getArticleId())
                .set(ApArticle::getStaticUrl, staticUrl));
        }

    }
}
