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

package org.openurp.edu.course.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.edu.model.{Course, CourseDirector, TeachingOffice}
import org.openurp.base.hr.model.Teacher
import org.openurp.base.model.{Department, Project, Semester, User}
import org.openurp.code.edu.model.CourseType
import org.openurp.edu.clazz.model.Clazz
import org.openurp.edu.course.model.CourseTask
import org.openurp.edu.course.service.CourseTaskService

import scala.collection.mutable

class CourseTaskServiceImpl extends CourseTaskService {

  var entityDao: EntityDao = _

  override def statTask(project: Project, semester: Semester): Int = {
    val builder = OqlBuilder.from[Array[Any]](classOf[Clazz].getName, "clazz")
    builder.where("clazz.semester=:semester and clazz.project=:project", semester, project)
    builder.join("clazz.teachers", "teacher")
    builder.select("clazz.course,clazz.teachDepart,clazz.courseType,teacher")
    val allTasks = entityDao.search(builder)
    val allTasksMap = Collections.newMap[(Course, Department), mutable.Map[CourseType, mutable.Set[Teacher]]]
    allTasks foreach { d =>
      val course = d(0).asInstanceOf[Course]
      val depart = d(1).asInstanceOf[Department]
      val courseType = d(2).asInstanceOf[CourseType]
      val teacher = d(3).asInstanceOf[Teacher]
      val task = allTasksMap.getOrElseUpdate((course, depart), mutable.Map[CourseType, mutable.Set[Teacher]]())
      val ts = task.getOrElseUpdate(courseType, mutable.HashSet[Teacher]())
      ts += teacher
    }

    val q = OqlBuilder.from(classOf[CourseTask], "task")
    q.where("task.course.project=:project and task.semester=:semester", project, semester)
    val existTasks = entityDao.search(q)
    val existMap = Collections.newMap[(Course, Department), CourseTask]
    existTasks foreach { t =>
      existMap.getOrElseUpdate((t.course, t.department), t)
    }

    var total = 0
    allTasksMap foreach { case ((course, depart), taskMap) =>
      var courseType: CourseType = taskMap.head._1
      var teacherCnt = taskMap.head._2.size
      val teachers = Collections.newSet[Teacher]
      taskMap.foreach { t =>
        if (t._2.size > teacherCnt) {
          teacherCnt = t._2.size
          courseType = t._1
        }
        teachers ++= t._2
      }
      val task = existMap.getOrElse((course, depart), new CourseTask(course, depart, semester, courseType))
      task.courseType = courseType
      task.teachers.clear()
      task.teachers ++= teachers
      if (!task.persisted) total += 1
      entityDao.saveOrUpdate(task)
    }
    entityDao.remove(existTasks.filter(t => !allTasksMap.contains(t.course, t.department)))
    total
  }

  override def isDirector(course: Course, teacher: Teacher): Boolean = {
    val q = OqlBuilder.from(classOf[CourseTask], "c")
    q.where("c.course=:course", course)
    q.where("c.director=:me", teacher)
    entityDao.search(q).nonEmpty
  }

  def getDirector(course: Course, depart: Department, semester: Semester): Option[User] = {
    val q = OqlBuilder.from(classOf[CourseTask], "ct")
    q.where("ct.course=:course and ct.department=:department", course, depart)
    q.where("ct.semester=:semester", semester)
    val tasks = entityDao.search(q)
    var director = tasks.headOption.flatMap(_.director)
    if (director.isEmpty) {
      director = entityDao.findBy(classOf[CourseDirector], "course", course).headOption.flatMap(_.director)
    }
    director match
      case None => None
      case Some(d) => entityDao.findBy(classOf[User], "code", d.code).headOption
  }

  override def getOfficeDirector(course: Course, depart: Department, semester: Semester): Option[User] = {
    val q = OqlBuilder.from(classOf[CourseTask], "ct")
    q.where("ct.course=:course and ct.department=:department", course, depart)
    q.where("ct.semester=:semester", semester)
    val tasks = entityDao.search(q)
    var director = tasks.headOption.flatMap(_.office.flatMap(_.director))
    if (director.isEmpty) {
      director = entityDao.findBy(classOf[CourseDirector], "course", course).headOption.flatMap(_.office.flatMap(_.director))
    }
    director match
      case None => None
      case Some(d) => entityDao.findBy(classOf[User], "code", d.code).headOption
  }

  override def getOffice(course: Course, depart: Department, semester: Semester): Option[TeachingOffice] = {
    val q = OqlBuilder.from(classOf[CourseTask], "ct")
    q.where("ct.course=:course and ct.department=:department", course, depart)
    q.where("ct.semester=:semester", semester)
    val tasks = entityDao.search(q)
    tasks.headOption.flatMap(_.office)
  }

}
