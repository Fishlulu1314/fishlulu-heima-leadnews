package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Service
@Transactional
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;
    /**
     * 上传图片素材
     *
     * @param multipartFile
     * @return
     */
    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        //检查参数
        if(multipartFile == null || multipartFile.getSize() == 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //上传图片到minio中
        String fileName = UUID.randomUUID().toString().replace("-", "");
        String originalFilename = multipartFile.getOriginalFilename();
        String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fieldId = null;
        try {
            fieldId = fileStorageService.uploadImgFile("", fileName + postfix,
                multipartFile.getInputStream());
            log.info("上传图片到minio中,field:{}",fieldId);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("{}上传图片到minio中失败",this.getClass().getName());
        }
        //保存图片信息到数据库
        WmMaterial wmMaterial = WmMaterial.builder()
            .url(fieldId)
            .isCollection((short)0)
            .type((short)0)
            .userId(WmThreadLocalUtil.getUser().getId())
            .createdTime(new Date())
            .build();
        save(wmMaterial);
        return ResponseResult.okResult(wmMaterial);
    }

    /**
     * 查询素材列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmMaterialDto dto) {
        //检查参数
        dto.checkParam();//如果缺失分页参数,则设置默认值
        //分页查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmMaterial> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //是否收藏
        if(dto.getIsCollection() != null && dto.getIsCollection() == 1){
            lambdaQueryWrapper.eq(WmMaterial::getIsCollection,dto.getIsCollection());
        }
        //按照用户查询
        lambdaQueryWrapper.eq(WmMaterial::getUserId,WmThreadLocalUtil.getUser().getId());
        //按照时间倒序查询
        lambdaQueryWrapper.orderByDesc(WmMaterial::getCreatedTime);
        page(page,lambdaQueryWrapper);
        //结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;

    }
}
