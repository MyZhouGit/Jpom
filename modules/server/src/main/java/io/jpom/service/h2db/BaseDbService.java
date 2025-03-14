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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.common.BaseServerController;
import io.jpom.model.BaseUserModifyDbModel;
import io.jpom.model.user.UserModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.Assert;
import top.jpom.db.DbExtConfig;
import top.jpom.h2db.BaseDbCommonService;
import top.jpom.model.BaseDbModel;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 数据库操作 通用 serve
 *
 * @author bwcx_jzy
 * @since 2021/8/13
 */
@Slf4j
public abstract class BaseDbService<T extends BaseDbModel> extends BaseDbCommonService<T> {

    @Autowired
    @Lazy
    private DbExtConfig extConfig;

    /**
     * 默认排序规则
     */
    private static final Order[] DEFAULT_ORDERS = new Order[]{
            new Order("createTimeMillis", Direction.DESC),
            new Order("modifyTimeMillis", Direction.DESC)
    };

    public void insert(T t) {
        this.fillInsert(t);
        super.insertDb(t);
        this.executeClear();
    }

    /**
     * 先尝试 更新，更新失败插入
     *
     * @param t 数据
     */
    public void upsert(T t) {
        int update = this.updateById(t);
        if (update <= 0) {
            this.insert(t);
        }
    }

    public void insert(Collection<T> t) {
        // def create time
        t.forEach(this::fillInsert);
        super.insertDb(t);
        this.executeClear();
    }

    /**
     * 插入数据填充
     *
     * @param t 数据
     */
    protected void fillInsert(T t) {
        // def create time
        t.setCreateTimeMillis(ObjectUtil.defaultIfNull(t.getCreateTimeMillis(), SystemClock.now()));
        t.setId(StrUtil.emptyToDefault(t.getId(), IdUtil.fastSimpleUUID()));
        if (t instanceof BaseUserModifyDbModel) {
            UserModel userModel = BaseServerController.getUserModel();
            userModel = userModel == null ? BaseServerController.getUserByThreadLocal() : userModel;
            // 获取数据修改人
            BaseUserModifyDbModel modifyDbModel = (BaseUserModifyDbModel) t;
            if (userModel != null) {
                modifyDbModel.setModifyUser(ObjectUtil.defaultIfNull(modifyDbModel.getModifyUser(), userModel.getId()));
                modifyDbModel.setCreateUser(ObjectUtil.defaultIfNull(modifyDbModel.getCreateUser(), userModel.getId()));
            }
        }
    }

    /**
     * update by id with data
     *
     * @param info          data
     * @param whereConsumer 查询条件回调
     * @return 影响的行数
     */
    public int updateById(T info, Consumer<Entity> whereConsumer) {
        // check id
        String id = info.getId();
        Assert.hasText(id, "不能执行：error");
        // def modify time
        info.setModifyTimeMillis(ObjectUtil.defaultIfNull(info.getModifyTimeMillis(), SystemClock.now()));
        // remove create time
        Long createTimeMillis = info.getCreateTimeMillis();
        info.setCreateTimeMillis(null);
        // fill modify user
        if (info instanceof BaseUserModifyDbModel) {
            BaseUserModifyDbModel modifyDbModel = (BaseUserModifyDbModel) info;
            UserModel userModel = BaseServerController.getUserModel();
            if (userModel != null) {
                modifyDbModel.setModifyUser(ObjectUtil.defaultIfNull(modifyDbModel.getModifyUser(), userModel.getId()));
            }
            modifyDbModel.setCreateUser(null);
        }
        //
        Entity entity = this.dataBeanToEntity(info);
        //
        entity.remove(StrUtil.format("`{}`", ID_STR));
        //
        Entity where = new Entity();
        where.set(ID_STR, id);
        if (whereConsumer != null) {
            whereConsumer.accept(where);
        }
        int update = super.updateDb(entity, where);
        // backtrack
        info.setCreateTimeMillis(createTimeMillis);
        return update;
    }


    /**
     * 根据主键查询实体
     *
     * @param keyValue 主键值
     * @return 数据
     */
    public T getByKey(String keyValue) {
        return this.getByKey(keyValue, true);
    }

