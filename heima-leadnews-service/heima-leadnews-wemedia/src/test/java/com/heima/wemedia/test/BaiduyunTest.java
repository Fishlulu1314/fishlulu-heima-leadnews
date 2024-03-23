package com.heima.wemedia.test;


import com.baidu.aip.contentcensor.AipContentCensor;
import com.heima.common.baiduyun.GreenImageScan;
import com.heima.common.baiduyun.GreenTextScan;
import com.heima.file.service.FileStorageService;
import com.heima.wemedia.WemediaApplication;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class BaiduyunTest {

    @Autowired
    private GreenTextScan greenTextScan;
    @Autowired
    private GreenImageScan greenImageScan;
    @Autowired
    private FileStorageService fileStorageService;
    /**
     * 测试文本内容审核
     */
    @Test
    public void testScanText() throws Exception {
        Map map = greenTextScan.greeTextScan("***");
        System.out.println(map);
    }
    @Test
    public void testScanImage() throws Exception {
        byte[] bytes = fileStorageService.downLoadFile("http://hmtt:9000/leadnews/2024/03/16/443030b3cc0547daad9f344fb2fea13b.jpg");
        Map map = greenImageScan.imageScan(Collections.singletonList(bytes));
        System.out.println(map);
    }
}
