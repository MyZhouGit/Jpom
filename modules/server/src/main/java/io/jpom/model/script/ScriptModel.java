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
package io.jpom.model.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.JpomApplication;
import io.jpom.common.Const;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.script.CommandParam;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.CommandUtil;
import io.jpom.util.FileUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.jpom.h2db.TableName;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@TableName(value = "SERVER_SCRIPT_INFO", name = "脚本模版")
@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptModel extends BaseWorkspaceModel {
    /**
     * 模版名称
     */
    private String name;
    /**
     * 最后执行人员
     */
    private String lastRunUser;
    /**
     * 定时执行
     */
    private String autoExecCron;
    /**
     * 默认参数
     */
    private String defArgs;
    /**
     * 描述
     */
    private String description;

    private String context;
    /**
     * 节点ID
     */
    private String nodeIds;
    /**
     * 触发器 token
     */
    private String triggerToken;

    public void setDefArgs(String defArgs) {
        this.defArgs = CommandParam.convertToParam(defArgs);
    }

    public File scriptPath() {
        return scriptPath(getId());
    }

    public static File scriptPath(String id) {
        if (StrUtil.isEmpty(id)) {
            throw new IllegalArgumentException("id 为空");
        }
        File path = JpomApplication.getInstance().getScriptPath();
        return FileUtil.file(path, id);
    }

    public File logFile(String executeId) {
        //File path = this.scriptPath();
        //return FileUtil.file(path, "log", executeId + ".log");
        return logFile(getId(), executeId);
    }

    public static File logFile(String id, String executeId) {
        File path = scriptPath(id);
        return FileUtil.file(path, "log", executeId + ".log");
    }

    public File scriptFile() {
        String dataPath = JpomApplication.getInstance().getDataPath();

        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), CommandUtil.SUFFIX));
        FileUtils.writeScript(this.getContext(), scriptFile, ExtConfigBean.getConsoleLogCharset());
//        File path = this.scriptPath();
//        File file = FileUtil.file(path, StrUtil.format("script.{}", CommandUtil.SUFFIX));
//        //
//        FileUtil.writeString(getContext(), file, ExtConfigBean.getConsoleLogCharset());
        return scriptFile;
    }
}
