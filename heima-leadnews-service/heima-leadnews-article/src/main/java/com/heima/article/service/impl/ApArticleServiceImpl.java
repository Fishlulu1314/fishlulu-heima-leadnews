package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleFreemarkerService;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
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
    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;
    @Autowired
    private ApArticleFreemarkerService apArticleFreemarkerService;

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
        if (type != ArticleConstants.LOADTYPE_LOAD_MORE
            && type != ArticleConstants.LOADTYPE_LOAD_NEW) {
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //频道参数校验
        if (StringUtils.isBlank(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        //时间校验
        //最大时间
        if (dto.getMaxBehotTime() == null) {
            dto.setMaxBehotTime(new Date());
        }
        //最小时间
        if (dto.getMinBehotTime() == null) {
            dto.setMinBehotTime(new Date());
        }
        //查询
        List<ApArticle> apArticleList = apArticleMapper.loadArticleList(dto, type);
        //返回结果
        return ResponseResult.okResult(apArticleList);
    }

    /**
     * 保存文章内容到app端文章数据库
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveArticle(ArticleDto dto) {
        //校验参数
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(dto, apArticle);
        //判断是否存在id
        if (dto.getId() == null) {
            //不存在id,则保存文章,文章配置,文章内容三张表

            //保存文章
            save(apArticle);
            //保存文章配置,mp自带的保存后会返回主键id
            ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
            apArticleConfigMapper.insert(apArticleConfig);
            //保存文章内容
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setContent(dto.getContent());
            apArticleContent.setArticleId(apArticle.getId());
            apArticleContentMapper.insert(apArticleContent);
        } else {
            //存在id,则更新文章,文章内容(配置有默认选项不用更新)
            //更新文章
            updateById(apArticle);
            //更新文章内容
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(
                Wrappers.<ApArticleContent>lambdaQuery()
                    .eq(ApArticleContent::getArticleId, dto.getId()));
            if (apArticleContent != null) {
                apArticleContent.setContent(dto.getContent());
                apArticleContentMapper.updateById(apArticleContent);
            }
        }
        //异步调用 生成静态文件上传到minio中
        apArticleFreemarkerService.buildArticleToMinio(apArticle, dto.getContent());
        //返回结果 文章id
        return ResponseResult.okResult(apArticle.getId());
    }
}
