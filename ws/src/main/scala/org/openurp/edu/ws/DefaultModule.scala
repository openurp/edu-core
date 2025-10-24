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

import org.beangle.commons.cdi.BindModule
import org.openurp.base.service.impl.{ProjectConfigServiceImpl, SemesterServiceImpl}
import org.openurp.edu.grade.service.{AutoAuditPlanJob, AutoGpaStatJob}
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import org.springframework.scheduling.config.{CronTask, ScheduledTaskRegistrar}

class DefaultModule extends BindModule {

  protected def binding(): Unit = {
    bind(classOf[SemesterServiceImpl])
    bind(classOf[ProjectConfigServiceImpl])
    bind(classOf[ConcurrentTaskScheduler])
    bind(classOf[ScheduledTaskRegistrar]).nowire("triggerTasks", "triggerTasksList")

    bind(classOf[AutoAuditPlanJob]).lazyInit(false)
    bindTask(classOf[AutoAuditPlanJob], "0 0 7,9,11,13,15,17,19,21,23 * * *") //every two hours

    bind(classOf[AutoGpaStatJob]).lazyInit(false)
    bindTask(classOf[AutoGpaStatJob], "0 27 7,9,11,13,15,17,19,20,23 * * *") //every two hours
  }

  protected def bindTask[T <: Runnable](clazz: Class[T], expression: String): Unit = {
    val taskName = clazz.getName
    bind(taskName + "Task", classOf[CronTask]).constructor(ref(taskName), expression).lazyInit(false)
  }
}
