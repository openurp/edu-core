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

package org.openurp.edu.schedule.service

import org.beangle.commons.collection.{Collections, Properties}
import org.beangle.data.model.LongId
import org.openurp.edu.clazz.model.ClazzActivity

import java.time.{LocalDate, LocalDateTime, LocalTime}

object LessonSchedule {

  def convert(activities: Iterable[ClazzActivity], beginAt: LocalDateTime, endAt: LocalDateTime): collection.Seq[LessonSchedule] = {
    val schedules = Collections.newBuffer[LessonSchedule]
    activities.foreach { activity =>
      val beginTime = LocalTime.of(activity.time.beginAt.hour, activity.time.beginAt.minute)
      val endTime = LocalTime.of(activity.time.endAt.hour, activity.time.endAt.minute)
      activity.time.dates foreach { date =>
        val s1 = date.atTime(beginTime)
        val e2 = date.atTime(endTime)
        if (s1.isBefore(endAt) && beginAt.isBefore(e2)) {
          val schedule = LessonSchedule(activity, date, beginTime, endTime)
          schedules.addOne(schedule)
        }
      }
    }
    schedules
  }

  def apply(ca: ClazzActivity, date: LocalDate, beginAt: LocalTime, endAt: LocalTime): LessonSchedule = {
    val s = new LessonSchedule
    s.id = ca.id
    s.date = date
    s.time = beginAt.toString + "~" + endAt.toString
    val task = new ClazzTask
    val clazz = ca.clazz
    task.id = clazz.id.toString
    val t = clazz.semester
    task.semester = Properties("code" -> t.code, "id" -> t.id.toString, "schoolYear" -> t.schoolYear, "name" -> t.name)
    task.subject = Properties("code" -> clazz.course.code, "name" -> clazz.course.name)
    task.people = ca.teachers.map { x => Properties("code" -> x.code, "name" -> x.name) }.toSeq
    task.crn = clazz.crn
    s.task = task
    s
  }
}

class LessonSchedule extends LongId, Ordered[LessonSchedule] {
  var task: ClazzTask = _
  var date: LocalDate = _
  var time: String = _

  override def compare(that: LessonSchedule): Int = {
    this.orderKey.compareTo(that.orderKey)
  }

  def teacherNames: String = {
    task.people.map(_.getOrElse("name", "--")).mkString(" ")
  }

  def orderKey: String = {
    task.subject.get("code").toString + date.getDayOfWeek.getValue.toString + time + teacherNames + date.toString
  }
}

class ClazzTask {
  var id: String = _
  var semester: Properties = _
  var subject: Properties = _
  var taskType: String = _
  var people: Seq[Properties] = Seq.empty
  var crn: String = _
}
