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
import org.openurp.edu.clazz.model.{Clazz, ClazzActivity}

import java.time.{LocalDate, LocalDateTime, LocalTime}

object LessonSchedule {

  def convert(clazz: Clazz): collection.Seq[LessonSchedule] = {
    val schedules = Collections.newMap[LocalDateTime, LessonSchedule]
    val merged = ScheduleDigestor.merge(clazz.semester,clazz.schedule.activities,true,true)
    merged.foreach { activity =>
      val beginTime = activity.time.beginAt.toLocalTime
      val endTime = activity.time.endAt.toLocalTime
      activity.time.dates foreach { date =>
        val s1 = date.atTime(beginTime)
        val e2 = date.atTime(endTime)
        if (!schedules.contains(s1)) {
          val schedule = LessonSchedule(activity, date, beginTime, endTime)
          schedules.put(s1, schedule)
        }
      }
    }
    schedules.values.toSeq.sorted
  }

  def convert(activities: Iterable[ClazzActivity], beginAt: LocalDateTime, endAt: LocalDateTime):
  collection.Seq[LessonSchedule] = {
    val schedules = Collections.newMap[String, LessonSchedule]
    activities.foreach { activity =>
      val beginTime = activity.time.beginAt.toLocalTime
      val endTime = activity.time.endAt.toLocalTime
      activity.time.dates foreach { date =>
        val s1 = date.atTime(beginTime)
        val e2 = date.atTime(endTime)
        val key = activity.clazz.id.toString + s1.toString
        if (s1.isBefore(endAt) && beginAt.isBefore(e2) && !schedules.contains(key)) {
          val schedule = LessonSchedule(activity, date, beginTime, endTime)
          schedules.put(key, schedule)
        }
      }
    }
    schedules.values.toSeq
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
    task.semester = Properties("id" -> t.id.toString, "code" -> t.code, "schoolYear" -> t.schoolYear, "name" -> t.name)
    task.subject = Properties("id" -> clazz.course.id.toString, "code" -> clazz.course.code, "name" -> clazz.course.name)
    task.people = ca.teachers.map { x => Properties("id" -> x.id.toString, "code" -> x.code, "name" -> x.name) }.toSeq
    task.crn = clazz.crn
    s.units = s"${ca.beginUnit}-${ca.endUnit}"
    s.hours = ca.endUnit - ca.beginUnit + 1
    s.room = ca.rooms.map(_.name).mkString(",")
    s.task = task
    s
  }
}

class LessonSchedule extends LongId, Ordered[LessonSchedule] {
  var task: ClazzTask = _
  var date: LocalDate = _
  var time: String = _
  var units: String = _
  var hours: Int = _
  var room: String = _

  override def compare(that: LessonSchedule): Int = {
    this.orderDayKey.compareTo(that.orderDayKey)
  }

  def teacherNames: String = {
    task.people.map(_.getOrElse("name", "--")).mkString(" ")
  }

  def orderDayKey: String = {
    task.subject.get("code").toString + date.toString + time
  }

  def orderWeekDayKey: String = {
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
