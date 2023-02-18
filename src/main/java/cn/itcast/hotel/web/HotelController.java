package cn.itcast.hotel.web;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
public class HotelController {
    @Autowired
    private IHotelService hotelService;

    /**
     * 查询酒店数据
     * @param params
     * @return
     */
    @PostMapping("/list")
    public PageResult search(@RequestBody RequestParams params) throws IOException {
        return hotelService.search(params);
    }

	/**
	 * 过滤项
	 * @param params
	 * @return
	 * @throws IOException
	 */
	@PostMapping("/filters")
    public Map<String, List<String>> getFilters(@RequestBody RequestParams params) throws IOException {
        return hotelService.getFilters(params);
    }
}
