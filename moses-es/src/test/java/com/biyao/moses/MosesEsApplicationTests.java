package com.biyao.moses;

import com.biyao.moses.service.ProductEsServiceImpl;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class MosesEsApplicationTests {
   @Autowired
   RestHighLevelClient restHighLevelClient;
   @Autowired
    ProductEsServiceImpl  productEsService;

    @Test
    void createIndex() throws IOException {
        productEsService.rebulidIndex();
    }

//    @Test
//     void blukUpdateIndex(){
//        List<Product> productList = new ArrayList<>();
//        byte shelfstatus =1;
////        Product product1 =new Product(1300135004L,28L,new Date(),shelfstatus,shelfstatus,1000000,shelfstatus,"2,7,9,1","111,222",1300135000L);
////        Product product2 =new Product(1300135000L,29L,new Date(),shelfstatus,shelfstatus,1000000,shelfstatus,"2","111,222",1300135000L);
////        productList.add(product1);productList.add(product2);
//
//    }
//
//
//
//
//
//
//    @Test
//    void delete() throws IOException {
//        //准备request对象
//        DeleteIndexRequest request = new DeleteIndexRequest("");
//        //通过client连接es
//        AcknowledgedResponse delete = restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);
//        //输出
//        System.out.println(delete.isAcknowledged());
//    }
//
//    @Test
//    void createDoc() throws IOException {
//
//        Map<String,Object>  jsonMap = new HashMap<>();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        String format = sdf.format(new Date());
//        Person person =new Person(1,"eve",18,new Date());
//        jsonMap.put("name",person.getName());
//        jsonMap.put("age",person.getAge());
//        jsonMap.put("birthday",format);
//        IndexRequest indexRequest = new IndexRequest(index).id("1").source(jsonMap);
//        IndexResponse response = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
//        DocWriteResponse.Result result = response.getResult();
//        System.out.println( result.toString());
//    }
//    @Test
//    void selectDoc()throws IOException {
//
//        GetRequest getRequest = new GetRequest(index, "56");
//        GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
//        Map<String, Object> map = response.getSource();
//        System.out.println(JSON.toJSONString(map));
//
//
//        DeleteRequest deleteRequest = new DeleteRequest(index, "56");
//        DeleteResponse deleteResponse = restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
//        DocWriteResponse.Result deleteResponseResult = deleteResponse.getResult();
//
//        Map<String,Object>  jsonMap = new HashMap<>();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        String format = sdf.format(new Date());
//        Person person =new Person(1,"eve5655",18,new Date());
//        jsonMap.put("name",person.getName());
//        jsonMap.put("age",person.getAge());
//        jsonMap.put("birthday",format);
//        UpdateRequest request = new UpdateRequest(index, "56").doc(jsonMap);
//        UpdateResponse updateResponse = restHighLevelClient.update(request, RequestOptions.DEFAULT);
//        DocWriteResponse.Result updateResponseResult = updateResponse.getResult();
//    }
//
//
//
//    @Test
//    void blukDoc()throws IOException {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        BulkRequest request = new BulkRequest();
//        List<Person> list =new ArrayList<>();
//        for (int i = 0; i < 100; i++) {
//            list.add(new Person(i,"eve"+i,18,new Date()));
//        }
//        for (Person person : list) {
//            request.add(new IndexRequest(index).id(person.getId().toString())
//                    .source(XContentType.JSON, "name", person.getName(), "age", person.getAge(), "birthday", sdf.format(new Date())));
//        }
//
//        BulkResponse bulkResponse = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
//
//        // 如果至少有一个操作失败，此方法返回true
//        if (bulkResponse.hasFailures()) {
//            StringBuffer sb = new StringBuffer("");
//            for (BulkItemResponse bulkItemResponse : bulkResponse) {
//                //指示给定操作是否失败
//                if (bulkItemResponse.isFailed()) {
//                    //检索失败操作的失败
//                    BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
//                    sb.append(failure.toString()).append("\n");
//                }
//            }
//            System.out.println("=bulk error="+sb.toString());
//            System.out.println(sb.toString());
//        } else {
//            System.out.println("success");
//        }
//    }
}