    /**
     * 根据主键查询实体
     *
     * @param keyValue 主键值
     * @return 数据
     */
    public List<T> getByKey(Collection<String> keyValue) {
        return this.getByKey(keyValue, true, null);
    }

    /**
     * 根据主键查询实体
     *
     * @param keyValue 主键值
     * @return 数据
     */
    public T getByKey(String keyValue, boolean fill) {
        return this.getByKey(keyValue, fill, null);
    }

    /**
     * update by id with data
     *
     * @param info data
     * @return 影响的行数
     */
    public int updateById(T info) {
        return this.updateById(info, null);
    }

    public int update(Entity entity, Entity where) {
        entity.remove("createUser");
        return super.updateDb(entity, where);
    }

    /**
     * 根据主键生成
     *
     * @param keyValue 主键值
     * @return 影响行数
     */
    public int delByKey(String keyValue) {
        return this.delByKey(keyValue, null);
    }

    /**
     * 根据主键生成
     *
     * @param keyValue 主键值
     * @param consumer 回调
     * @return 影响行数
     */
    public int delByKey(Object keyValue, Consumer<Entity> consumer) {
        Entity where = new Entity(tableName);
        if (keyValue != null) {
            where.set(ID_STR, keyValue);
        }
        if (consumer != null) {
            consumer.accept(where);
        }
        Assert.state(where.size() > 0, "没有添加任何参数:-1");
        return del(where);
    }

    /**
     * 判断是否存在
     *
     * @param data 实体
     * @return true 存在
     */
    public boolean exists(T data) {
        Entity entity = this.dataBeanToEntity(data);
        return this.exists(entity);
    }

    /**
     * 判断是否存在
     *
     * @param where 条件
     * @return true 存在
     */
    public boolean exists(Entity where) {
        long count = this.count(where);
        return count > 0;
    }

    /**
     * 查询一个
     *
     * @param where 条件
     * @return Entity
     */
    public Entity query(Entity where) {
        List<Entity> entities = this.queryList(where);
        return CollUtil.getFirst(entities);
    }

    /**
     * 查询 list
     *
     * @param where 条件
     * @return data
     */
    public List<T> listByEntity(Entity where) {
        List<Entity> entity = this.queryList(where);
        return this.entityToBeanList(entity);
    }

    /**
     * 查询列表
     *
     * @param data   数据
     * @param count  查询数量
     * @param orders 排序
     * @return List
     */
    public List<T> queryList(T data, int count, Order... orders) {
        Entity where = this.dataBeanToEntity(data);
        return this.queryList(where, count, orders);
    }

    /**
     * 查询列表
     *
     * @param where  条件
     * @param count  查询数量
     * @param orders 排序
     * @return List
     */
    public List<T> queryList(Entity where, int count, Order... orders) {
        Page page = new Page(1, count);
        page.addOrder(orders);
        PageResultDto<T> tPageResultDto = this.listPage(where, page);
        return tPageResultDto.getResult();
    }


    /**
     * 分页查询
     *
     * @param where 条件
     * @param page  分页
     * @return 结果
     */
    public PageResultDto<T> listPage(Entity where, Page page) {
        return this.listPage(where, page, true);
    }

    /**
     * 分页查询
     *
     * @param where 条件
     * @param page  分页
     * @return 结果
     */
    public List<T> listPageOnlyResult(Entity where, Page page) {
        PageResultDto<T> pageResultDto = this.listPage(where, page);
        return pageResultDto.getResult();
    }

    /**
     * sql 查询 list
     *
     * @param sql    sql 语句
     * @param params 参数
     * @return list
     */
    public List<T> queryList(String sql, Object... params) {
        List<Entity> query = this.query(sql, params);
        return this.entityToBeanList(query);
    }

    /**
     * 查询实体对象
     *
     * @param data 实体
     * @return data
     */
    public List<T> listByBean(T data) {
        return this.listByBean(data, true);
    }

