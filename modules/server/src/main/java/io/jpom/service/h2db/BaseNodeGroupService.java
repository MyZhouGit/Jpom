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
package io.jpom.service.h2db;

import cn.hutool.db.Entity;
import io.jpom.common.Const;
import io.jpom.common.ServerConst;
import io.jpom.model.BaseNodeGroupModel;
import io.jpom.service.node.NodeService;
import io.jpom.service.system.WorkspaceService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bwcx_jzy
 * @since 2023/2/8
 */
public abstract class BaseNodeGroupService<T extends BaseNodeGroupModel> extends BaseNodeService<T> {

    protected BaseNodeGroupService(NodeService nodeService,
                                   WorkspaceService workspaceService,
                                   String dataName) {
        super(nodeService, workspaceService, dataName);
    }

    /**
     * load date group by group name
     *
     * @return list
     */
    public List<String> listGroup(HttpServletRequest request) {
        String workspaceId = getCheckUserWorkspace(request);
        String sql = "select `GROUP` from " + getTableName() + " where workspaceId=? group by `GROUP`";
        List<Entity> list = super.query(sql, workspaceId);
        // 筛选字段
        return list.stream().flatMap(entity -> {
                Object obj = entity.get(ServerConst.GROUP_STR);
                if (obj == null) {
                    return null;
                }
                return Stream.of(String.valueOf(obj));
            }).filter(Objects::nonNull)
            .distinct().collect(Collectors.toList());
    }

    /**
     * 恢复字段
     */
    public void repairGroupFiled() {
        String sql = "update " + getTableName() + " set `GROUP`=? where `GROUP` is null or `GROUP`=''";
        super.execute(sql, Const.DEFAULT_GROUP_NAME);
    }
}
