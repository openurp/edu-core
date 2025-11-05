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

package org.openurp.edu.grade.service

import org.beangle.commons.lang.time.Stopwatch
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.OqlBuilder
import org.beangle.data.orm.hibernate.{AbstractDaoTask, SessionHelper}
import org.openurp.base.model.Project
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.model.{CourseGrade, StdGpa}

import java.time.{Instant, LocalDate}

/** GPA定时计算服务
 */
class AutoGpaStatJob extends AbstractDaoTask, Logging {
  var gpaService: GpaService = _

  // one day
  var minUpdateDays = 1
  // 每次计算多少个
  private var bulkSize = 100
  //一次任务执行最多计算多少个
  var maxStdCount = 10000

  private var projectIds: Seq[Int] = Seq.empty

  override def execute(): Unit = {
    projectIds = getActiveProjectIds()
    if (projectIds.isEmpty) return

    var cnt = 0
    //优先处理无数据的学生
    var breakable = false
    while (cnt < maxStdCount && !breakable) {
      val sw = new Stopwatch(true)
      val stds = getStdsWithoutData()
      if (stds.isEmpty) {
        breakable = true
      } else {
        cnt += stds.size
        stds.foreach(gpaService.stat)
        val first = stds.head
        val last = stds.last
        logger.info("stat new " + bulkSize + "(" + first.code + "-" + last.code + ") gpas using " + sw)
      }
      clean()
    }

    if (cnt < maxStdCount) {
      //处理数据过期1天的绩点
      var breakable = false
      while (cnt < maxStdCount && !breakable) {
        val sw = new Stopwatch(true)
        val expired = getOutdated()
        if (expired.isEmpty) {
          breakable = true
        } else {
          cnt += expired.size
          expired.foreach(gpaService.refresh)
          val first = expired.head
          val last = expired.last
          logger.info("stat outdated " + bulkSize + "(" + first.std.code + "-" + last.std.code + ") gpas using " + sw)
        }
        clean()
      }
    }

    if (cnt < maxStdCount) {
      //处理当天过期的的绩点
      var breakable = false
      while (cnt < maxStdCount && !breakable) {
        val sw = new Stopwatch(true)
        val expired = getExpired()
        if (expired.isEmpty) {
          breakable = true
        } else {
          cnt += expired.size
          expired.foreach(gpaService.refresh)
          val first = expired.head
          val last = expired.last
          logger.info("stat expired " + bulkSize + "(" + first.std.code + "-" + last.std.code + ") gpas using " + sw)
        }
        clean()
      }
    }
  }

  /** 优先处理没有数据的学生
   *
   * @return
   */
  private def getStdsWithoutData(): collection.Seq[Student] = {
    val query = OqlBuilder.from(classOf[Student], "s")
    query.where("s.project.id in(:projectIds)", projectIds)
    query.where("not exists(from " + classOf[StdGpa].getName + " r where r.std=s)")
    query.orderBy("s.code")
    query.limit(1, bulkSize)
    entityDao.search(query)
  }

  /** 学籍有效，但是是数据更新一天以上
   *
   * @return
   */
  private def getOutdated(): collection.Seq[StdGpa] = {
    val query = OqlBuilder.from(classOf[StdGpa], "d")
    query.where("d.project.id in(:projectIds)", projectIds)
    query.where("d.updatedAt <= :lastUpdatedAt", Instant.now.minusSeconds(minUpdateDays * 24 * 60 * 60))
    query.where("d.std.beginOn <= :now and d.std.endOn >= :now", LocalDate.now)
    query.orderBy("d.std.code")
    query.limit(1, bulkSize)
    entityDao.search(query)
  }

  /** 学籍有效，数据更新一天以内
   *
   * @return
   */
  private def getExpired(): collection.Seq[StdGpa] = {
    val query = OqlBuilder.from(classOf[StdGpa], "d")
    query.where("d.project.id in(:projectIds)", projectIds)
    query.where("exists(from " + classOf[CourseGrade].getName + " cg where cg.std=s and cg.updatedAt > d.updatedAt)")
    query.where("d.std.beginOn <= :now and d.std.endOn >= :now", LocalDate.now)
    query.orderBy("d.std.code")
    query.limit(1, bulkSize)
    entityDao.search(query)
  }

  private def getActiveProjectIds(): Seq[Int] = {
    val q = OqlBuilder.from(classOf[Project], "p")
    q.where("p.endOn is null or p.endOn >= :projectEndOn", LocalDate.now)
    entityDao.search(q).map(_.id).toSeq
  }

}
