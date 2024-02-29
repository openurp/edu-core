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

package org.openurp.edu.finalmakeup.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.{Numbers, Strings}
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.model.Semester
import org.openurp.edu.exam.model.FinalMakeupCourse
import org.openurp.edu.finalmakeup.service.MakeupCourseCrnGenerator

import scala.collection.mutable

object MakeupCourseCrnGeneratorImpl {
  val initCrnNo = "0001"
  val prefix = "BK"
}

class MakeupCourseCrnGeneratorImpl extends MakeupCourseCrnGenerator {
  var entityDao: EntityDao = _

  def gen(makeupCourse: FinalMakeupCourse): Unit = {
    if (!Strings.isEmpty(makeupCourse.crn)) return
    this synchronized {
      val crns = loadCrns(makeupCourse.semester)
      var newCrn = 0
      var breaked = false
      for (s <- crns if !breaked) {
        val crn = s.substring(MakeupCourseCrnGeneratorImpl.prefix.length)
        if (!crn.matches(".*[^\\d]+.*")) {
          if (Numbers.toInt(crn) - newCrn >= 2) {
            breaked = true
          } else {
            newCrn = Numbers.toInt(crn)
          }
        }
      }
      newCrn += 1
      putCrn(makeupCourse, newCrn)
    }
  }
  private def putCrn(makeupCourse: FinalMakeupCourse, crn: Int): Unit = {
    makeupCourse.crn =  "BK" + Strings.repeat("0", 4 - String.valueOf(crn).length) + crn
  }

  private def loadCrns(semester: Semester): Seq[String] = {
    val builder = OqlBuilder.from(classOf[FinalMakeupCourse].getName + " makeupCourse")
    builder.where("makeupCourse.semester = :semester", semester)
    builder.orderBy("makeupCourse.crn")
    builder.select("makeupCourse.crn")
    entityDao.search(builder)
  }

  def gen(makeupCourses: collection.Seq[FinalMakeupCourse]): Unit = {
    val courseBySemesterMap = Collections.newMap[Semester, mutable.Buffer[FinalMakeupCourse]]
    for (makeupCourse <- makeupCourses) {
      if (Strings.isEmpty(makeupCourse.crn)) {
        courseBySemesterMap.getOrElseUpdate(makeupCourse.semester, Collections.newBuffer[FinalMakeupCourse]) += makeupCourse
      }
    }
    for (semester <- courseBySemesterMap.keySet) {
      genCourseCrns(semester, courseBySemesterMap(semester))
    }
  }

  private def genCourseCrns(semester: Semester, courses: collection.Seq[FinalMakeupCourse]): Unit = {
    if (courses.isEmpty) return
    this synchronized {
      val allCrns = loadCrns(semester)
      var newCrn = 0
      var seq = 0
      var allocated = 0
      val courseIter = courses.iterator
      var breaked = false
      for (s <- allCrns if !breaked) {
        val crn = s.substring(MakeupCourseCrnGeneratorImpl.prefix.length)
        seq = Numbers.toInt(crn)
        if (seq - newCrn >= 2) {
          val gap = seq - newCrn - 1
          var i = 0
          while (i < gap) {
            allocated += 1
            newCrn += 1
            putCrn(courseIter.next(), newCrn)
            if (allocated >= courses.size) {
              i = gap //break
            } else {
              i += 1
            }
          }

          if (allocated >= courses.size) {
            breaked = true
          } else {
            newCrn = seq
          }
        }
      }
      while (allocated < courses.size) {
        newCrn += 1
        allocated += 1
        putCrn(courseIter.next(), newCrn)
      }
    }
  }

}
