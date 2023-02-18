package cn.itcast.hotel.pojo;

import lombok.Data;

@Data
public class RequestParams {
    private String key;
    private Integer page;
    private Integer size;
    private String sortBy;

    private String city;
    private String starName;
    private String brand;
    private Integer minPrice;
    private Integer maxPrice;
    private String price ;//300-500  split

    //增加一个字段来接收前端传递过来的location的值 位置
    private String location;
}
