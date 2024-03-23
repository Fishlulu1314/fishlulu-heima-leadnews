package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;

public interface ApArticleFreemarkerService {

    /**
     * 生成静态文件上传到minio中
     * @param apArticle
     * @param content
     */
    void buildArticleToMinio(ApArticle apArticle,String content);
}
