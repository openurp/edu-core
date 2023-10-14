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

import org.beangle.commons.bean.orderings.MultiPropertyOrdering
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.beangle.commons.lang.time.{WeekState, WeekTime, Weeks}
import org.openurp.base.edu.model.Teacher
import org.openurp.base.model.Semester
import org.openurp.edu.clazz.domain.WeekTimeBuilder
import org.openurp.edu.clazz.model.{Clazz, ClazzActivity}

import java.text.SimpleDateFormat

/**
 * 输出一个教学任务教学活动的字符串表示
 */
object ScheduleDigestor {
  val singleTeacher = ":teacher1"
  val multiTeacher = ":teacher+"
  val moreThan1Teacher = ":teacher2"
  val day = ":day"
  val units = ":units"
  val weeks = ":weeks"
  val time = ":time"
  val room = ":room"
  val building = ":building"
  val district = ":district"
  val clazz = ":clazz"
  val course = ":course"
  val starton = ":starton"
  /**
   * :teacher+ :day :units :weeks :room
   */
  val defaultFormat = ":teacher+ :day :units :weeks :room"
  private val delimeter = ","

  val weekdayNamesCn = Array("empty", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

  /**
   * 不计较年份,比较是否是相近的教学活动.
   *
   * @param other
   * @param teacher 是否考虑教师
   * @param room    是否考虑教室
   * @return
   */
  private def isSameActivityExcept(target: ClazzActivity, other: ClazzActivity, teacher: Boolean, room: Boolean): Boolean = {
    if (teacher) if (!(target.teachers == other.teachers)) return false
    if (room) if (!(target.rooms == other.rooms)) return false
    target.time.mergeable(other.time, 20)
  }

  def merge(semester: Semester, activities: collection.Iterable[ClazzActivity], hasTeacher: Boolean, hasRoom: Boolean): collection.Seq[ClazzActivity] = {
    val mergedActivities = Collections.newBuffer[ClazzActivity]
    var activitiesList = Collections.newBuffer[ClazzActivity]
    for (ca <- activities) {
      activitiesList.addOne(ca.clone())
    }
    activitiesList = activitiesList.sorted
    val semesterStartYear = semester.beginOn.getYear
    // 合并相同时间点(不计年份)的教学活动
    for (ca <- activitiesList) {
      val activity = ca
      if (ca.time.startOn.getYear != semesterStartYear) {
        val nextYearStart = activity.time.startOn
        val thisYearStart = WeekTime.getStartOn(semesterStartYear, activity.time.weekday)
        val weeks = Weeks.between(thisYearStart, nextYearStart)
        activity.time.startOn = thisYearStart
        activity.time.weekstate = new WeekState(activity.time.weekstate.value << weeks)
      }
      var merged = false
      for (added <- mergedActivities) {
        if (added.clazz == activity.clazz && isSameActivityExcept(added, activity, hasTeacher, hasRoom)) {
          if (added.time.beginAt >= activity.time.beginAt) added.time.beginAt = activity.time.beginAt
          if (added.time.endAt <= activity.time.endAt) added.time.endAt = activity.time.endAt
          added.time.weekstate = new WeekState(added.time.weekstate.value | activity.time.weekstate.value)
          merged = true
        }
      }
      if (!merged) mergedActivities.addOne(activity)
    }
    mergedActivities
  }

  /**
   * 根据默认格式 {@link # defaultFormat} ，获得教学任务的排课文字信息
   *
   * @param clazz
   * @return
   */
  def digest(clazz: Clazz): String = digest(clazz, ScheduleDigestor.defaultFormat)

  /**
   * 根据格式，获得教学任务的排课文字信息
   *
   * @param textResource
   * @param clazz
   * @param format
   * @return
   */
  def digest(clazz: Clazz, format: String): String = digest(clazz.schedule.activities, format)

  /**
   * 根据默认格式 {@link # defaultFormat}格式，获得教学任务里部分排课活动的文字信息
   *
   * @param textResource
   * @param timeSetting
   * @param activities 任务里的部分排课活动
   * @return
   */
  def digest(activities: collection.Iterable[ClazzActivity]): String = digest(activities, ScheduleDigestor.defaultFormat)

  /**
   * 根据格式，获得教学任务里部分排课活动的文字信息
   *
   * @param textResource
   * @param timeSetting
   * @return
   */
  def digest(activities: collection.Iterable[ClazzActivity], f: String): String = {
    if (activities.isEmpty) return ""
    var format = f
    if (Strings.isEmpty(format)) format = ScheduleDigestor.defaultFormat
    val clazzx = activities.iterator.next.clazz
    val semester = clazzx.semester
    val teachers = Collections.newSet[Teacher]
    val hasRoom = Strings.contains(format, ScheduleDigestor.room)
    val hasTeacher = Strings.contains(format, "teacher")
    if (hasTeacher) {
      for (ca <- activities) {
        if (ca.teachers.nonEmpty) teachers.addAll(ca.teachers)
      }
    }
    var mergedActivities = ScheduleDigestor.merge(semester, activities, hasTeacher, hasRoom)
    // 是否添加老师
    var addTeacher = false
    if (hasTeacher) {
      addTeacher = true
      if (format.indexOf(ScheduleDigestor.singleTeacher) != -1 && teachers.size != 1) addTeacher = false
      if (format.indexOf(ScheduleDigestor.moreThan1Teacher) != -1 && teachers.size < 2) addTeacher = false
      if (format.indexOf(ScheduleDigestor.multiTeacher) != -1 && teachers.isEmpty) addTeacher = false
    }
    val CourseArrangeBuf = new StringBuffer
    mergedActivities = mergedActivities.sorted(new MultiPropertyOrdering("clazz.course.code,time.startOn"))
    // 合并后的教学活动
    for (activity <- mergedActivities) {
      CourseArrangeBuf.append(format)
      var replaceStart = 0
      replaceStart = CourseArrangeBuf.indexOf(":teacher")
      if (addTeacher) {
        val teacherStr = new StringBuilder("")
        for (teacher <- activity.teachers) {
          teacherStr.append(teacher.name)
        }
        CourseArrangeBuf.replace(replaceStart, replaceStart + 9, teacherStr.toString)
      }
      else if (-1 != replaceStart) CourseArrangeBuf.replace(replaceStart, replaceStart + 9, "")
      replaceStart = CourseArrangeBuf.indexOf(ScheduleDigestor.day)
      if (-1 != replaceStart) {
        val weekday = activity.time.weekday
        CourseArrangeBuf.replace(replaceStart, replaceStart + ScheduleDigestor.day.length, weekdayNamesCn(weekday.id))
      }
      replaceStart = CourseArrangeBuf.indexOf(ScheduleDigestor.units)
      if (-1 != replaceStart) {
        CourseArrangeBuf.replace(replaceStart, replaceStart + ScheduleDigestor.units.length, activity.beginUnit + "-" + activity.endUnit)
      }
      replaceStart = CourseArrangeBuf.indexOf(ScheduleDigestor.time)
      if (-1 != replaceStart) {
        // 如果教学活动中有具体时间
        CourseArrangeBuf.replace(replaceStart, replaceStart + ScheduleDigestor.time.length, activity.time.beginAt.toString + "-" + activity.time.endAt.toString)
      }
      replaceStart = CourseArrangeBuf.indexOf(ScheduleDigestor.clazz)
      if (-1 != replaceStart) CourseArrangeBuf.replace(replaceStart, replaceStart + ScheduleDigestor.clazz.length, activity.clazz.crn)
      replaceStart = CourseArrangeBuf.indexOf(ScheduleDigestor.course)
      if (-1 != replaceStart) CourseArrangeBuf.replace(replaceStart, replaceStart + ScheduleDigestor.course.length, activity.clazz.course.name + "(" + activity.clazz.course.code + ")")
      replaceStart = CourseArrangeBuf.indexOf(ScheduleDigestor.weeks)
      if (-1 != replaceStart) {
        // 以本年度的最后一周(而不是从教学日历周数计算而来)作为结束周进行缩略.
        // 是因为很多日历指定的周数,仅限于教学使用了.
        CourseArrangeBuf.replace(replaceStart, replaceStart + ScheduleDigestor.weeks.length, WeekTimeBuilder.digest(activity.time, semester) + " ")
      }
      val sdf = new SimpleDateFormat("M月dd日起")
      replaceStart = CourseArrangeBuf.indexOf(ScheduleDigestor.starton)
      if (-1 != replaceStart) {
        activity.clazz.schedule.firstDate foreach { f =>
          CourseArrangeBuf.replace(replaceStart, replaceStart + ScheduleDigestor.starton.length, sdf.format(f))
        }
      }
      replaceStart = CourseArrangeBuf.indexOf(ScheduleDigestor.room)
      if (-1 != replaceStart) {
        val rooms = activity.rooms
        val roomStr = new StringBuilder("")
        val it = rooms.iterator
        while (it.hasNext) {
          val room = it.next
          roomStr.append(room.name)
          if (it.hasNext) roomStr.append(",")
        }
        if (roomStr.isEmpty && activity.places.nonEmpty) roomStr.append(activity.places.get)
        CourseArrangeBuf.replace(replaceStart, replaceStart + ScheduleDigestor.room.length, roomStr.toString)
        replaceStart = CourseArrangeBuf.indexOf(ScheduleDigestor.building)
        if (-1 != replaceStart) {
          val buildingStr = new StringBuilder("")
          val iterator = rooms.iterator
          while (iterator.hasNext) {
            val room = iterator.next
            room.building foreach { b => buildingStr.append(b.name) }
            if (iterator.hasNext) buildingStr.append(",")
          }
          CourseArrangeBuf.replace(replaceStart, replaceStart + ScheduleDigestor.building.length, buildingStr.toString)
        }
        replaceStart = CourseArrangeBuf.indexOf(ScheduleDigestor.district)
        if (-1 != replaceStart) {
          val districtStr = new StringBuilder("")
          val it = rooms.iterator
          while (it.hasNext) {
            val room = it.next
            districtStr.append(room.campus.name)
            if (it.hasNext) districtStr.append(",")
          }
          CourseArrangeBuf.replace(replaceStart, replaceStart + ScheduleDigestor.district.length, districtStr.toString)
        }
      }
      CourseArrangeBuf.append(" ").append(delimeter)
    }
    if (CourseArrangeBuf.lastIndexOf(delimeter) != -1) CourseArrangeBuf.delete(CourseArrangeBuf.lastIndexOf(delimeter), CourseArrangeBuf.length)
    CourseArrangeBuf.toString
  }
}
