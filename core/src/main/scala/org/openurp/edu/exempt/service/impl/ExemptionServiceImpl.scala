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

package org.openurp.edu.exempt.service.impl

import org.apache.commons.jexl3.introspection.JexlPermissions
import org.apache.commons.jexl3.scripting.JexlScriptEngine
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.beangle.commons.script.{ExpressionEvaluator, JSR223ExpressionEvaluator}
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.edu.model.Course
import org.openurp.base.model.Semester
import org.openurp.base.service.SemesterService
import org.openurp.base.std.model.Student
import org.openurp.code.edu.model.{CourseTakeType, CourseType}
import org.openurp.edu.exempt.config.CertExemptSetting
import org.openurp.edu.exempt.service.ExemptionService
import org.openurp.edu.extern.model.{CertificateGrade, ExternGrade}
import org.openurp.edu.grade.model.{CourseGrade, Grade}
import org.openurp.edu.grade.service.GradeRateService
import org.openurp.edu.program.domain.CoursePlanProvider
import org.openurp.edu.program.model.{CoursePlan, PlanCourse, Program}

import java.time.{LocalDate, YearMonth}

class ExemptionServiceImpl extends ExemptionService {

  var entityDao: EntityDao = _

  var semesterService: SemesterService = _

  var coursePlanProvider: CoursePlanProvider = _

  var gradeRateService: GradeRateService = _

  var evaluator = {
    JexlScriptEngine.setPermissions(JexlPermissions.UNRESTRICTED)
    ExpressionEvaluator.jsr223("jexl3")
  }

  override def calcExemptScore(grade: CertificateGrade): Option[Float] = {
    val q = OqlBuilder.from(classOf[CertExemptSetting], "setting")
    q.where("setting.certificate=:certificate", grade.certificate)
    q.where("setting.config.project=:project", grade.std.project)
    entityDao.search(q).flatMap(_.scoreExpr).headOption match {
      case None => None
      case Some(expr) =>
        if (Strings.isNotBlank(expr)) {
          evaluator.eval(expr, Map("grade" -> grade)) match
            case null => None
            case v: Number => Some(v.floatValue())
        } else None
    }
  }

  override def getSemester(program: Program, term: Option[Int]): Option[Semester] = {
    term match {
      case Some(t) => semesterService.get(program.project, program.beginOn, program.endOn, t)
      case None => None
    }
  }

  override def getConvertablePlanCourses(std: Student, plan: CoursePlan): Seq[PlanCourse] = {
    val coursesMap = Collections.newMap[Course, PlanCourse]
    plan.planCourses foreach { pc =>
      coursesMap.put(pc.course, pc)
    }
    val query = OqlBuilder.from(classOf[CourseGrade], "cg")
    query.where("cg.std=:std and cg.status=:status", std, Grade.Status.Published)
    val courseGrades = entityDao.search(query)
    for (courseGrade <- courseGrades) {
      if (courseGrade.passed) coursesMap.remove(courseGrade.course)
    }
    coursesMap.filterInPlace { case (_, pc) =>
      pc.terms.termList.nonEmpty && semesterService.get(std.project, std.beginOn, std.endOn, pc.terms.termList.head).nonEmpty
    }
    coursesMap.values.toSeq
  }

  override def getConvertedGrades(std: Student, courses: collection.Iterable[Course]): Seq[CourseGrade] = {
    if (courses.isEmpty) {
      List.empty
    } else {
      val query2 = OqlBuilder.from(classOf[CourseGrade], "cg")
      query2.where("cg.std=:std", std)
      query2.where("cg.course in(:courses)", courses)
      query2.where("cg.courseTakeType.id=:exemption", CourseTakeType.Exemption)
      entityDao.search(query2)
    }
  }

  override def removeExemption(eg: ExternGrade, course: Course): Unit = {
    eg.exempts.subtractOne(course)
    entityDao.saveOrUpdate(eg)
    val es = eg.externStudent
    removeExemption(es.std, course)
    entityDao.saveOrUpdate(eg)
  }

