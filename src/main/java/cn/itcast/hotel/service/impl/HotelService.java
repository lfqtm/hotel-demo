package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

	@Resource
	private RestHighLevelClient client;

	/**
	 * 酒店结果过滤
	 *
	 * @param params
	 * @param request
	 */
	private void buildBasicQuery(RequestParams params, SearchRequest request) {
		//1、获取参数进行判断
		String keyword = params.getKey();
		//2、设置查询条件
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
		//2.1、设置关键字搜索的条件
        /*
        Java限制了所谓“表达式语句”的表达式类型。只有有副作用的有意义的表达式才允许，不允许语法上无意义的语句比如 0; 或 a+b; 。他们只是被排除在语言语法之外。
        StringUtils.isEmpty(keyword) ? boolQueryBuilder.must(QueryBuilders.matchAllQuery()) : boolQueryBuilder.must(QueryBuilders.matchQuery("all", keyword));
        */
		if (StringUtils.isEmpty(keyword)) {//查询所有
			boolQueryBuilder.must(QueryBuilders.matchAllQuery());
		} else {//使用匹配查询
			boolQueryBuilder.must(QueryBuilders.matchQuery("all", keyword));
		}
		//2.2、设置城市查询的条件
		String city = params.getCity();
		if (!StringUtils.isEmpty(city)) {
			boolQueryBuilder.must(QueryBuilders.matchQuery("city", city));
		}

		//2.3 设置星级查询的条件
		String starName = params.getStarName();
		if (!StringUtils.isEmpty(starName)) {
			boolQueryBuilder.must(QueryBuilders.termQuery("starName", starName));
		}

		//2.4 设置品牌查询的条件
		String brand = params.getBrand();
		if (!StringUtils.isEmpty(brand)) {
			boolQueryBuilder.must(QueryBuilders.termQuery("brand", brand));
		}

		//2.5、设置价格区间查询的条件
		Integer minPrice = params.getMinPrice();
		Integer maxPrice = params.getMaxPrice();
		if (minPrice != null && maxPrice != null) {
			boolQueryBuilder.must(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice));
		}

		//算分控制
		FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
			//原始查询，相关性算分查询
			boolQueryBuilder,
			//function score 的数组
			new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
				//其中的一个function score元素
				new FunctionScoreQueryBuilder.FilterFunctionBuilder(
					//过滤条件
					QueryBuilders.termQuery("isAD", true),
					//算分函数 ， 设置常量值
					ScoreFunctionBuilders.weightFactorFunction(100)
				)
			}
		).boostMode(CombineFunction.MULTIPLY);   //设置算法运算类型：乘法

		//3、请求体
		// request.source().query(boolQueryBuilder);
		request.source().query(functionScoreQuery);

	}

	@Override
	public PageResult search(RequestParams params) throws IOException {
		//1、创建searchRequest对象，指定索引名称
		SearchRequest request = new SearchRequest("hotel");
		//2、设置查询条件封装
		buildBasicQuery(params, request);
		//3、设置其它条件
		//3.1、设置分页条件
		Integer page = params.getPage();
		Integer size = params.getSize();
		request.source().from((page - 1) * size).size(size);

		//周边的酒店
		//3.2、设置排序条件（地理坐标的排序)
		String location = params.getLocation();
		if (!StringUtils.isEmpty(location)) {
			request.source().sort(
				SortBuilders.geoDistanceSort("location", new GeoPoint(location))
					//设置排序的字段和地理位置坐标
					.order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS)
			);
		}
		//4、执行查询
		SearchResponse response = client.search(request, RequestOptions.DEFAULT);
		//5、处理结果
		return handleResponse(response);
	}


	private PageResult handleResponse(SearchResponse response) {
		//5、获取到结果进行封装，返回
		SearchHits hits = response.getHits();
		//获取总记录数
		Long total = hits.getTotalHits().value;
		//当前页集合
		List<HotelDoc> hotelDocs = new ArrayList<>();
		SearchHit[] hits1 = hits.getHits();
		//6、封装
		for (SearchHit hit : hits1) {
			String sourceAsString = hit.getSourceAsString();
			HotelDoc hotelDoc = JSON.parseObject(sourceAsString, HotelDoc.class);
			//还需要获取到距离，设置到hoteldoc中
			Object[] sortValues = hit.getSortValues();
			if (sortValues != null && sortValues.length > 0) {
				hotelDoc.setDistance(sortValues[0]);
			}
			hotelDocs.add(hotelDoc);  //一个个文档对象转成了POJO，放到了集合中
		}
		return new PageResult(total, hotelDocs);
	}

	@Override
	public Map<String, List<String>> getFilters(RequestParams params) throws IOException {
		//1、创建searchRequest对象
		SearchRequest request = new SearchRequest("hotel");
		//2、设置聚合的范围查询（设置查询条件）
		buildBasicQuery(params, request);
		request.source().size(0);
		//3、设置聚合的条件
		request.source().aggregation(
			AggregationBuilders.terms("brandaggs")  //设置聚合的别名
				.field("brand")   //分组的字段
				.size(100)   //限定查询的最大的数量
		);
		request.source().aggregation(
			AggregationBuilders.terms("cityaggs")
				.field("city")
				.size(100)
		);
		request.source().aggregation(
			AggregationBuilders.terms("staraggs")
				.field("starName")
				.size(100)
		);
		//4、执行查询（聚合查询）
		SearchResponse response = client.search(request, RequestOptions.DEFAULT);
		//5、获取到结果 封装 返回map
		//5.0、创建map对象
		HashMap<String, List<String>> resultMap = new HashMap<>();
		//5.1、获取到所有的聚合结果
		Aggregations aggregations = response.getAggregations();
		//5.2、获取到指定的聚合名称的聚合结果===品牌的
		Terms brandaggsTerms = aggregations.get("brandaggs");
		List<String> brandList = new ArrayList<>();
		List<? extends Terms.Bucket> brandBuckets = brandaggsTerms.getBuckets();
		for (Terms.Bucket brandBucket : brandBuckets) {
			brandList.add(brandBucket.getKeyAsString());
		}

		Terms cityaggsTerms = aggregations.get("cityaggs");
		List<String> cityList = new ArrayList<>();
		List<? extends Terms.Bucket> cityBuckets = cityaggsTerms.getBuckets();
		for (Terms.Bucket cityBucket : cityBuckets) {
			cityList.add(cityBucket.getKeyAsString());
		}

		Terms staraggsTerms = aggregations.get("staraggs");
		List<String> starList = new ArrayList<>();
		List<? extends Terms.Bucket> starBuckets = staraggsTerms.getBuckets();
		for (Terms.Bucket starBucket : starBuckets) {
			starList.add(starBucket.getKeyAsString());
		}

		resultMap.put("brand", brandList);
		resultMap.put("city", cityList);
		resultMap.put("starName", starList);


		return resultMap;
	}


}
