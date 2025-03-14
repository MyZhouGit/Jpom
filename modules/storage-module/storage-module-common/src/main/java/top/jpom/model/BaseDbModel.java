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
package top.jpom.model;

import cn.hutool.core.date.SystemClock;
import io.jpom.model.BaseIdModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据基础实体
 *
 * @author jzy
 * @since 2021-08-13
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseDbModel extends BaseIdModel {

	/**
	 * 数据创建时间
	 *
	 * @see SystemClock#now()
	 */
	private Long createTimeMillis;

	/**
	 * 数据修改时间
	 */
	private Long modifyTimeMillis;

	@Override
	public String toString() {
		return super.toString();
	}
}
