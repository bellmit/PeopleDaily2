/**
 * 
 */
package com.ronglian.service.impl;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ronglian.dao.NewsInfoDao;
import com.ronglian.entity.NewsInfo;
import com.ronglian.entity.NewsPicture;
import com.ronglian.entity.NewsTopic;
import com.ronglian.service.NewsInfoService;
import com.ronglian.utils.PageCountResult;
import com.ronglian.utils.PageResult;
import com.ronglian.utils.RongLianResult;
import com.ronglian.utils.RongLianUtils;
import com.ronglian.utils.model.request.ImageInfo;
import com.ronglian.dao.NewsPictureDao;
import com.ronglian.dao.TopicDao;

/**
 * @author liyang
 * @createTime 2017/12/27
 */
@Service
public class NewsInfoServiceImpl implements NewsInfoService {

	/* (non-Javadoc)
	 * @see com.ronglian.service.NewsInfoService#inserNewsInfo(com.ronglian.entity.NewsInfo)
	 */
	@Autowired
	private NewsInfoDao newsInfoDao;
	@Autowired
	private NewsPictureDao newsPictureDao;
	@Autowired
	private TopicDao topicDao;
	@Override
	public RongLianResult inserNewsInfo(NewsInfo newsInfo) {
		NewsInfo result = this.newsInfoDao.save(newsInfo);
		if(result != null){
			return RongLianResult.ok();
		}else{
			return RongLianResult.build(200, "save failed");
		}
	}
	/**
	 * @author liyang
	 * @createTime 2017年12月27日
	 */
	@Override
	public PageCountResult findNewsList(int pageSize, int pageNo, String channelUniqueId) {
		int start = 0;
		int counter = 0;
		List<Map> resultList = new ArrayList<Map>();
		if(channelUniqueId != null){
			start = (pageNo-1)*pageSize;
			List<NewsInfo> list = this.newsInfoDao.selectNewsInfoByChannel(channelUniqueId,start,pageSize);
			if(list != null && list.size() > 0){
//				counter = list.size();
				counter = this.newsInfoDao.countNewsInfoByChannel(channelUniqueId);
				for(NewsInfo news:list){
					Map resultMap = new HashMap();
					resultMap.put("newsTitle", news.getNewsTitle());
					resultMap.put("newsId", news.getNewsId());
					resultMap.put("newsTags", news.getNewsTags());
					resultMap.put("publishTime", RongLianUtils.changeDateTime(news.getPublishTime()));
					resultMap.put("newsSort", news.getNewsSort());
					resultMap.put("showType", news.getShowType());
					resultMap.put("fullColumnImgUrl", news.getFullColumnImgUrl());
					resultMap.put("hasVideo", news.getHasVideo());
					resultMap.put("isLive", news.getIsLive());
					resultMap.put("isLiveReplay", news.getIsLiveReplay());
					//追加直播5个字段
					resultMap.put("appointCoverImage ",news.getAppointCoverImage());
					resultMap.put("liveUrl",news.getLiveUrl());
					resultMap.put("liveReplayUrl",news.getLiveReplayUrl());
					resultMap.put("liveHostChatid",news.getLiveHostChatid());
					resultMap.put("liveUsChatid",news.getLiveUsChatid());
					
					//追加dataMode、link两个字段
					resultMap.put("link",news.getLink());
					resultMap.put("dataMode",news.getDataMode());
					
					Integer isTopic = news.getIsTopic();
					resultMap.put("isTopic", news.getIsTopic());
					String topicUniqueId = null;
					if(isTopic > 0){
						topicUniqueId = news.getTopicUniqueId();
						resultMap.put("topicUniqueId", topicUniqueId);
					}
					//如果是isTopic不为null,说明是专题
					if(StringUtils.isNotBlank(topicUniqueId)){
						//根据专题的uniqueId查询专题
						NewsTopic topic = this.topicDao.getNewsTopicByTopicId(topicUniqueId);
						if(topic != null){
							Map topicDetail = new HashMap();
							topicDetail.put("topicDesc", topic.getTopicDesc());
							topicDetail.put("bannerPhoto", topic.getBannerImage());
							topicDetail.put("coverPhoto", topic.getListImage());
							resultMap.put("topicDetail",topicDetail);
						}else{
							resultMap.put("topicDetail",null);
						}
					}else{
						resultMap.put("topicDetail",null);
					}
					
					//查看图片
					Integer imageCount = news.getImageList();
					if(imageCount == null){
						imageCount = 0;
					}
					resultMap.put("imageCount", imageCount);
					
					if(imageCount > 0){
						List<NewsPicture> pictures = this.newsPictureDao.selectNewsPictureByNewsId(news.getNewsId());
						if(pictures != null && pictures.size() > 0){
							List<Map> photoList = new ArrayList<Map>();
							for(NewsPicture picture:pictures){
								Map photo = new HashMap();
								photo.put("pictureId", picture.getPictureId());
								photo.put("picPath", picture.getImagePath());
								photo.put("picTitle", picture.getPictureTitle());
								photo.put("picDesc", picture.getPictureDesc());
								photoList.add(photo);
							}
							resultMap.put("photoList",photoList);
						}else{
							resultMap.put("photoList",null);
						}
					}else{//imageList锟斤拷锟斤拷0
						resultMap.put("photoList",null);
					}
					resultList.add(resultMap);
				}
				return PageCountResult.build(0, "ok",counter,pageNo, pageSize, resultList);
			}else{
				return PageCountResult.error(200, "result is null", pageNo, pageSize);
			}
		}else{
			return PageCountResult.error(200, "channelUniqueId can not be null", pageNo, pageSize);
		}
		
	}
	/**
	 * @author liyang
	 * @createTime 2017年12月27日
	 */
	@Override
	public RongLianResult findTopnewsList(String channelUniqueId) {
		if(channelUniqueId != null){
			List<Map> resultList = new ArrayList<Map>();
			List<NewsInfo> list = this.newsInfoDao.selectTopnewsByChannel(channelUniqueId);
			if(list != null && list.size() > 0){
				for(NewsInfo news:list){
					Map resultMap = new HashMap();
					resultMap.put("newsTitle", news.getNewsTitle());
					resultMap.put("newsId", news.getNewsId());
					resultMap.put("newsTags", news.getNewsTags());
					resultMap.put("publishTime", RongLianUtils.changeDateTime(news.getPublishTime()));
					resultMap.put("newsSort", news.getNewsSort());
					resultMap.put("showType", news.getShowType());
					resultMap.put("fullColumnImgUrl", news.getShowType());
					resultMap.put("hasVideo", news.getHasVideo());
					resultMap.put("isLive", news.getIsLive());
					resultMap.put("isLiveReplay", news.getIsLiveReplay());
					resultMap.put("channelName", news.getChannelName());
					resultMap.put("channelUniqueId", news.getChannelUniqueId());
					
					//追加直播5个字段
					resultMap.put("appointCoverImage ",news.getAppointCoverImage());
					resultMap.put("liveUrl",news.getLiveUrl());
					resultMap.put("liveReplayUrl",news.getLiveReplayUrl());
					resultMap.put("liveHostChatid",news.getLiveHostChatid());
					resultMap.put("liveUsChatid",news.getLiveUsChatid());
					
					//追加dataMode、link两个字段
					resultMap.put("link",news.getLink());
					resultMap.put("dataMode",news.getDataMode());
					
					Integer isTopic = news.getIsTopic();
					resultMap.put("isTopic", news.getIsTopic());
					String topicUniqueId = null;
					if(isTopic > 0){
						topicUniqueId = news.getTopicUniqueId();
						resultMap.put("topicUniqueId", topicUniqueId);
					}
					//如果是isTopic不为null,说明是专题
					if(StringUtils.isNotBlank(topicUniqueId)){
						//根据专题的uniqueId查询专题
						NewsTopic topic = this.topicDao.getNewsTopicByTopicId(topicUniqueId);
						if(topic != null){
							Map topicDetail = new HashMap();
							topicDetail.put("topicDesc", topic.getTopicDesc());
							topicDetail.put("bannerPhoto", topic.getBannerImage());
							topicDetail.put("coverPhoto", topic.getListImage());
							resultMap.put("topicDetail",topicDetail);
						}else{
							resultMap.put("topicDetail",null);
						}
					}else{
						resultMap.put("topicDetail",null);
					}
					//图片张数
					Integer imageCount = news.getImageList();
					if(imageCount == null){
						imageCount = 0;
					}
					resultMap.put("imageCount", imageCount);
					
					if(imageCount > 0){
						List<NewsPicture> pictures = this.newsPictureDao.selectNewsPictureByNewsId(news.getNewsId());
						if(pictures != null && pictures.size() > 0){
							List<Map> photoList = new ArrayList<Map>();
							for(NewsPicture picture:pictures){
								Map photo = new HashMap();
								photo.put("pictureId", picture.getPictureId());
								photo.put("picPath", picture.getImagePath());
								photo.put("picTitle", picture.getPictureTitle());
								photo.put("picDesc", picture.getPictureDesc());
								photoList.add(photo);
							}
							resultMap.put("photoList",photoList);
						}else{
							resultMap.put("photoList",null);
						}
					}else{//imageList锟斤拷锟斤拷0
						resultMap.put("photoList",null);
					}
					resultList.add(resultMap);
				}
				return RongLianResult.ok(resultList);
			}else{
				return RongLianResult.build(200, "result is null");
			}
		}else{
			return RongLianResult.build(200, "channelUniqueId can not be null");
		}
		
	}
	/**
	 * @author liyang
	 * @createTime 2017年12月27日
	 */
	@Override
	public RongLianResult findEditorNewsList(String channelUniqueId) {
		if(channelUniqueId != null){
			List<Map> resultList = new ArrayList<Map>();
			List<NewsInfo> list = this.newsInfoDao.selectEditorNewsByChannel(channelUniqueId);
			if(list != null && list.size() > 0){
				for(NewsInfo news:list){
					Map resultMap = new HashMap();
					resultMap.put("newsTitle", news.getNewsTitle());
					resultMap.put("newsId", news.getNewsId());
					resultMap.put("newsTags", news.getNewsTags());
					resultMap.put("channelName", news.getChannelName());
					resultMap.put("channelUniqueId", news.getChannelUniqueId());
					
					String publishTime =  RongLianUtils.changeDateTime(news.getPublishTime());
					resultMap.put("publishTime", publishTime);
					resultMap.put("newsSort", news.getNewsSort());
					resultMap.put("showType", news.getShowType());
					resultMap.put("fullColumnImgUrl", news.getShowType());
					resultMap.put("hasVideo", news.getHasVideo());
					resultMap.put("isLive", news.getIsLive());
					resultMap.put("isLiveReplay", news.getIsLiveReplay());
					
					//追加直播5个字段
					resultMap.put("appointCoverImage ",news.getAppointCoverImage());
					resultMap.put("liveUrl",news.getLiveUrl());
					resultMap.put("liveReplayUrl",news.getLiveReplayUrl());
					resultMap.put("liveHostChatid",news.getLiveHostChatid());
					resultMap.put("liveUsChatid",news.getLiveUsChatid());
					
					//追加dataMode、link两个字段
					resultMap.put("link",news.getLink());
					resultMap.put("dataMode",news.getDataMode());
					
					Integer isTopic = news.getIsTopic();
					resultMap.put("isTopic", news.getIsTopic());
					String topicUniqueId = null;
					if(isTopic > 0){
						topicUniqueId = news.getTopicUniqueId();
						resultMap.put("topicUniqueId", topicUniqueId);
					}
					//如果是isTopic不为null,说明是专题
					if(StringUtils.isNotBlank(topicUniqueId)){
						//根据专题的uniqueId查询专题
						NewsTopic topic = this.topicDao.getNewsTopicByTopicId(topicUniqueId);
						if(topic != null){
							Map topicDetail = new HashMap();
							topicDetail.put("topicDesc", topic.getTopicDesc());
							topicDetail.put("bannerPhoto", topic.getBannerImage());
							topicDetail.put("coverPhoto", topic.getListImage());
							resultMap.put("topicDetail",topicDetail);
						}else{
							resultMap.put("topicDetail",null);
						}
					}else{
						resultMap.put("topicDetail",null);
					}
					//
					Integer imageCount = news.getImageList();
					if(imageCount == null){
						imageCount = 0;
					}
					resultMap.put("imageCount", imageCount);
					
					if(imageCount > 0){
						List<NewsPicture> pictures = this.newsPictureDao.selectNewsPictureByNewsId(news.getNewsId());
						if(pictures != null && pictures.size() > 0){
							List<Map> photoList = new ArrayList<Map>();
							for(NewsPicture picture:pictures){
								Map photo = new HashMap();
								photo.put("pictureId", picture.getPictureId());
								photo.put("picPath", picture.getImagePath());
								photo.put("picTitle", picture.getPictureTitle());
								photo.put("picDesc", picture.getPictureDesc());
								photoList.add(photo);
							}
							resultMap.put("photoList",photoList);
						}else{
							resultMap.put("photoList",null);
						}
					}else{
						resultMap.put("photoList",null);
					}
					resultList.add(resultMap);
				}
				return RongLianResult.ok(resultList);
			}else{
				return RongLianResult.build(200, "result is null");
			}
		}else{
			return RongLianResult.build(200, "channelUniqueId can not be null");
		}
	}
	/**
	 * @author liyang
	 * @createTime 2017年12月27日
	 */
	@Override
	public PageCountResult findTopicNewsList(String topicId,int pageNo,int pageSize) {
		if(topicId == null ){
			return PageCountResult.error(200, "topicId can not be null ", pageNo, pageSize);
		}
		pageNo = (pageNo-1)*pageSize;
		int count = 0;
		List<NewsInfo> newsInfoList = this.newsInfoDao.selectTopicNewsByNewsInfoId(topicId,pageNo,pageSize);
		List<Map> resultList = new ArrayList<Map>();
		count = newsInfoList.size();
		if(newsInfoList != null && count > 0){
			for(NewsInfo news:newsInfoList){
				Map resultMap = new HashMap();
				resultMap.put("topicId", topicId);
				resultMap.put("newsTitle", news.getNewsTitle());
				resultMap.put("newsId", news.getNewsId());
				resultMap.put("newsTags", news.getNewsTags());
				String publishTime = RongLianUtils.changeDateTime(news.getPublishTime());
				resultMap.put("publishTime", publishTime);
				resultMap.put("newsSort", news.getNewsSort());
				resultMap.put("showType", news.getShowType());
				resultMap.put("fullColumnImgUrl", news.getShowType());
				resultMap.put("hasVideo", news.getHasVideo());
				resultMap.put("isLive", news.getIsLive());
				resultMap.put("isLiveReplay", news.getIsLiveReplay());
				
				//追加直播5个字段
				resultMap.put("appointCoverImage ",news.getAppointCoverImage());
				resultMap.put("liveUrl",news.getLiveUrl());
				resultMap.put("liveReplayUrl",news.getLiveReplayUrl());
				resultMap.put("liveHostChatid",news.getLiveHostChatid());
				resultMap.put("liveUsChatid",news.getLiveUsChatid());
				//追加dataMode、link两个字段
				resultMap.put("link",news.getLink());
				resultMap.put("dataMode",news.getDataMode());
				
				resultMap.put("isTopic", news.getIsTopic());
				resultMap.put("topicUniqueId", news.getTopicUniqueId());
				
				Integer imageCount = news.getImageList();
				if(imageCount == null){
					imageCount = 0;
				}
				resultMap.put("imageCount", imageCount);
				
				if(imageCount > 0){
					List<NewsPicture> pictures = this.newsPictureDao.selectNewsPictureByNewsId(news.getNewsId());
					if(pictures != null && pictures.size() > 0){
						List<Map> photoList = new ArrayList<Map>();
						for(NewsPicture picture:pictures){
							Map photo = new HashMap();
							photo.put("pictureId", picture.getPictureId());
							photo.put("picPath", picture.getImagePath());
							photo.put("picTitle", picture.getPictureTitle());
							photo.put("picDesc", picture.getPictureDesc());
							photoList.add(photo);
						}
						resultMap.put("photoList",photoList);
					}else{
						resultMap.put("photoList",null);
					}
				}else{
					resultMap.put("photoList",null);
				}
				resultList.add(resultMap);
			}
			return PageCountResult.build(200, "ok",count,pageNo, pageSize, resultList);
		}else{
			return PageCountResult.error(200, "result is null", pageNo, pageSize);
		}
	}
	/**
	 * @author liyang
	 * @createTime 2017年12月27日
	 */
	@Override
	public RongLianResult getNewsInfoContent(String newsId) {
		if(newsId == null){
			return RongLianResult.build(200, "newsId can not be null");
		}
		NewsInfo newsInfo = this.newsInfoDao.findOne(newsId);
		if(newsInfo != null){
			Map data = new HashMap();
			data.put("incNo",newsInfo.getContentId() );
			data.put("newsContent",newsInfo.getNewsContent() );
			data.put("newsOrganization", newsInfo.getNewsOrganization());
			data.put("newsAuthors", newsInfo.getNewsAuthors());
			data.put("publishTime", RongLianUtils.changeDateTime(newsInfo.getPublishTime()));
			data.put("newsTitle",newsInfo.getNewsTitle() );
			data.put("appriseUPCount",newsInfo.getAppriseUpNum());
			data.put("appriseDownCount",newsInfo.getAppriseDownNum());
			data.put("commentNum", newsInfo.getCommentNum());
			Integer imageCount = newsInfo.getImageList();
			if(imageCount == null){
				imageCount = 0;
			}
			data.put("imageCount",imageCount );
			
			if(imageCount > 0){
				List<NewsPicture> pictures = this.newsPictureDao.selectNewsPictureByNewsId(newsInfo.getNewsId());
				if(pictures != null && pictures.size() > 0){
					List<Map> photoList = new ArrayList<Map>();
					for(NewsPicture picture:pictures){
						Map photo = new HashMap();
						photo.put("pictureId", picture.getPictureId());
						photo.put("picPath", picture.getImagePath());
						photo.put("picTitle", picture.getPictureTitle());
						photo.put("picDesc", picture.getPictureDesc());
						photoList.add(photo);
					}
					data.put("imageList",photoList);
				}else{
					data.put("imageList",null);
				}
			}else{
				data.put("imageList",null);
			}
			return RongLianResult.ok(data);
		}else{
			return RongLianResult.build(200, "The content of newsInfo is null ");
		}
	}
	@Override
	/**
	 * 刘瀚博   
	 * imedia后台同步新闻内容
	 */
	public RongLianResult addNewsInfo(String newsStr) throws JsonParseException, JsonMappingException, IOException, NumberFormatException, ParseException{
		ObjectMapper mapper = new ObjectMapper();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		/*try{*/
		Map mapRes = mapper.readValue(newsStr, Map.class);
		Map map = (Map)mapRes.get("data");
		if(map != null){
			if(map.get("newsId")==null){
				return RongLianResult.build(200, "newsId can not be null");
			}else{
				if(map.get("channelUniqueId")==null||map.get("channelName")==null){
					return RongLianResult.build(200, "channelUniqueId or channelName can not be null");
				}
				/*
				 * @author liyang
				 * @createTime 2018年2月5日  追加返回字段
				 * 获取专题信息
				 * */
				String topicUniqueId = null;
				Object obj = map.get("topicUniqueId");
				if(obj != null){
					topicUniqueId = obj.toString();
				}
				/*
				 * @author liyang
				 * @createTime 2018年2月5日   追加返回字段
				 * 获取直播：liveUrl、liveReplayUrl、liveHostChatid、liveUsChatid、appointCoverImage
				 */
				NewsInfo newsInfo=new NewsInfo(map.get("newsId").toString(), (map.get("canComment")!=null)?map.get("canComment").toString():null, (map.get("channelUniqueId")!=null)?map.get("channelUniqueId").toString():null,
						(map.get("channelName")!=null)?map.get("channelName").toString():null, null, (map.get("contentId")!=null)?(int)map.get("contentId"):null,
						null, (map.get("createTime")!=null)?sdf.parse(map.get("createTime").toString()):null, (map.get("editExpire")!=null)?sdf.parse(map.get("editExpire").toString()):null,
						null, (map.get("isEditRecom")!=null)?(map.get("isEditRecom").toString().equals("true")?(byte)1:(byte)0):null, (map.get("isToTop")!=null)?(map.get("isToTop").toString().equals("true")?(byte)1:(byte)0):null , 
						//(map.get("isTopic")!=null)?(int)map.get("isTopic"):null,
								(map.get("isTopic")!=null)?(map.get("isTopic").toString().equals("true")?1:0):null,
						null, null, null,
						(map.get("modifyTime")!=null)?sdf.parse(map.get("modifyTime").toString()):null, (map.get("newsAuthors")!=null)?map.get("newsAuthors").toString():null, (map.get("newsContent")!=null)?map.get("newsContent").toString():null,
								(map.get("newsOrganization")!=null)?map.get("newsOrganization").toString():null, (map.get("newsOriginal")!=null)?(int)map.get("newsOriginal"):null, (map.get("newsSort")!=null)?(int)map.get("newsSort"):null,
										(map.get("newsSource")!=null)?map.get("newsSource").toString():null, (map.get("newsSourceUrl")!=null)?map.get("newsSourceUrl").toString():null, null,
												(map.get("newsSummary")!=null)?map.get("newsSummary").toString():null, (map.get("newsTags")!=null)?map.get("newsTags").toString():null, (map.get("newsTitle")!=null)?map.get("newsTitle").toString():null,
														(map.get("publishTime")!=null)?sdf.parse(map.get("publishTime").toString()):null, (map.get("topExpire")!=null)?sdf.parse(map.get("topExpire").toString()):null,null,null,
																(map.get("dataStatus")!=null)?(int)map.get("dataStatus"):null, (map.get("showType")!=null)?(int)map.get("showType"):null,(map.get("fullColumnImgUrl")!=null)?map.get("fullColumnImgUrl").toString():null,
																		(map.get("hasVideo")!=null)?(map.get("hasVideo").toString().equals("true")?(byte)1:(byte)0):null, (map.get("isLive")!=null)?(map.get("isLive").toString().equals("true")?(byte)1:(byte)0):null,(map.get("isLiveReplay")!=null)?(map.get("isLiveReplay").toString().equals("true")?(byte)1:(byte)0):null,topicUniqueId);
				/*
				 *  @author liyang
				 * @createTime 2018年2月5日
				 * 追加 直播的字段：String liveUrl
     			* 				String liveReplayUrl
     			* 				String liveHostChatid
    			* 				String liveUsChatid
				* 				String appointCoverImage 
				*/
				String liveUrl = null;
				String liveReplayUrl = null;
				String liveHostChatid = null;
				String liveUsChatid = null;
				String appointCoverImage = null;
				Integer topnewsSort = null;
				
				Byte isTopnews = null;
				Byte isEditRecom = null;
				Byte isTopnewsTotop = null;
				Byte isLive = null;
				Byte isLiveReplay = null;
				Byte isToTop = null;
				
				obj = map.get("topnewsSort");
				if(obj != null){
					topnewsSort = Integer.parseInt(obj.toString());
				}
				obj = map.get("isToTop");
				if(obj != null){
					isToTop = Byte.parseByte(obj.toString());
				}
				obj = map.get("isLive");
				if(obj != null){
					isLive = Byte.parseByte(obj.toString());
				}
				obj = map.get("isLiveReplay");
				if(obj != null){
					isLiveReplay = Byte.parseByte(obj.toString());
				}
				obj = map.get("isTopnews");
				if(obj != null){
					isTopnews = Byte.parseByte(obj.toString());
				}
				obj = map.get("isEditRecom");
				if(obj != null){
					isEditRecom = Byte.parseByte(obj.toString());
				}
				obj = map.get("isTopnewsTotop");
				if(obj != null){
					isTopnewsTotop = Byte.parseByte(obj.toString());
				}
				obj = map.get("liveUrl");
				if(obj != null){
					liveUrl = obj.toString();
				}
				obj = map.get("liveReplayUrl");
				if(obj != null){
					liveReplayUrl = obj.toString();
				}
				obj = map.get("liveHostChatid");
				if(obj != null){
					liveHostChatid = obj.toString();
				}
				obj = map.get("liveUsChatid");
				if(obj != null){
					liveUsChatid = obj.toString();
				}
				obj = map.get("appointCoverImage");
				if(obj != null){
					appointCoverImage = obj.toString();
				}
				newsInfo.setLiveUrl(liveUrl);
				newsInfo.setLiveReplayUrl(liveReplayUrl);
				newsInfo.setLiveHostChatid(liveHostChatid);
				newsInfo.setLiveUsChatid(liveUsChatid);
				newsInfo.setAppointCoverImage(appointCoverImage);
				newsInfo.setIsEditRecom(isEditRecom);
				newsInfo.setIsTopnews(isTopnews);
				newsInfo.setIsTopnewsTotop(isTopnewsTotop);
				newsInfo.setIsLive(isLive);
				newsInfo.setIsLiveReplay(isLiveReplay);
				newsInfo.setIsToTop(isToTop);
				newsInfo.setTopnewsSort(topnewsSort);
				
				/*
				 *  @author liyang
				 * @createTime 2018年3月5日
				 * 追加 的字段：String link
     			* 			Byte dataMode
				*/
				String link = null;
				Byte dataMode = null;
				obj = map.get("link");
				if(obj != null){
					link = obj.toString();
				}
				obj = map.get("dataMode");
				if(obj != null){
					dataMode = Byte.parseByte(obj.toString());
				}
				newsInfo.setDataMode(dataMode);
				newsInfo.setLink(link);
				//录入APP后台数据库
				newsPictureDao.deleteByNewsID(newsInfo.getNewsId());
				int i=0;
				boolean less=false;
				if(newsInfo.getNewsContent()!=null){
				String[] imgs=getImgs(newsInfo.getNewsContent());
				if(imgs!=null){
					for(;i<imgs.length;i++){
						NewsPicture newsPicture=new NewsPicture(newsInfo.getNewsId(),newsInfo.getNewsId()+"_"+i,imgs[i],i);
						newsPictureDao.save(newsPicture);
					}
					less=true;
				}
				}
				if(map.get("imageList")!=null){
				List<Map> imageList = (List<Map>)map.get("imageList");
		        for(Map imageInfoMap:imageList){
		        	NewsPicture newsPicture=new NewsPicture(newsInfo.getNewsId()
		        			,newsInfo.getNewsId()+"_"+i
		        			,imageInfoMap.get("picPath")!=null?imageInfoMap.get("picPath").toString():null
		        			,imageInfoMap.get("picDesc")!=null?imageInfoMap.get("picDesc").toString():null
		        			,imageInfoMap.get("picTitle")!=null?imageInfoMap.get("picTitle").toString():null
		        			,i);
		        	i++;
		        	newsPictureDao.save(newsPicture);
		        }
				}
				newsInfo.setImageList(i);
				this.newsInfoDao.save(newsInfo);
				return RongLianResult.ok();
			}
		}else{
			return RongLianResult.build(200, "maybe requestBody is null");
		}
	}
	
