package com.heima.model.wemedia.dtos;

import com.heima.model.common.dtos.PageRequestDto;
import lombok.Data;

@Data
public class WmMaterialDto extends PageRequestDto {

    /**
     * 是否收藏
     * 0 未收藏
     * 1 已收藏
     */
    private Short isCollection;

}
