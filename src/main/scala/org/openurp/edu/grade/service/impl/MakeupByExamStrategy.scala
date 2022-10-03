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
import org.openurp.code.edu.model.ExamType
import org.openurp.edu.grade.service.MakeupStdStrategy
import org.openurp.edu.clazz.model.CourseTaker
import org.openurp.edu.exam.model.ExamTaker
import org.openurp.edu.clazz.model.Clazz
import org.openurp.code.edu.model.GradeType
import org.openurp.edu.grade.BaseServiceImpl

/**
 * 按照排考情况，统计补缓名单
 */
class MakeupByExamStrategy extends BaseServiceImpl with MakeupStdStrategy {

  def getCourseTakers(clazz: Clazz): Seq[CourseTaker] = {
    val query = OqlBuilder.from(classOf[CourseTaker], "taker")
    query.where("taker.clazz = :clazz", clazz).where(" exists (from " + classOf[ExamTaker].getName + " et " +
      " where et.std = taker.std and et.clazz = taker.clazz and et.examType.id in(:examTypeIds))", Array(
      ExamType.Makeup,
      ExamType.Delay))
    entityDao.search(query)
  }

  def getCourseTakerCounts(clazzes: Seq[Clazz]): collection.Map[Clazz, Number] = {
    if (clazzes.isEmpty) return Map.empty

    val clazzMap = Collections.newMap[Long, Clazz]
    for (clazz <- clazzes) clazzMap.put(clazz.id, clazz)
    val query = OqlBuilder.from(classOf[CourseTaker], "taker")
    query.where("taker.clazz in (:clazzes)", clazzes).where(" exists (from " + classOf[ExamTaker].getName + " et " +
      " where et.std = taker.std and et.clazz = taker.clazz and et.examType.id in(:examTypeIds))", Array(
      ExamType.Makeup,
      ExamType.Delay))
      .select("taker.clazz.id,count(*)")
      .groupBy("taker.clazz.id")
    val rs = entityDao.search(query)
    val counts = Collections.newMap[Clazz, Number]
    for (obj <- rs) {
      val count = obj.asInstanceOf[Array[Any]]
      counts.put(clazzMap(count(0).asInstanceOf[Long]), count(1).asInstanceOf[Number])
    }
    counts
  }

  def getClazzCondition(gradeTypeId: Int): String = {
    if (gradeTypeId == GradeType.EndGa) "" else "and exists(from " + classOf[ExamTaker].getName + " et where et.clazz=clazz and et.examType.id in(" +
      ExamType.Makeup +
      "," +
      ExamType.Delay +
      "))"
  }
}
