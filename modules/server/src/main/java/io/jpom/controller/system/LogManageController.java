/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSONArray;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.common.validator.ValidatorRule;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.socket.ServiceFileTailWatcher;
import io.jpom.system.LogbackConfig;
import io.jpom.util.DirTreeUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 系统日志管理
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_LOG)
@SystemPermission
public class LogManageController extends BaseServerController {


    @RequestMapping(value = "log_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONArray> logData(String nodeId) {
        if (StrUtil.isNotEmpty(nodeId)) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.SystemLog);
        }
        JSONArray data = DirTreeUtil.getTreeData(LogbackConfig.getPath());
        return JsonMessage.success("", data);
    }

    /**
     * 删除 需要验证是否最后修改时间
     *
     * @param nodeId 节点
     * @param path   路径
     * @return json
     */
    @RequestMapping(value = "log_del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> logData(String nodeId,
                                       @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "path错误") String path) {
        if (StrUtil.isNotEmpty(nodeId)) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.DelSystemLog);
        }
        File file = FileUtil.file(LogbackConfig.getPath(), path);
        // 判断修改时间
        long modified = file.lastModified();
        Assert.state(System.currentTimeMillis() - modified > TimeUnit.DAYS.toMillis(1), "不能删除近一天相关的日志(文件修改时间)");

        if (FileUtil.del(file)) {
            // 离线上一个日志
            ServiceFileTailWatcher.offlineFile(file);
            return JsonMessage.success("删除成功");
        }
        return new JsonMessage<>(500, "删除失败");
    }


    @RequestMapping(value = "log_download", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void logDownload(String nodeId,
                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "path错误") String path,
                            HttpServletResponse response,
                            HttpServletRequest request) {
        if (StrUtil.isNotEmpty(nodeId)) {
            NodeForward.requestDownload(getNode(), request, response, NodeUrl.DownloadSystemLog);
            return;
        }
        File file = FileUtil.file(LogbackConfig.getPath(), path);
        if (file.isFile()) {
            ServletUtil.write(response, file);
        }
    }
}