    /**
     * 查询实体对象
     *
     * @param data 实体
     * @return data
     */
    public List<T> listByBean(T data, boolean fill) {
        Entity where = this.dataBeanToEntity(data);
        List<Entity> entitys = this.queryList(where);
        return this.entityToBeanList(entitys, fill);
    }

    /**
     * 查询实体对象
     *
     * @param data 实体
     * @return data
     */
    public T queryByBean(T data) {
        Entity where = this.dataBeanToEntity(data);
        Entity entity = this.query(where);
        return this.entityToBean(entity, true);
    }

    public List<T> list() {
        return this.list(true);
    }

    public List<T> list(boolean fill) {
        return this.listByBean(ReflectUtil.newInstance(this.tClass), fill);
    }

    public long count() {
        return super.count(Entity.create());
    }

    public long count(T data) {
        return super.count(this.dataBeanToEntity(data));
    }

    /**
     * 通用的分页查询, 使用该方法查询，数据库表字段不能包含 "page", "limit", "order_field", "order", "total"
     * <p>
     * page=1&limit=10&order=ascend&order_field=name
     *
     * @param request 请求对象
     * @return page
     */
    public PageResultDto<T> listPage(HttpServletRequest request) {
        return this.listPage(request, true);
    }

    /**
     * 通用的分页查询, 使用该方法查询，数据库表字段不能包含 "page", "limit", "order_field", "order", "total"
     * <p>
     * page=1&limit=10&order=ascend&order_field=name
     *
     * @param request 请求对象
     * @return page
     */
    public PageResultDto<T> listPage(HttpServletRequest request, boolean fill) {
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        return this.listPage(paramMap, fill);
    }

    /**
     * 转换为 page 对象
     *
     * @param paramMap 请求参数
     * @return page
     */
    public Page parsePage(Map<String, String> paramMap) {
        int page = Convert.toInt(paramMap.get("page"), 1);
        int limit = Convert.toInt(paramMap.get("limit"), 10);
        Assert.state(page > 0, "page value error");
        Assert.state(limit > 0 && limit < 200, "limit value error");
        // 移除 默认字段
        MapUtil.removeAny(paramMap, "page", "limit", "order_field", "order", "total");
        //
        return new Page(page, limit);
    }

    /**
     * 通用的分页查询, 使用该方法查询，数据库表字段不能包含 "page", "limit", "order_field", "order", "total"
     * <p>
     * page=1&limit=10&order=ascend&order_field=name
     *
     * @param paramMap 请求参数
     * @return page
     */
    public PageResultDto<T> listPage(Map<String, String> paramMap) {
        return this.listPage(paramMap, true);
    }

