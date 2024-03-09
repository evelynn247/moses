package com.biyao.moses;

import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static com.biyao.moses.common.constant.EsIndexConstant.SUPPORT_MJQ;

@SpringBootTest
@Slf4j
class MosesMatchOnlineApplicationTests {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    private Session session;
    @Autowired
    FileSystem fileSystem;
    @Test
    void contextLoads() throws Exception {

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        SearchSourceBuilder searchSourceBuilder =new SearchSourceBuilder();
        // 精确匹配
        BoolQueryBuilder mustQueryBuilder = QueryBuilders.boolQuery();

        mustQueryBuilder.must(QueryBuilders.termQuery("shelfStatus", 1));
        mustQueryBuilder.must(QueryBuilders.termQuery("showStatus", 0));
////        mustQueryBuilder.must(QueryBuilders.rangeQuery("firstOnshelfTime").
////                                                gte(new java.util.Date(1569138010000L)).
////                                                lt(new java.util.Date(System.currentTimeMillis())));
//        mustQueryBuilder.mustNot(QueryBuilders.termQuery("supportPlatform", "3"));
        boolQuery.must(mustQueryBuilder);

        BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery();
        shouldQueryBuilder.should(QueryBuilders.termsQuery(SUPPORT_MJQ, "111","444"));
        boolQuery.must(shouldQueryBuilder);
//        BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery();
//        // should 匹配
//        shouldQueryBuilder.should(QueryBuilders.termQuery("supportPlatform", "3"));
//        boolQuery.must(shouldQueryBuilder);

        searchSourceBuilder.query(boolQuery);



        // 召回字段设置
        String [] fetchSource ={"shelfStatus","firstOnshelfTime"};
        searchSourceBuilder.fetchSource(fetchSource,null);
        // 控制查询数量
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(200);

        // 排序
        searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
        SearchRequest rq = new SearchRequest();
        //索引名
        rq.indices("by_moses_product_aries");
        rq.source(searchSourceBuilder);
        SearchResponse response = restHighLevelClient.search(rq, RequestOptions.DEFAULT);

        SearchHits searchHits = response.getHits();
        if (response.status() != RestStatus.OK || searchHits == null || searchHits.getTotalHits().value <0) {
           return;
        }

        List<Map<String, Object>> collect = Arrays.stream(searchHits.getHits()).map(SearchHit::getSourceAsMap).collect(Collectors.toList());
        Map<String, Float> pidAndScore = Arrays.stream(searchHits.getHits()).collect(Collectors.toMap(SearchHit::getId, SearchHit::getScore));
        System.out.println();

    }

    @Test
    public void loaderModel(){
//        String modelPath = "/eve/test/model/model.pb";
//        try {
//            Graph graph = new Graph();
//            ByteArrayOutputStream contents = new ByteArrayOutputStream();
//            IOUtils.copy(fileSystem.open(new Path(modelPath)), contents);
//            graph.importGraphDef(GraphDef.parseFrom(contents.toByteArray()));
//            this.session = new Session(graph);
//        }catch(Exception e) {
//            System.err.println("Init tensorflow model from " + modelPath + " error: " + e.toString());
//            e.printStackTrace();
//        }
//        System.out.println();
        try {
            FSDataInputStream open = fileSystem.open(new Path("/eve/test/model/model.pb"));
            System.out.println();
        }catch (Exception e){

        }


    }
        @Test
        public void loaderModel2() throws  Exception{
        SavedModelBundle modelBundle = SavedModelBundle.load("F:\\BYjavaProject\\mosesonline\\moses-online\\moses-parent-online\\moses-match-online\\src\\main\\resources\\fm_i2v_model", "serve");
        this.session = modelBundle.session();
        System.out.println();
    }

    @Test
    public String getRemoteFile() throws Exception {
        String destUrl ="http://conf.nova.biyao.com/nova/model.pb";
        List<String> lines = new ArrayList<>();
        File tmpDir = Files.createTempDir();
        String fileName = tmpDir + File.separator + String.valueOf( Math.random() ) + (new Date()).getTime() + ".bak";
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        byte[] buf = new byte[10240];
        int size = 0;
        try {
            // 建立链接
            url = new URL( destUrl );
            httpUrl = (HttpURLConnection) url.openConnection();
            // 连接指定的资源
            httpUrl.connect();
            // 获取网络输入流
            bis = new BufferedInputStream( httpUrl.getInputStream() );
            // 建立文件
            fos = new FileOutputStream( fileName );

            // 保存文件
            while ((size = bis.read( buf )) != -1) {
                fos.write( buf, 0, size );
            }
            fos.close();
            bis.close();
            httpUrl.disconnect();

            SavedModelBundle modelBundle = SavedModelBundle.load(fileName, "serve");
            this.session = modelBundle.session();
        } catch (Exception e) {
            throw new Exception("读取网络文件出错", e );
        }finally {
            tmpDir.delete();
        }
        return fileName;
    }
}
