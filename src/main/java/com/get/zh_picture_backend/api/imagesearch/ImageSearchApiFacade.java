package com.get.zh_picture_backend.api.imagesearch;

import com.get.zh_picture_backend.api.imagesearch.model.ImageSearchResult;
import com.get.zh_picture_backend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.get.zh_picture_backend.api.imagesearch.sub.GetImageListApi;
import com.get.zh_picture_backend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {
    public static List<ImageSearchResult> searchImage(String imgUrl){
        //1.首先调用第一个接口 获取一个url 该url会返回我们相似照片所在的一个网页
        String firstUrl = GetImagePageUrlApi.getImagePageUrl(imgUrl);
        //2.拿到第一步给的网页之后 解析他的html结构 获取到其中firstUrl
        String imageListUrl = GetImageFirstUrlApi.getImageFirstUrl(firstUrl);
        //3.然后再去访问firstUrl 会访问图片的集合
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageListUrl);
        return imageList;
    }

    public static void main(String[] args) {
        String imgUrl="https://inews.gtimg.com/om_bt/OdjjDdm3QxgQuVPKxW5O6U9Bn95Oe1QB1fGHpZnhx5YJgAA/641";
        List<ImageSearchResult> imageSearchResults = searchImage(imgUrl);
        System.out.println(imageSearchResults);
    }
}