    /**
     * 通用的分页查询, 使用该方法查询，数据库表字段不能包含 "page", "limit", "order_field", "order", "total"
     * <p>
     * page=1&limit=10&order=ascend&order_field=name
     *
     * @param paramMap 请求参数
     * @return page
     */
    public PageResultDto<T> listPage(Map<String, String> paramMap, boolean fill) {
        String orderField = paramMap.get("order_field");
        String order = paramMap.get("order");
        //
        Page pageReq = this.parsePage(paramMap);
        Entity where = Entity.create();
        List<String> ignoreField = new ArrayList<>(10);
        // 查询条件
        for (Map.Entry<String, String> stringStringEntry : paramMap.entrySet()) {
            String key = stringStringEntry.getKey();
            String value = stringStringEntry.getValue();
            if (StrUtil.isEmpty(value)) {
                continue;
            }
            key = StrUtil.removeAll(key, "%");
            if (StrUtil.startWith(stringStringEntry.getKey(), "%") && StrUtil.endWith(stringStringEntry.getKey(), "%")) {
                where.set(StrUtil.format("`{}`", key), StrUtil.format(" like '%{}%'", value));
            } else if (StrUtil.endWith(stringStringEntry.getKey(), "%")) {
                where.set(StrUtil.format("`{}`", key), StrUtil.format(" like '{}%'", value));
            } else if (StrUtil.startWith(stringStringEntry.getKey(), "%")) {
                where.set(StrUtil.format("`{}`", key), StrUtil.format(" like '%{}'", value));
            } else if (StrUtil.containsIgnoreCase(key, "time") && StrUtil.contains(value, "~")) {
                // 时间筛选
                String[] val = StrUtil.splitToArray(value, "~");
                if (val.length == 2) {
                    DateTime startDateTime = DateUtil.parse(val[0], DatePattern.NORM_DATETIME_FORMAT);
                    where.set(key, ">= " + startDateTime.getTime());

                    DateTime endDateTime = DateUtil.parse(val[1], DatePattern.NORM_DATETIME_FORMAT);
                    if (startDateTime.equals(endDateTime)) {
                        endDateTime = DateUtil.endOfDay(endDateTime);
                    }
                    // 防止字段重复
                    where.set(key + " ", "<= " + endDateTime.getTime());
                }
            } else if (StrUtil.containsIgnoreCase(key, "time")) {
                // 时间筛选
                String timeKey = StrUtil.removeAny(key, "[0]", "[1]");
                if (ignoreField.contains(timeKey)) {
                    continue;
                }
                String startTime = paramMap.get(timeKey + "[0]");
                String endTime = paramMap.get(timeKey + "[1]");
                if (StrUtil.isAllNotEmpty(startTime, endTime)) {
                    DateTime startDateTime = DateUtil.parse(startTime, DatePattern.NORM_DATETIME_FORMAT);
                    where.set(timeKey, ">= " + startDateTime.getTime());

                    DateTime endDateTime = DateUtil.parse(endTime, DatePattern.NORM_DATETIME_FORMAT);
                    if (startDateTime.equals(endDateTime)) {
                        endDateTime = DateUtil.endOfDay(endDateTime);
                    }
                    // 防止字段重复
                    where.set(timeKey + " ", "<= " + endDateTime.getTime());
                }
                ignoreField.add(timeKey);
            } else if (StrUtil.endWith(key, ":in")) {
                String inKey = StrUtil.removeSuffix(key, ":in");
                where.set(StrUtil.format("`{}`", inKey), StrUtil.split(value, StrUtil.COMMA));
            } else {
                where.set(StrUtil.format("`{}`", key), value);
            }
        }
        // 排序
        if (StrUtil.isNotEmpty(orderField)) {
            orderField = StrUtil.removeAll(orderField, "%");
            pageReq.addOrder(new Order(StrUtil.format("`{}`", orderField), StrUtil.equalsIgnoreCase(order, "ascend") ? Direction.ASC : Direction.DESC));
        }
        return this.listPage(where, pageReq, fill);
    }

    public PageResultDto<T> listPage(Entity where, Page page, boolean fill) {
        if (ArrayUtil.isEmpty(page.getOrders())) {
            page.addOrder(this.defaultOrders());
        }
        return this.listPageDb(where, page, fill);
    }

    protected Order[] defaultOrders() {
        return DEFAULT_ORDERS;
    }

    /**
     * 多个 id 查询数据
     *
     * @param ids ids
     * @return list
     */
    public List<T> listById(Collection<String> ids) {
        return this.listById(ids, null);
    }

    /**
     * 多个 id 查询数据
     *
     * @param ids ids
     * @return list
     */
    public List<T> listById(Collection<String> ids, boolean fill) {
        return this.listById(ids, null, fill);
    }

    /**
     * 多个 id 查询数据
     *
     * @param ids ids
     * @return list
     */
    public List<T> listById(Collection<String> ids, Consumer<Entity> consumer) {
        return this.listById(ids, consumer, true);
    }

    /**
     * 多个 id 查询数据
     *
     * @param ids ids
     * @return list
     */
    public List<T> listById(Collection<String> ids, Consumer<Entity> consumer, boolean fill) {
        if (CollUtil.isEmpty(ids)) {
            return null;
        }
        Entity entity = Entity.create();
        entity.set(ID_STR, ids);
        if (consumer != null) {
            consumer.accept(entity);
        }
        List<Entity> entities = super.queryList(entity);
        return this.entityToBeanList(entities, fill);
    }

