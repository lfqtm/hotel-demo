package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class ESSearchTest {


	@Autowired
	private IHotelService hotelService;
	private final String ES_BASE_HOST = "192.168.137.105";
	private RestHighLevelClient client = null;

	/*
	 * 查询的5大类:
	 *      查询所有: match_all
	 *      全文检索: match mutil_match
	 *      精准查询: term range
	 *      地理坐标查询: geo_bounding_box geo_distance
	 *      复合查询: function_score bool
	 */

	/**
	 * 高亮
	 */
	@Test
	void testHighlight() throws IOException {
		// 1.准备Request
		SearchRequest request = new SearchRequest("hotel");
		// 2.准备DSL
		// 2.1.query
		request.source().query(QueryBuilders.matchQuery("all", "如家"));
		// 2.2.高亮
		request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
		// 3.发送请求
		SearchResponse response = client.search(request, RequestOptions.DEFAULT);
		// 4.解析响应
		handleResponse(response);

	}

	/**
	 * 排序，分页
	 */
	@Test
	void testPageAndSort() throws IOException {
		// 页码，每页大小
		int page = 1, size = 5;

		// 1.准备Request
		SearchRequest request = new SearchRequest("hotel");
		// 2.准备DSL
		// 2.1.query
		request.source().query(QueryBuilders.matchAllQuery());
		// 2.2.排序 sort
		request.source().sort("price", SortOrder.ASC);
		// 2.3.分页 from、size
		request.source().from((page - 1) * size).size(5);
		// 3.发送请求
		SearchResponse response = client.search(request, RequestOptions.DEFAULT);
		// 4.解析响应
		handleResponse(response);

	}

	/**
	 * bool查询
	 */
	@Test
	void testBool() throws IOException {
		// 1.准备Request
		SearchRequest request = new SearchRequest("hotel");
		// 2.准备DSL
		// 2.1.准备BooleanQuery
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		// 2.2.添加term must条件
		boolQuery.must(QueryBuilders.termQuery("city", "杭州"));
		// 2.3.添加range
		boolQuery.filter(QueryBuilders.rangeQuery("price").lte(250));

		request.source().query(boolQuery);
		// 3.发送请求
		SearchResponse response = client.search(request, RequestOptions.DEFAULT);
		// 4.解析响应
		handleResponse(response);

	}

	/**
	 * 查询所有文档信息
	 */
	@Test
	public void search01() throws IOException {
		//1.创建请求语义对象
		SearchRequest request = new SearchRequest("hotel");
		// QueryBuilders: 构建查询类型
		request.source().query(QueryBuilders.matchAllQuery());
		//searchRequest.source().query(QueryBuilders.matchQuery("name","如家酒店")); //单字段查询
		//searchRequest.source().query(QueryBuilders.multiMatchQuery("如家酒店","name","brand"));
		//searchRequest.source().query(QueryBuilders.termQuery("brand","如家"));
		//searchRequest.source().query(QueryBuilders.rangeQuery("price").gte("100").lte("200"));
		//2.发送请求给ES
		SearchResponse response = client.search(request, RequestOptions.DEFAULT);
		//3.处理返回结果
		handleResponse(response);
	}

	/**
	 * 处理响应结果
	 */
	public void handleResponse(SearchResponse response) {
		// 获取命中的所有内容
		SearchHits searchHits = response.getHits();
		// 获取命中的总条数
		long count = searchHits.getTotalHits().value;
		System.out.println("命中的条数为: " + count);
		// 获取命中的文档对象数组
		SearchHit[] hits = searchHits.getHits();
		for (SearchHit hit : hits) {
			// 解析每一个hit对象得到对应的文档数据
			String json = hit.getSourceAsString();
			HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
			System.out.println(hotelDoc);
		}
	}

	/**
	 * 在单元测试执行前执行 创建RestAPI对象
	 */
	@BeforeEach
	public void init() {
		client = new RestHighLevelClient(
			RestClient.builder(
				new HttpHost(ES_BASE_HOST, 9200, "http")
				//new HttpHost("localhost", 9201, "http")
			));
		System.out.println(client);
	}


	/**
	 * 在单元测试执行前执行 关闭client对象
	 */
	@AfterEach
	public void destory() throws Exception {
		if (client != null) {
			client.close();
		}
	}


}
