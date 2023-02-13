package org.openurp.edu.progarm.service

import org.beangle.cdi.bind.BindModule
import org.openurp.edu.program.domain.{DefaultCoursePlanProvider, DefaultProgramProvider}

class DefaultModule extends BindModule {
  override protected def binding(): Unit = {
    bind(classOf[DefaultProgramProvider])
    bind(classOf[DefaultCoursePlanProvider])
  }

}
