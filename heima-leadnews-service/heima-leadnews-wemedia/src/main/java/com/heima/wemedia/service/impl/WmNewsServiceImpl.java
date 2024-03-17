package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNews.Status;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl  extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {
    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;
    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    /**
     * 条件查询文章列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        //检查参数
        //分页检查
        dto.checkParam();
        //分页条件查询
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
        //状态精确查询
        .eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus())
        //频道精确查询
        .eq(dto.getChannelId() != null, WmNews::getChannelId, dto.getChannelId())
        //时间范围查询
        .ge(dto.getBeginPubDate() != null, WmNews::getPublishTime, dto.getBeginPubDate())
        .le(dto.getEndPubDate() != null, WmNews::getPublishTime, dto.getEndPubDate())
        //查询当前登录人的文章
        .eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId())
        //关键字模糊查询
        .like(StringUtils.isNotBlank(dto.getKeyword()), WmNews::getTitle, dto.getKeyword())
        //按照发布时间倒序查询
        .orderByDesc(WmNews::getPublishTime);
        page = page(page, lambdaQueryWrapper);
        //返回结果
        PageResponseResult pageResponseResult = PageResponseResult.builder()
            .currentPage(dto.getPage())
            .size(dto.getSize())
            .total((int) page.getTotal()).build();
        pageResponseResult.setData(page.getRecords());
        return pageResponseResult;

    }

    /**
     * 发布修改文章和保存草稿
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        //条件判断
        if(dto == null || dto.getContent() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto, wmNews);
        //封面图片 list转字符串
        if(dto.getImages() != null && !dto.getImages().isEmpty()){
            wmNews.setImages(StringUtils.join(dto.getImages(), ","));
        }
        //如果当前封面类型为自动
        //根据图片数量设置封面类型
        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            wmNews.setType(null);//后续会根据图片数量设置
        }
        //保存或修改文章
        saveOrUpdateNews(wmNews);

        //判断是否是草稿
        //如果是草稿，结束方法
        if(dto.getStatus().equals(Status.NORMAL.getCode())){
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        //不是草稿,保存文章内容图片与素材的关系
        //获取文章内容中的图片信息
        List<String> materials = extractImageContent(dto);
        //保存文章内容图片与素材的关系
        saveRelativeImages(materials, wmNews.getId(), WemediaConstants.WM_CONTENT_REFERENCE);
        //保存文章封面图片与素材的关系
        //如果当前封面类型为自动,需要匹配封面图片(到内容图片中找)
        saveRelativeCover(dto,wmNews,materials);
        //返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 功能1:如果当前封面类型为自动,则设置封面类型
     *      匹配规则:
     *      1.如果文章内容中没有图片,则设置为无图
     *      2.如果文章内容中有图片,则第一张图片为封面
     *      3.如果文章内容中的图片数量大于等于3,则设置为多图
     * 功能2:保存封面图片与素材的关系
     * @param dto
     * @param wmNews
     * @param materials
     */
    private void saveRelativeCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {
        //如果当前封面类型为自动,根据图片数量设置封面类型
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            //如果文章内容中没有图片,则设置为无图
            if (materials == null || materials.isEmpty()) {
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            } else if (materials.size() >= 3) {
                //如果文章内容中的图片数量大于等于3,则设置为多图
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
            } else {
                //如果文章内容中有小于3张图片,则设置为单图
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
            }
        }
        if (materials == null || materials.isEmpty()) {
            return;
        }
        List<String> coverImages = new ArrayList<>();
        if (wmNews.getType() != null) {
            if(wmNews.getType().equals(WemediaConstants.WM_NEWS_SINGLE_IMAGE)){
                coverImages.add(materials.get(0));
            }else if(wmNews.getType().equals(WemediaConstants.WM_NEWS_MANY_IMAGE)){
                if (materials.size() >= 3) {
                    coverImages = materials.subList(0, 3);
                }
            }
        }
        wmNews.setImages(StringUtils.join(coverImages, ","));
        updateById(wmNews);
        saveRelativeImages(coverImages, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
    }


    private void saveRelativeImages(List<String> materials,Integer newsId,Short type) {
        //如果没有图片信息，结束方法
        if(materials == null || materials.isEmpty()){
            log.info("文章中没有图片信息");
            return;
        }
        //通过图片地址查询素材id
        List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(
            Wrappers.<WmMaterial>lambdaQuery()
            .in(WmMaterial::getUrl, materials));
        //判断素材是否有效
        if(dbMaterials == null || dbMaterials.isEmpty()){
            //手动抛出异常 提示调用者素材失效了,并可以进行数据回滚
            throw new CustomException(AppHttpCodeEnum.MATERIAL_REFRENCE_FAIL);
        }
        List<Integer> materialIdList = dbMaterials.stream().map(WmMaterial::getId)
            .collect(Collectors.toList());
        //批量保存文章图片与素材的关系
        wmNewsMaterialMapper.saveRelations(materialIdList,newsId,type);
    }

    private static List<String> extractImageContent(WmNewsDto dto) {
        String content = dto.getContent();
        List<Map> mapList = JSON.parseArray(content, Map.class);
        List<String> materials = new ArrayList<>();
        for (Map map : mapList) {
            if(map.get("type").equals(WemediaConstants.WM_NEWS_TYPE_IMAGE)){//与前端约定好的固定字段
                materials.add(map.get("value").toString());
            }
        }
        return materials;
    }

    private void saveOrUpdateNews(WmNews wmNews) {
        //补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short) 1);//默认上架
        if(wmNews.getId() == null){
            save(wmNews);
        }else{
            //修改文章
            //删除文章图片与素材的关系
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            //更新文章
            updateById(wmNews);
        }
    }
}