    /**
     * 执行清理
     */
    private void executeClear() {
        int h2DbLogStorageCount = extConfig.getLogStorageCount();
        if (h2DbLogStorageCount <= 0) {
            return;
        }
        this.executeClearImpl(h2DbLogStorageCount);
    }

    /**
     * 清理分发实现
     *
     * @param h2DbLogStorageCount 保留数量
     */
    protected void executeClearImpl(int h2DbLogStorageCount) {
        String[] strings = this.clearTimeColumns();
        for (String timeColumn : strings) {
            this.autoClear(timeColumn, h2DbLogStorageCount, time -> {
                Entity entity = Entity.create(super.getTableName());
                entity.set(timeColumn, "< " + time);
                int count = super.del(entity);
                if (count > 0) {
                    log.debug("{} 清理了 {}条数据", super.getTableName(), count);
                }
            });
        }
    }

    /**
     * 安装时间自动清理数据对字段
     *
     * @return 数组
     */
    protected String[] clearTimeColumns() {
        return new String[]{};
    }

    /**
     * 自动清理数据接口
     *
     * @param timeColumn 时间字段
     * @param maxCount   最大数量
     * @param consumer   查询出超过范围的时间回调
     */
    protected void autoClear(String timeColumn, int maxCount, Consumer<Long> consumer) {
        if (maxCount <= 0) {
            return;
        }
        ThreadUtil.execute(() -> {
            long timeValue = this.getLastTimeValue(timeColumn, maxCount, null);
            if (timeValue <= 0) {
                return;
            }
            consumer.accept(timeValue);
        });
    }

    /**
     * 查询指定字段降序 指定条数对最后一个值
     *
     * @param timeColumn 时间字段
     * @param maxCount   最大数量
     * @param whereCon   添加查询条件回调
     * @return 时间
     */
    protected long getLastTimeValue(String timeColumn, int maxCount, Consumer<Entity> whereCon) {
        Entity entity = Entity.create(super.getTableName());
        if (whereCon != null) {
            // 条件
            whereCon.accept(entity);
        }
        Page page = new Page(maxCount, 1);
        page.addOrder(new Order(timeColumn, Direction.DESC));
        PageResultDto<T> pageResult;
        try {
            pageResult = this.listPage(entity, page);
        } catch (java.lang.IllegalStateException illegalStateException) {
            return 0L;
        } catch (Exception e) {
            log.error("查询数据错误", e);
            return 0L;
        }
        if (pageResult.isEmpty()) {
            return 0L;
        }
        T entity1 = pageResult.get(0);
        Object fieldValue = ReflectUtil.getFieldValue(entity1, timeColumn);
        return Convert.toLong(fieldValue, 0L);
    }

    /**
     * 自动清理数据接口
     *
     * @param timeClo   时间字段
     * @param maxCount  最大数量
     * @param predicate 查询出超过范围的时间,回调
     */
    protected void autoLoopClear(String timeClo, int maxCount, Consumer<Entity> whereCon, Predicate<T> predicate) {
        if (maxCount <= 0) {
            return;
        }
        ThreadUtil.execute(() -> {
            Entity entity = Entity.create(super.getTableName());
            long timeValue = this.getLastTimeValue(timeClo, maxCount, whereCon);
            if (timeValue <= 0) {
                return;
            }
            if (whereCon != null) {
                // 条件
                whereCon.accept(entity);
            }
            entity.set(timeClo, "< " + timeValue);
            while (true) {
                Page page = new Page(1, 50);
                page.addOrder(new Order(timeClo, Direction.DESC));
                PageResultDto<T> pageResult = this.listPage(entity, page);
                if (pageResult.isEmpty()) {
                    return;
                }
//                pageResult.each(consumer);
                List<String> ids = pageResult.getResult().stream().filter(predicate).map(BaseDbModel::getId).collect(Collectors.toList());
                this.delByKey(ids, null);
            }
        });
    }

    /**
     * 根据 节点和数据ID查询数据
     *
     * @param nodeId 节点ID
     * @param dataId 数据ID
     * @return data
     */
    public T getData(String nodeId, String dataId) {
        return this.getByKey(dataId);
    }
}