  private def removeExemption(std: Student, course: Course): Unit = {
    val cgs = getExemptionGrades(std, course)
    if (cgs.size > 1) {
      throw new RuntimeException(s"found ${cgs.size} exemption grades of ${std.code}")
    } else {
      entityDao.remove(cgs)
    }
  }

  override def removeExemption(cg: CertificateGrade, course: Course): Unit = {
    cg.exempts.subtractOne(course)
    entityDao.saveOrUpdate(cg)
    removeExemption(cg.std, course)
    entityDao.saveOrUpdate(cg)
  }

  override def addExemption(eg: ExternGrade, courses: Iterable[Course], score: Option[Float]): Unit = {
    val remark = eg.externStudent.school.name + " " + eg.courseName + " " + eg.scoreText
    val std = eg.externStudent.std
    addCourseGrades(std, courses, s"${eg.id}@ExternGrade", Some(eg.acquiredOn), score, remark)
    val emptyCourses = eg.exempts filter (x => getExemptionGrades(std, x).isEmpty)
    eg.exempts.subtractAll(emptyCourses)
    eg.exempts ++= courses
    entityDao.saveOrUpdate(eg)
  }

  override def addExemption(cg: CertificateGrade, courses: Iterable[Course], score: Option[Float]): Unit = {
    val remark = cg.certificate.name + " " + cg.scoreText
    val std = cg.std
    addCourseGrades(std, courses, s"${cg.id}@CertificateGrade", None, score, remark)
    val emptyCourses = cg.exempts filter (x => getExemptionGrades(std, x).isEmpty)
    cg.exempts.subtractAll(emptyCourses)
    cg.exempts ++= courses
    entityDao.saveOrUpdate(cg)
  }

  private def getExemptionGrades(std: Student, course: Course): Iterable[CourseGrade] = {
    val cgQuery = OqlBuilder.from(classOf[CourseGrade], "cg")
    cgQuery.where("cg.std=:std and cg.course=:course", std, course)
    cgQuery.where("cg.courseTakeType.id=:exemption", CourseTakeType.Exemption)
    entityDao.search(cgQuery)
  }

  private def addCourseGrades(std: Student, courses: Iterable[Course], provider: String, acquireOn: Option[YearMonth], score: Option[Float], remark: String): Unit = {
    //1. 删除过往认定过的，不属于目前该范围的成绩
    val cgQuery = OqlBuilder.from(classOf[CourseGrade], "cg")
    cgQuery.where("cg.std=:std and cg.courseTakeType.id=:exemptionTypeId", std, CourseTakeType.Exemption)
    cgQuery.where("cg.provider=:provider", provider)
    val exemptGrades = entityDao.search(cgQuery)
    val courseSet = courses.toSet
    entityDao.remove(exemptGrades.filter(x => !courseSet.contains(x.course)))

    //2. 重新建立成绩
    val convertor = new CourseGradeConvertor(entityDao, gradeRateService)
    val coursePlan = coursePlanProvider.getCoursePlan(std)
    courses foreach { c =>
      var semester: Semester = null
      var courseType: CourseType = null
      coursePlanProvider.getPlanCourse(std, c) foreach { pc =>
        courseType = pc.group.courseType
        if (acquireOn.isEmpty || acquireOn.get.atDay(1).isBefore(std.beginOn)) {
          pc.terms.termList.headOption foreach { term =>
            coursePlan foreach { plan =>
              val program = plan.program
              semester = semesterService.get(std.project, program.beginOn, program.endOn, term).orNull
            }
          }
        } else {
          semester = semesterService.get(std.project, acquireOn.get.atDay(1))
        }
      }
      if null == semester then semester = semesterService.get(std.project, LocalDate.now)
      if (null == courseType) courseType = c.courseType.orNull

      val grade = convertor.convert(std, ExemptionCourse(c, courseType, semester, score), provider, remark)
      entityDao.saveOrUpdate(grade)
    }
  }
}

case class ExemptionCourse(course: Course, courseType: CourseType, semester: Semester, score: Option[Float])
