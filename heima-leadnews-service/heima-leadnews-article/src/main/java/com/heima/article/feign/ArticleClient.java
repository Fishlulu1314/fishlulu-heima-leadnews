package com.heima.article.feign;

import com.heima.apis.article.IArticleClient;
import com.heima.article.service.ApArticleService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArticleClient implements IArticleClient {

    @Autowired
    private ApArticleService apArticleService;
    /**
     * 保存文章内容到app端文章数据库
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult save(ArticleDto dto) {
        return apArticleService.saveArticle(dto);
    }
}
