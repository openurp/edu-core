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

package org.openurp.edu.program.service.checkers

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.openurp.edu.program.model.{MajorPlan, ProgramDoc}
import org.openurp.edu.program.service.DocChecker

/** 文本中的主干课程是否存在
 */
class MajorCourseDocChecker extends DocChecker {

  def check(doc: ProgramDoc, plan: MajorPlan): Seq[String] = {
    val rs = Collections.newBuffer[String]

    val allCourses = Collections.newSet[String]
    //登记所有课程，处理括弧和空格
    for (g <- plan.groups; pc <- g.planCourses) {
      var courseName = Strings.replace(pc.journal.name, "（", "(")
      courseName = Strings.replace(courseName, "）", ")")
      courseName = Strings.replace(courseName, " ", "")
      courseName = courseName.toLowerCase
      allCourses.addOne(courseName)
    }
    doc.getText("courses") foreach { pt =>
      var contents = pt.contents
      // 主要课程包括：高等数学、B、C和D等
      contents = Strings.replace(contents, "：", ":")
      if (contents.contains(":")) {
        contents = Strings.substringAfter(contents, ":")
      }
      contents = Strings.replace(contents, "（", "(")
      contents = Strings.replace(contents, "）", ")")
      if (contents.contains("等。")) {
        contents = Strings.substringBefore(contents, "等。")
      }
      contents = Strings.replace(contents, "。", "")
      contents = contents.toLowerCase

      var courseNames = Strings.split(contents, Array(',', '，', ';', '\r', '\n', '、'))
      courseNames = courseNames.map { x =>
        var n = (if x.endsWith("等") then x.substring(0, x.length - 1) else x)
        n = Strings.replace(n, " ", "")
        if (n.startsWith("以及")) n = n.substring(2)
        if (null != Strings.substringBetween(n, "等", "课")) {
          n = Strings.substringBefore(n, "等")
        }
        n
      }
      val errorNames = courseNames.filter(x => !allCourses.contains(x)).toBuffer
      //去除模糊的叫法
      val removed = Collections.newSet[String]
      val newer = Collections.newSet[String]
      errorNames.foreach { name =>
        if allCourses.exists(_.startsWith(name)) then removed.addOne(name)
        else {
          val andIdx = name.indexOf("和")
          if (andIdx > 0) {
            removed.addOne(name)
            val first = name.substring(0, andIdx)
            val second = name.substring(andIdx + 1)
            if !allCourses.exists(_.startsWith(first)) then newer.addOne(first)
            if !allCourses.exists(_.startsWith(second)) then newer.addOne(second)
          }
        }
      }
      errorNames.subtractAll(removed)
      errorNames.addAll(newer)
      if (errorNames.nonEmpty) {
        rs.addOne(s"文本中的【主要课程】中，有${errorNames.mkString("、")} ${errorNames.size}门课程在教学计划中找不到，请检查")
      }
    }
    rs.toSeq
  }
}
