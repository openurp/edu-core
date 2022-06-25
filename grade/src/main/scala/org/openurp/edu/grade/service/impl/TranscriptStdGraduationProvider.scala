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
import org.beangle.data.dao.OqlBuilder
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.BaseServiceImpl
import org.openurp.edu.grade.service.TranscriptDataProvider
import org.openurp.std.info.model.Graduation

import scala.collection.mutable.Buffer

/**
 * 毕业信息提供者
 *
 *
 * @since 2012-06-07
 */
class TranscriptStdGraduationProvider extends BaseServiceImpl with TranscriptDataProvider {

  def getDatas(stds: Seq[Student], options: collection.Map[String, String]): AnyRef = {
    val query = OqlBuilder.from(classOf[Graduation], "graduation")
    query.where("graduation.std in (:std)", stds)
    val stdGraduations = entityDao.search(query)
    val datas = Collections.newMap[Student, Buffer[Graduation]]
    for (stdGraduation <- stdGraduations) {
      if (!datas.contains(stdGraduation.std)) {
        datas.put(stdGraduation.std, Collections.newBuffer[Graduation])
      }
      datas(stdGraduation.std) += stdGraduation
    }
    datas
  }

  def dataName: String = "stdGraduations"
}
