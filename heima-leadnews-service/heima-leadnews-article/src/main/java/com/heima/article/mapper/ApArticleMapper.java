package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApArticleMapper extends BaseMapper<ApArticle> {

    /**
     * 加兹文章列表
     * @param dto
     * @param type 1加载更多 2加载最新
     * @return
     */
    List<ApArticle> loadArticleList(ArticleHomeDto dto,Short type);
}
