package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;

public interface ApArticleService extends IService<ApArticle> {
    /**
     * 加载文章列表
     * @param dto
     * @param type 1加载更多 2加载最新
     * @return
     */
    ResponseResult load(ArticleHomeDto dto,short type);

    /**
     * 保存文章内容到app端文章数据库
     *
     * @param dto
     * @return
     */

    ResponseResult saveArticle(ArticleDto dto);

}
