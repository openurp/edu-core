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

package org.openurp.edu.ws

import org.beangle.cdi.bind.BindModule
import org.openurp.base.service.impl.{ProjectConfigServiceImpl, SemesterServiceImpl}
import org.openurp.edu.ws.grade.AutoAuditJob
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import org.springframework.scheduling.config.{CronTask, ScheduledTaskRegistrar}

class DefaultModule extends BindModule {

  protected def binding(): Unit = {
    bind(classOf[SemesterServiceImpl])
    bind(classOf[ProjectConfigServiceImpl])
    bind(classOf[ConcurrentTaskScheduler])
    bind(classOf[ScheduledTaskRegistrar]).nowire("triggerTasks", "triggerTasksList")
    bind(classOf[AutoAuditJob]).lazyInit(false)
    bindTask(classOf[AutoAuditJob], "0 0 7,11,15,19,23 * * *") //every four hours
  }

  protected def bindTask[T <: Runnable](clazz: Class[T], expression: String): Unit = {
    val taskName = clazz.getName
    bind(taskName + "Task", classOf[CronTask]).constructor(ref(taskName), expression).lazyInit(false)
  }
}
