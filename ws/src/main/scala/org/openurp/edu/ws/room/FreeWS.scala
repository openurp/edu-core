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

package org.openurp.edu.ws.room

import org.beangle.commons.collection.page.{Page, PageLimit}
import org.beangle.commons.collection.{Collections, Properties}
import org.beangle.commons.lang.Strings
import org.beangle.commons.lang.time.{HourMinute, WeekTime}
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.beangle.data.json.JsonAPI
import org.beangle.she.webmvc.{JsonAPISupport, QueryHelper}
import org.beangle.webmvc.annotation.mapping
import org.beangle.webmvc.context.ActionContext
import org.beangle.webmvc.support.{ActionSupport, MimeSupport}
import org.openurp.base.model.Campus
import org.openurp.base.resource.model.{Building, Classroom}
import org.openurp.edu.room.model.Occupancy

import java.time.LocalDate

/** 空闲教室查询
 */
class FreeWS extends ActionSupport, JsonAPISupport, MimeSupport {

  var entityDao: EntityDao = _

  @mapping("")
  def index(): Any = {
    val times = getAll("time", classOf[String])
    if (times.isEmpty) {
      List.empty
    } else {
      val builder = OqlBuilder.from(classOf[Classroom], "classroom")
        .where("classroom.beginOn <= :now and (classroom.endOn is null or classroom.endOn >= :now)", LocalDate.now)

      val limit = new PageLimit(QueryHelper.pageIndex, QueryHelper.pageSize)
      builder.limit(limit)
      getInt("room.campus.id") foreach { campusId =>
        builder.where("classroom.campus.id=:campusId", campusId)
      }
      getInt("room.building.id") foreach { buildingId =>
        builder.where("classroom.building.id=:buildingId", buildingId)
      }
      getInt("room.roomType.id") foreach { roomTypeId =>
        builder.where("classroom.roomType.id=:roomTypeId", roomTypeId)
      }
      get("room.name") foreach { name =>
        if (Strings.isNotBlank(name)) {
          builder.where("classroom.name like :name", s"%${name.trim()}%")
        }
      }
      getInt("room.capacity") foreach { capacity =>
        builder.where("classroom.capacity >:capacity", capacity)
      }
      getInt("room.courseCapacity") foreach { courseCapacity =>
        builder.where("classroom.courseCapacity >:courseCapacity", courseCapacity)
      }
      getInt("room.examCapacity") foreach { examCapacity =>
        builder.where("classroom.examCapacity >:examCapacity", examCapacity)
      }

      val weekTimes = parseTimes(times)
      addOccupancyQuery(builder, weekTimes)
      val rooms = entityDao.search(builder).asInstanceOf[Page[Classroom]]

      if (isAcceptJsonApi) {
        val context = JsonAPI.context(ActionContext.current.params)
        context.filters.include(classOf[Campus], "id", "code", "name")
        context.filters.include(classOf[Building], "id", "code", "name")
        val rs = context.mkJson(rooms, "id", "code", "name", "enName", "campus", "building", "capacity", "courseCapacity", "examCapacity")
        addPageInfo(rs, rooms)
        rs
      } else {
        rooms.map(r => new Properties(r, "id", "code", "name", "enName", "capacity", "courseCapacity", "examCapacity"))
      }
    }
  }

  private def addOccupancyQuery(query: OqlBuilder[Classroom], times: collection.Seq[WeekTime]): Unit = {
    val hql = new StringBuilder(s" from ${classOf[Occupancy].getName} occupancy where occupancy.room = classroom and ")
    val conditions = times.indices map { i =>
      val time = times(i)
      query.param("endTime" + i, time.endAt)
      query.param("startTime" + i, time.beginAt)
      query.param("startOn" + i, time.startOn)
      s"(bitand(occupancy.time.weekstate,${time.weekstate.value})>0 and occupancy.time.startOn = :startOn${i}" +
        s" and occupancy.time.beginAt < :endTime${i} and occupancy.time.endAt > :startTime${i})"
    }
    hql.append(s"(${conditions.mkString(" or ")})")
    query.where("exists (" + hql.toString + ")")
  }

  private def parseTimes(times: Iterable[String]): collection.Seq[WeekTime] = {
    val wts = times flatMap { t =>
      //2024-09-09 09:00~10:30
      val dates = Strings.split(Strings.substringBefore(t, " "), ",").map(x => LocalDate.parse(x))

      val time = Strings.substringAfter(t, " ")
      val beginAt = HourMinute(Strings.substringBefore(time, "-").trim())
      val endAt = HourMinute(Strings.substringAfter(time, "-").trim())
      dates.map(date => WeekTime.of(dates.head, beginAt, endAt))
    }
    val rs = Collections.newBuffer[WeekTime]
    wts foreach { wt =>
      rs.find(t => t.mergeable(wt, 0)) match {
        case None => rs.addOne(wt)
        case Some(t) => t.merge(wt, 0)
      }
    }
    rs
  }
}
