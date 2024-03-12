package com.heima.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements
    ApArticleService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 10;
    @Autowired
    private ApArticleMapper apArticleMapper;

    /**
     * 加载文章列表
     *
     * @param dto
     * @param type 1加载更多 2加载最新
     * @return
     */
    @Override
    public ResponseResult load(ArticleHomeDto dto, short type) {
        //校验参数
        //分页条数校验
        Integer size = dto.getSize();
        if (size == null || size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }
        //分页的值不超过50
        Math.min(size, MAX_PAGE_SIZE);
        //校验type参数,如果参数错误,默认加载更多
        if(type != ArticleConstants.LOADTYPE_LOAD_MORE && type != ArticleConstants.LOADTYPE_LOAD_NEW){
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //频道参数校验
        if(StringUtils.isBlank(dto.getTag())){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        //时间校验
        //最大时间
        if(dto.getMaxBehotTime() == null){
            dto.setMaxBehotTime(new Date());
        }
        //最小时间
        if(dto.getMinBehotTime() == null){
            dto.setMinBehotTime(new Date());
        }
        //查询
        List<ApArticle> apArticleList = apArticleMapper.loadArticleList(dto, type);
        //返回结果
        return ResponseResult.okResult(apArticleList);
    }
}
