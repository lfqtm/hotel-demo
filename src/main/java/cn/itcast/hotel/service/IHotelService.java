package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IHotelService extends IService<Hotel> {

	/**
	 * 查询list
	 * @param params
	 * @return
	 * @throws IOException
	 */
    PageResult search(RequestParams params) throws IOException;

	/**
	 * 条件过滤
	 * @param params
	 * @return
	 * @throws IOException
	 */
    Map<String, List<String>> getFilters(RequestParams params) throws IOException;
}
