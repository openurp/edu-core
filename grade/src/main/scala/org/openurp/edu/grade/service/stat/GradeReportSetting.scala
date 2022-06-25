/*
 * Copyright (C) 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openurp.edu.grade.service.stat

import java.time.Instant

import org.beangle.commons.collection.Order
import org.openurp.code.edu.model.GradeType
import org.openurp.base.model.Project

/**
  * 报表设置
  */
class GradeReportSetting {

  /**
    * 打印绩点
    */
  var printGpa: Boolean = true

  /** 是否打印每学期绩点 */
  var printTermGpa: Boolean = false

  /**
    * 打印成绩类型<br>
    */
  var gradeFilters: String = _

  /**
    * 每页打印的成绩数量
    */
  var pageSize: Int = 80

  /**
    * 成绩中的字体大小
    */
  var fontSize: Int = 10

  var project: Project = _

  /**
    * 打印奖励学分
    */
  var printAwardCredit: Boolean = true

  /** 是否打印校外考试成绩 */
  var printOtherGrade: Boolean = true

  /**
    * 成绩依照什么进行排序,具体含义要依照报表样式
    */
  var order: Order = _
  /**
    * 打印成绩的类型
    */
  var gradeType: GradeType = new GradeType(GradeType.Final)

  /** 打印责任人 */
  var printBy: String = _

  /** 打印模板 */
  var template: String = _

  /** 打印时间 */
  var printAt: Instant = Instant.now

}
