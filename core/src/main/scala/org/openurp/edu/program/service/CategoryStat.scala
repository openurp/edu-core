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

package org.openurp.edu.program.service

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.openurp.base.edu.model.Terms
import org.openurp.code.edu.model.{CourseRank, TeachingNature}

/**
 * 统计某个维度的总学分、总学时、分类课时、学期学分
 */
class CategoryStat(val name: String, val rank: Option[CourseRank]) {
  var practical = false //实践/理论环节

  var compulsory = false //必修/选修

  var inner = false //课内实践

  var credits = .0 //总学分

  var hours = 0 //总课时

  var termCredits: Array[Float] = null //分学期学分

  val typeHours = Collections.newMap[TeachingNature, Int]

  def this(o: CategoryStat) = {
    this(o.name, None)
    this.practical = o.practical
    this.compulsory = o.compulsory
    this.credits = o.credits
    this.hours = o.hours
    this.termCredits = java.util.Arrays.copyOf(o.termCredits, o.termCredits.length)
    this.typeHours.addAll(o.typeHours)
  }

  def this(name: String, rank: CourseRank, compulsory: Boolean, practical: Boolean, inner: Boolean, maxTerm: Int) = {
    this(name, Option(rank))
    this.compulsory = compulsory
    this.practical = practical
    this.inner = inner
    this.termCredits = new Array[Float](maxTerm)
  }

  def merge(o: CategoryStat): Unit = {
    this.credits += o.credits
    this.hours += o.hours
    for (i <- o.termCredits.indices) {
      this.termCredits(i) += o.termCredits(i)
    }
    o.typeHours foreach { case (n, h) =>
      this.typeHours.put(n, this.typeHours.getOrElse(n, 0) + h)
    }
  }

  /** 添加一个课程
   *
   * @param credit
   * @param typeHour
   * @param term
   */
  def addCourse(credit: Float, typeHour: collection.Map[TeachingNature, Int], term: Terms): Unit = {
    this.credits += credit
    typeHour foreach { case (n, h) =>
      hours += h
      typeHours.put(n, typeHours.getOrElse(n, 0) + h)
    }
    val tt = term.first - 1 //学期都是1开始计算的
    if (tt >= 0 && tt < termCredits.length) termCredits(tt) += credit
  }

  def addGroup(credit: Float, typeHour: collection.Map[TeachingNature, Int], termCredits: String): Unit = {
    this.credits += credit
    typeHour foreach { case (n, h) =>
      hours += h
      typeHours.put(n, typeHours.getOrElse(n, 0) + h)
    }
    if (Strings.isNotEmpty(termCredits)) {
      val optionTermCredits = Strings.split(termCredits, ",")
      val terms = Math.min(this.termCredits.length, optionTermCredits.length)
      for (i <- 0 until terms) {
        this.termCredits(i) += optionTermCredits(i).toFloat
      }
    }
  }

  override def toString: String = {
    val natureHours = typeHours.map(x => s"${x._1.name}:${x._2}").mkString(" ")
    s"${name} ${credits}credits ${hours}hours($natureHours)"
  }

  def getHour(t: TeachingNature): Int = typeHours.getOrElse(t, 0)

  def getHour(natureId: String): Int = typeHours.find(_._1.id.toString == natureId).map(_._2).getOrElse(0)
}