	private String[] getImgs(String content) {  
	    String img = "";  
	    Pattern p_image;  
	    Matcher m_image;  
	    String str = "";  
	    String[] images = null; 
	    String regEx_img = "<(img|IMG)(.*?)(/>|></img>|>)";
	    p_image = Pattern.compile(regEx_img, Pattern.CASE_INSENSITIVE);  
	    m_image = p_image.matcher(content);  
	    while (m_image.find()) {  
	        img = m_image.group();  
	        Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img);  
	        while (m.find()) {  
	            String tempSelected = m.group(1);  
	  
	            if ("".equals(str)) {  
	                str = tempSelected;  
	            } else {  
	                String temp = tempSelected;  
	                str = str + "," + temp;  
	            }  
	        }  
	    }  
	    if (!"".equals(str)) {  
	        images = str.split(",");  
	    }  
	    return images;  
	}  
	/**
	 * @author liyang
	 * @createTime 2017年12月27日
	 */
	@Override
	public RongLianResult getPhotoNewsByNewsId(String newsID,Integer incNo){
		if(incNo == null){
			return RongLianResult.build(200, "the param incNo can not be null");
		}
		List<NewsInfo> list = this.newsInfoDao.selectNewsInfoNearUpByIncNo(incNo);
		List<NewsInfo> list2 = this.newsInfoDao.selectNewsInfoNearDownByIncNo(incNo);
		if(list != null){
			list.addAll(list2);
		}else{
			list = list2;
		}
		if(list != null && list.size() > 0){
			List<Map> resultList = new ArrayList<Map>();
			
			for(NewsInfo news:list){
				Map result = new HashMap();
				result.put("newsTitle", news.getNewsTitle());
				result.put("newsId", news.getNewsId());
				result.put("newsTags", news.getNewsTags());
				result.put("publishTime", RongLianUtils.changeDateTime(news.getPublishTime()));
				result.put("newsSort", news.getNewsSort());
				
				List<NewsPicture> photos = this.newsPictureDao.selectNewsPictureByNewsId(news.getNewsId());
				List pictureList = new ArrayList();
					for(NewsPicture picture:photos){
						Map photo = new HashMap();
						photo.put("pictureId", picture.getPictureId());
						photo.put("picTitle", picture.getPictureTitle());
						photo.put("picPath", picture.getImagePath());
						photo.put("picDesc", picture.getPictureDesc());
						pictureList.add(photo);
					}
				result.put("pictureList", pictureList);
				resultList.add(result);
			}
			//
			if(resultList.size() == 4 || resultList.size() == 2 || resultList.size() == 1){
				return RongLianResult.ok(resultList);
			}else{
				//
				resultList.remove(0);
				return RongLianResult.ok(resultList);
			}
		}else{
			return RongLianResult.build(200, "the result is null");
		}
	}
	@Override
	public RongLianResult getTopnewsAhead() {
		// TODO Auto-generated method stub
		List<NewsInfo> list = this.newsInfoDao.selectTopnewsAhead();
		if(list == null || !(list.size() > 0)){
			return RongLianResult.build(200, "result is null", null);
		}
		List resultList = new ArrayList();
		for(NewsInfo news:list){
			Map resultMap = new HashMap();
			resultMap.put("newsTitle", news.getNewsTitle());
			resultMap.put("newsId", news.getNewsId());
			resultMap.put("newsTags", news.getNewsTags());
			resultMap.put("publishTime", RongLianUtils.changeDateTime(news.getPublishTime()));
			resultMap.put("newsSort", news.getNewsSort());
			resultMap.put("showType", news.getShowType());
			resultMap.put("fullColumnImgUrl", news.getShowType());
			resultMap.put("hasVideo", news.getHasVideo());
			resultMap.put("isLive", news.getIsLive());
			resultMap.put("isLiveReplay", news.getIsLiveReplay());
			resultMap.put("channelName", news.getChannelName());
			resultMap.put("channelUniqueId", news.getChannelUniqueId());
			resultMap.put("appointCoverImage ",news.getAppointCoverImage());
			resultMap.put("liveUrl",news.getLiveUrl());
			resultMap.put("liveReplayUrl",news.getLiveReplayUrl());
			resultMap.put("liveHostChatid",news.getLiveHostChatid());
			resultMap.put("liveUsChatid",news.getLiveUsChatid());
			
			//追加dataMode、link两个字段
			resultMap.put("link",news.getLink());
			resultMap.put("dataMode",news.getDataMode());
			
			Integer isTopic = news.getIsTopic();
			resultMap.put("isTopic", news.getIsTopic());
			String topicUniqueId = null;
			if(isTopic > 0){
				topicUniqueId = news.getTopicUniqueId();
				resultMap.put("topicUniqueId", topicUniqueId);
			}
			//如果是isTopic不为null,说明是专题
			if(StringUtils.isNotBlank(topicUniqueId)){
				//根据专题的uniqueId查询专题
				NewsTopic topic = this.topicDao.getNewsTopicByTopicId(topicUniqueId);
				if(topic != null){
					Map topicDetail = new HashMap();
					topicDetail.put("topicDesc", topic.getTopicDesc());
					topicDetail.put("bannerPhoto", topic.getBannerImage());
					topicDetail.put("coverPhoto", topic.getListImage());
					resultMap.put("topicDetail",topicDetail);
				}else{
					resultMap.put("topicDetail",null);
				}
			}else{
				resultMap.put("topicDetail",null);
			}
			
			Integer imageCount = news.getImageList();
			if(imageCount == null){
				imageCount = 0;
			}
			resultMap.put("imageCount", imageCount);
			
			if(imageCount > 0){
				List<NewsPicture> pictures = this.newsPictureDao.selectNewsPictureByNewsId(news.getNewsId());
				if(pictures != null && pictures.size() > 0){
					List<Map> photoList = new ArrayList<Map>();
					for(NewsPicture picture:pictures){
						Map photo = new HashMap();
						photo.put("pictureId", picture.getPictureId());
						photo.put("picPath", picture.getImagePath());
						photo.put("picTitle", picture.getPictureTitle());
						photo.put("picDesc", picture.getPictureDesc());
						photoList.add(photo);
					}
					resultMap.put("photoList",photoList);
				}else{
					resultMap.put("photoList",null);
				}
			}else{
				resultMap.put("photoList",null);
			}
			resultList.add(resultMap);
		}
		return RongLianResult.ok(resultList);
	}
	
}