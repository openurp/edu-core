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

package org.openurp.edu.openapi

import org.beangle.commons.cdi.BindModule

import java.time.{LocalDate, LocalDateTime, LocalTime, OffsetDateTime, YearMonth, ZonedDateTime}
import java.time.format.DateTimeFormatter

class DefaultModule extends BindModule {

  protected def binding(): Unit = {
    bind(classOf[exam.StudentWS])
    bind(classOf[grade.StdWS])
  }
}
object DefaultModule {
  def main(args: Array[String]): Unit = {
    println(ZonedDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    println(OffsetDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    println(YearMonth.now)
    println(LocalTime.now.withNano(0))
  }
}
