package com.biyao.moses.tensorflowModel;

import com.biyao.moses.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tensorflow.ConcreteFunction;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;

import javax.annotation.PostConstruct;

@Slf4j
@Service
public class TensorflowModelCache {

    // 单个模型文件网络地址
    @Value("${saved.model.file.path}")
    private String saveModelFilePath;
    @Value("${variables.index.file.path}")
    private String varIndexFilePath;
    @Value("${variables.dat.file.path}")
    private String varDataFilePath;
    // 单个模型文件下载到本地的地址
    @Value("${saved.model.file.path.save.path}")
    private String saveModelFileSavePath;
    @Value("${variables.index.file.save.path}")
    private String varIndexFileSavePath;
    @Value("${variables.dat.file.save.path}")
    private String varDataFileSavePath;
    //模型文件本地地址
    @Value("${user.model.path}")
    private String userModelPath;

    private static final String SERVE = "serve";
    private static final String SERVE_DEFAULT = "serving_default";

    private ConcreteFunction function;

    /**
     * 用户向量模型加载
     */
    @PostConstruct
    public void loadTensorflowModel() {
        log.info("[操作日志]加载tensorflow模型文件开始");
        long start = System.currentTimeMillis();
        try {
            //下载模型文件到本地
            FileUtil.getRemoteFile(saveModelFilePath, saveModelFileSavePath);
            FileUtil.getRemoteFile(varIndexFilePath, varIndexFileSavePath);
            FileUtil.getRemoteFile(varDataFilePath, varDataFileSavePath);
            //加载文件
            SavedModelBundle modelBundle = SavedModelBundle.
                    load(userModelPath, SERVE);
             function = modelBundle.function(SERVE_DEFAULT);
        } catch (Exception e) {
            log.error("[严重异常][邮件告警]下载并加载tensorflow模型文件异常，异常信息：", e);
            String path = userModelPath;
            SavedModelBundle modelBundle = SavedModelBundle.
                    load(path, SERVE);
            function = modelBundle.function(SERVE_DEFAULT);
            //异常情况加载兜底的模型文件
        }
        log.error("[操作日志]加载tensorflow模型文件结束，耗时：{}", System.currentTimeMillis() - start);
    }


    /**
     * 获取模型文件session
     *
     * @return
     */
    public ConcreteFunction getTensorflowModelfunction() {
        return function;
    }
}
