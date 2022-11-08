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

package org.openurp.edu.grade.service.impl

import org.beangle.commons.collection.Collections
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.domain.{CourseGradeProvider, GpaPolicy, GradeFilter}
import org.openurp.edu.grade.model.{CourseGrade, StdGpa}
import org.openurp.edu.grade.service.TranscriptDataProvider
import org.openurp.edu.grade.service.impl.GradeFilterRegistry

/**
 * 成绩绩点提供者
 *
 * @since 2012-05-21
 */
class TranscriptGpaProvider extends TranscriptDataProvider {

  var courseGradeProvider: CourseGradeProvider = _

  var gpaPolicy: GpaPolicy = _

  var gradeFilterRegistry: GradeFilterRegistry = _

  def dataName: String = "gpas"

  def getDatas(stds: Seq[Student], options: collection.Map[String, String]): AnyRef = {
    val matched = getFilters(options)
    val datas = Collections.newMap[Student, StdGpa]
    val gradeMap = courseGradeProvider.getPublished(stds)
    for (std <- stds) {
      var grades: Iterable[CourseGrade] = gradeMap(std)
      for (filter <- matched) grades = filter.filter(grades)
      datas.put(std, gpaPolicy.calc(std, grades, true))
    }
    datas
  }

  /**
   * 获取相应的过滤器
   *
   * @param name
   * @return
   */
  protected def getFilters(options: collection.Map[String, String]): Seq[GradeFilter] = {
    if (null == options || options.isEmpty) return List.empty
    gradeFilterRegistry.getFilters(options.getOrElse("gpa.filters", ""))
  }

}
